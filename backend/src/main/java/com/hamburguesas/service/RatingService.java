package com.hamburguesas.service;

import com.hamburguesas.dto.NotaYCuantasDto;
import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.dto.ResumenDeReseniasDto;
import com.hamburguesas.dto.TemaDeReseniasDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.logros.LogrosPorRecalcular;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RatingService {

    /** Cuántas fotos puede llevar una reseña (#185). */
    public static final int MAXIMO_DE_FOTOS = 4;

    private final RatingRepository ratingRepository;
    private final BurgerJointRepository burgerJointRepository;
    private final UserRepository userRepository;
    private final FotosDeResenias fotos;
    private final Bloqueos bloqueos;
    private final Reacciones reacciones;
    private final ApplicationEventPublisher eventos;

    /**
     * Las fotos son parte de la reseña, no un agregado posterior.
     *
     * Antes se guardaba el texto primero y la foto después, para que un problema con la
     * foto no se llevara puesto lo escrito. Con la foto obligatoria ese razonamiento se
     * da vuelta: una reseña sin foto no es una reseña, así que guardarla igual sería
     * dejar en la base algo que la app no considera válido. Van juntas o no va ninguna.
     */
    @Transactional
    public RatingResponse rate(Long userId, Long burgerJointId, RatingRequest request,
                               List<MultipartFile> fotosSubidas) {
        if (ratingRepository.findByUser_IdAndBurgerJoint_Id(userId, burgerJointId).isPresent()) {
            throw new ConflictException("You already rated this burger joint. Update it instead of creating a new one.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BurgerJoint burgerJoint = burgerJointRepository.findById(burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));

        List<byte[]> nuevas = leerTodas(fotosSubidas);
        if (nuevas.isEmpty()) {
            throw new ConflictException("Toda reseña lleva una foto de lo que comiste");
        }
        exigirQueEntren(nuevas.size());

        // Se guardan antes de tocar la base: si una foto no sirve, la transacción no
        // llegó a escribir nada.
        List<String> rutas = guardarTodas(nuevas);

        Rating rating = Rating.builder()
            .user(user)
            .burgerJoint(burgerJoint)
            .score(request.score())
            .comment(request.comment())
            .build();
        rating.ponerFotos(rutas);

        // Ansioso: la primera reseña del local con comentario. El local recuerda que ya tuvo una,
        // así que borrarla no deja el lugar libre para que otro la herede.
        boolean primeraDelLocal = !burgerJoint.isTuvoResenias();
        rating.setPrimeraConComentario(primeraDelLocal && request.comment() != null && !request.comment().isBlank());
        burgerJoint.setTuvoResenias(true);

        rating = ratingRepository.save(rating);
        eventos.publishEvent(new XpPorRecalcular(userId));
        eventos.publishEvent(LogrosPorRecalcular.de(userId));
        return toResponse(rating, ReaccionesDto.NINGUNA);
    }

    /**
     * Al editar se dice cuáles de las fotos que ya tenía se quedan, y cuáles se suman.
     *
     * Cambiar una coma no puede obligar a volver a sacar las fotos. Pero las reseñas de
     * antes de que la foto fuera obligatoria no tienen ninguna, y esas sí la piden: es
     * la forma de que el "toda reseña tiene foto" termine siendo cierto sin borrarle la
     * reseña a nadie.
     *
     * @param quedan las que ya tenía y se quedan, en el orden en que se muestran; las
     *   nuevas van después. Null es lo que manda la versión de la app de antes de #185,
     *   que tenía una sola foto: si trae una nueva, reemplaza a la que había.
     */
    @Transactional
    public RatingResponse update(Long userId, Long burgerJointId, RatingRequest request,
                                 List<MultipartFile> fotosSubidas, List<String> quedan) {
        Rating rating = ratingRepository
            .findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("You haven't rated this burger joint yet"));

        List<byte[]> nuevas = leerTodas(fotosSubidas);
        List<String> actuales = rating.todasLasFotos();
        List<String> siguen = queSiguen(actuales, quedan, !nuevas.isEmpty());

        if (siguen.isEmpty() && nuevas.isEmpty()) {
            throw new ConflictException("Tu reseña necesita una foto");
        }
        exigirQueEntren(siguen.size() + nuevas.size());

        if (!nuevas.isEmpty() || !siguen.equals(actuales)) {
            List<String> todas = new ArrayList<>(siguen);
            todas.addAll(guardarTodas(nuevas));
            rating.ponerFotos(todas);

            // Recién cuando la base confirmó: si fallara, la reseña seguiría apuntándolas.
            actuales.stream()
                .filter(foto -> !siguen.contains(foto))
                .forEach(sacada -> despuesDeConfirmar(() -> fotos.borrar(sacada)));
        }

        rating.setScore(request.score());
        rating.setComment(request.comment());
        // Agregarle el comentario la pasa de 70 a 100: la XP se recalcula al confirmar.
        eventos.publishEvent(new XpPorRecalcular(userId));
        eventos.publishEvent(LogrosPorRecalcular.de(userId));
        return conReacciones(List.of(rating), userId).get(0);
    }

    /**
     * Borra la reseña de quien la pide en ese local, con sus fotos (#180).
     *
     * Se busca por quien llama y el local, igual que al editar: no hay forma de nombrar
     * la reseña de otro, así que no hace falta comprobar de quién es. El feed, el
     * promedio y el ranking se calculan de las reseñas, así que ahí no queda nada que
     * limpiar.
     *
     * Los archivos se borran recién cuando la base confirmó: si la transacción fallara,
     * la reseña seguiría ahí, y tiene que seguir teniendo sus fotos.
     */
    @Transactional
    public void borrar(Long userId, Long burgerJointId) {
        Rating rating = ratingRepository
            .findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("You haven't rated this burger joint yet"));

        List<String> suyas = rating.todasLasFotos();
        // Quienes reaccionaron, leídos antes de borrar: pueden perder Stalker al irse la reacción.
        List<Long> quienesReaccionaron = reacciones.quienesReaccionaron(rating.getId());
        ratingRepository.delete(rating);
        eventos.publishEvent(new XpPorRecalcular(userId));
        eventos.publishEvent(LogrosPorRecalcular.de(userId, quienesReaccionaron));

        suyas.forEach(foto -> despuesDeConfirmar(() -> fotos.borrar(foto)));
    }

    /**
     * Las que se quedan, de las que ya tenía.
     *
     * Solo pueden ser suyas: una ruta que no está en la reseña podría ser la foto de
     * otra persona, y aceptarla dejaría que la próxima edición le borre el archivo.
     */
    private List<String> queSiguen(List<String> actuales, List<String> quedan, boolean mandaNuevas) {
        if (quedan == null) {
            return mandaNuevas ? List.of() : actuales;
        }
        List<String> pedidas = quedan.stream()
            .filter(foto -> foto != null && !foto.isBlank())
            .distinct()
            .toList();
        if (!actuales.containsAll(pedidas)) {
            throw new ConflictException("Esa foto no es de tu reseña");
        }
        return pedidas;
    }

    private void exigirQueEntren(int cuantas) {
        if (cuantas > MAXIMO_DE_FOTOS) {
            throw new ConflictException("Una reseña lleva hasta " + MAXIMO_DE_FOTOS + " fotos");
        }
    }

    /**
     * Guarda las nuevas en disco y devuelve sus rutas, en el mismo orden.
     *
     * Si la tercera no sirve, las dos primeras ya quedaron escritas y ninguna reseña las
     * va a apuntar: se borran antes de avisar. Y si después es la base la que falla, se
     * borran también, por lo mismo.
     */
    private List<String> guardarTodas(List<byte[]> nuevas) {
        List<String> rutas = new ArrayList<>();
        try {
            for (byte[] foto : nuevas) {
                rutas.add(fotos.guardar(foto));
            }
        } catch (RuntimeException ex) {
            rutas.forEach(fotos::borrar);
            throw ex;
        }
        siSeDeshace(() -> rutas.forEach(fotos::borrar));
        return rutas;
    }

    /** Fuera de una transacción (en un test, por ejemplo) no hay qué esperar: corre ya. */
    private static void despuesDeConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }

    /** Al revés: solo si la base no llegó a confirmar. Fuera de una transacción, nunca. */
    private static void siSeDeshace(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int estado) {
                    if (estado == STATUS_ROLLED_BACK) {
                        accion.run();
                    }
                }
            });
        }
    }

    /** Los archivos que llegaron, sin los vacíos: un campo de archivo sin elegir llega así. */
    private List<byte[]> leerTodas(List<MultipartFile> subidas) {
        if (subidas == null) {
            return List.of();
        }
        return subidas.stream()
            .filter(foto -> foto != null && !foto.isEmpty())
            .map(this::leer)
            .toList();
    }

    /**
     * Las reseñas de un local, sin las de la gente con la que hay un bloqueo.
     *
     * Es la tercera pantalla donde aparece gente, después del feed y del buscador, y
     * hasta ahora era la única que no miraba los bloqueos: para volver a cruzarte con
     * alguien que bloqueaste alcanzaba con abrir una hamburguesería que los dos
     * hubieran visitado.
     *
     * Lo que no cambia es el promedio del local ni la distribución de notas. El puntaje
     * de una hamburguesería es un hecho del lugar, no de quién lo mira: si dependiera de
     * a quién bloqueaste, dos personas verían promedios distintos del mismo local y
     * ninguna sabría por qué. Se esconde a la persona, no se reescribe la nota.
     */
    @Transactional(readOnly = true)
    public Page<RatingResponse> list(Long burgerJointId, Long userId, Pageable pageable) {
        Page<Rating> pagina = ratingRepository
            .deUnLocalSalvo(burgerJointId, bloqueos.queNoPuedeVer(userId), pageable);
        List<RatingResponse> conTodo = conReacciones(pagina.getContent(), userId);
        return new PageImpl<>(conTodo, pagina.getPageable(), pagina.getTotalElements());
    }

    /**
     * Una sola reseña de un local, para llegar a ella desde el feed o un perfil (#260).
     *
     * La ficha trae las primeras veinte: una más vieja no estaría en la página y el
     * enlace llevaría a un lugar donde no está. Con las mismas reglas que la lista: de
     * otro local o de alguien con un bloqueo de por medio, no existe.
     */
    @Transactional(readOnly = true)
    public RatingResponse una(Long burgerJointId, Long ratingId, Long userId) {
        List<Long> ocultos = bloqueos.queNoPuedeVer(userId);
        Rating resenia = ratingRepository.findById(ratingId)
            .filter(r -> r.getBurgerJoint().getId().equals(burgerJointId))
            .filter(r -> !ocultos.contains(r.getUser().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Rating not found"));
        return conReacciones(List.of(resenia), userId).get(0);
    }

    /**
     * Lo que va arriba de la lista de reseñas: la distribución de notas y de qué hablan.
     *
     * Es información del local, no de nadie: no mira quién pregunta ni los bloqueos, y
     * un local se puede mirar sin cuenta.
     */
    @Transactional(readOnly = true)
    public ResumenDeReseniasDto resumen(Long burgerJointId, Long userId) {
        Map<Integer, Long> cuantasPorNota = ratingRepository
            .distribucionDeNotas(burgerJointId).stream()
            .collect(Collectors.toMap(NotaYCuantasDto::nota, NotaYCuantasDto::cuantas));

        List<NotaYCuantasDto> distribucion = IntStream.rangeClosed(1, 5)
            .mapToObj(nota -> new NotaYCuantasDto(nota, cuantasPorNota.getOrDefault(nota, 0L)))
            .toList();

        return new ResumenDeReseniasDto(distribucion, temas(burgerJointId));
    }

    /**
     * De qué hablan las reseñas escritas de este local.
     *
     * El cálculo vive aparte, sin Spring ni base de datos, porque lo que hay que poder
     * probar es qué cuenta como mención y qué no: acá adentro solo se traen las reseñas
     * y se traduce el resultado.
     */
    private List<TemaDeReseniasDto> temas(Long burgerJointId) {
        List<TemasDeLasResenias.Mencion> menciones = ratingRepository
            .notasYComentarios(burgerJointId).stream()
            .map(fila -> new TemasDeLasResenias.Mencion(fila.nota(), fila.comentario()))
            .toList();

        return TemasDeLasResenias.de(menciones).stream()
            .map(tema -> new TemaDeReseniasDto(tema.nombre(), tema.menciones(), tema.aFavor()))
            .toList();
    }

    private byte[] leer(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ConflictException("No llegó ninguna foto");
        }
        try {
            return foto.getBytes();
        } catch (IOException ex) {
            throw new ConflictException("No pudimos leer esa foto. Probá de nuevo.");
        }
    }

    /** Las reacciones de todas juntas, en dos consultas y no dos por reseña (#186). */
    private List<RatingResponse> conReacciones(List<Rating> resenias, Long quienMira) {
        if (resenias.isEmpty()) {
            return List.of();
        }
        Map<Long, ReaccionesDto> deCadaUna = reacciones.de(
            resenias.stream().map(Rating::getId).toList(), quienMira);
        return resenias.stream()
            .map(r -> toResponse(r, deCadaUna.getOrDefault(r.getId(), ReaccionesDto.NINGUNA)))
            .toList();
    }

    private RatingResponse toResponse(Rating r, ReaccionesDto susReacciones) {
        return new RatingResponse(
            r.getId(), r.getUser().getId(), r.getUser().getUsername(), r.getUser().getHamburguesa(), r.getUser().getXp(),
            r.getScore(), r.getComment(), r.todasLasFotos(), r.getCreatedAt(), susReacciones
        );
    }
}
