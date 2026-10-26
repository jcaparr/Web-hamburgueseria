package com.hamburguesas.logros;

import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Todo lo que hay que medir de una cuenta para saber qué logros le tocan (#314).
 *
 * Son consultas chicas y estándar (JPQL, sin nada propio de Postgres), así que corren
 * igual en la base de los tests. Se piden juntas y de una vez por cuenta: el recálculo
 * compara todo el catálogo contra estas cifras, no un logro por vez.
 */
@Component
public class CifrasDeLogros {

    /** Una reseña de la cuenta, con lo que hace falta de ella y de su local. */
    public record Resenia(Long id, Long localId, String area, boolean cadena, String nombreDelLocal,
                          Instant creada, String comentario, int fotos, boolean primeraConComentario) {
    }

    public record Cifras(
        List<Resenia> resenias,
        long seguidores,
        long guardadas,
        long recorridos,
        long reaccionesQueDio,
        long masReaccionesEnUnaResenia,
        boolean unaConLosCincoTipos,
        long risasRecibidas,
        long seguidoresQueReseniaronDespues,
        long reseniasConGuardadaPrevia,
        String barrioDondeEsIntendente,
        boolean esDeLasPrimeras,
        boolean armoSuHamburguesa
    ) {
    }

    /** Las diez primeras cuentas, por orden de id, son las Pioneras. */
    public static final int PIONEROS = 10;

    @PersistenceContext
    private EntityManager em;

    public Cifras de(User cuenta) {
        Long id = cuenta.getId();
        List<Resenia> resenias = em.createQuery("""
                select r.id, b.id, b.area, b.fastFood, b.name, r.createdAt, r.comment, r.primeraConComentario
                from Rating r join r.burgerJoint b where r.user.id = :id
                """, Object[].class)
            .setParameter("id", id).getResultList().stream()
            .map(f -> new Resenia((Long) f[0], (Long) f[1], (String) f[2], (Boolean) f[3], (String) f[4],
                (Instant) f[5], (String) f[6], 0, (Boolean) f[7]))
            .toList();
        resenias = conFotos(id, resenias);

        long maxReacciones = 0;
        boolean combo = false;
        for (Object[] fila : em.createQuery("""
                select re.rating.id, count(re), count(distinct re.tipo)
                from Reaccion re where re.rating.user.id = :id group by re.rating.id
                """, Object[].class).setParameter("id", id).getResultList()) {
            maxReacciones = Math.max(maxReacciones, (Long) fila[1]);
            combo |= (Long) fila[2] >= TipoDeReaccion.values().length;
        }

        return new Cifras(
            resenias,
            contar("select count(f) from Follow f where f.followed.id = :id", id),
            contar("select count(w) from WishlistItem w where w.user.id = :id", id),
            contar("select count(t) from SavedTour t where t.user.id = :id", id),
            contar("select count(re) from Reaccion re where re.user.id = :id", id),
            maxReacciones,
            combo,
            em.createQuery("select count(re) from Reaccion re where re.rating.user.id = :id and re.tipo = :tipo",
                Long.class).setParameter("id", id).setParameter("tipo", TipoDeReaccion.RISA).getSingleResult(),
            contar("""
                select count(distinct f.follower.id) from Follow f
                where f.followed.id = :id
                  and exists (select 1 from Rating ru, Rating rf
                              where ru.user.id = :id and rf.user.id = f.follower.id
                                and rf.burgerJoint.id = ru.burgerJoint.id and rf.createdAt > ru.createdAt)
                """, id),
            contar("""
                select count(r) from Rating r
                where r.user.id = :id
                  and exists (select 1 from WishlistItem w
                              where w.user.id = :id and w.burgerJoint.id = r.burgerJoint.id
                                and w.createdAt < r.createdAt)
                """, id),
            barrioDondeEsIntendente(id, resenias),
            contar("select count(u) from User u where u.id < :id", id) < PIONEROS,
            cuenta.getHamburguesa() != null
        );
    }

    private long contar(String jpql, Long id) {
        return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }

    /** Las fotos de cada reseña, en una consulta para todas (es una colección aparte). */
    private List<Resenia> conFotos(Long id, List<Resenia> resenias) {
        if (resenias.isEmpty()) {
            return resenias;
        }
        var porReseniaId = new java.util.HashMap<Long, Integer>();
        for (Object[] fila : em.createQuery("""
                select r.id, count(f) from Rating r join r.fotos f where r.user.id = :id group by r.id
                """, Object[].class).setParameter("id", id).getResultList()) {
            porReseniaId.put((Long) fila[0], ((Long) fila[1]).intValue());
        }
        return resenias.stream()
            .map(r -> new Resenia(r.id(), r.localId(), r.area(), r.cadena(), r.nombreDelLocal(), r.creada(),
                r.comentario(), porReseniaId.getOrDefault(r.id(), 0), r.primeraConComentario()))
            .toList();
    }

    /**
     * El barrio donde esta cuenta es la que más reseñó, con un mínimo de dos (#314). Si hay
     * empate gana quien llegó primero a esa cantidad: el que tiene la última reseña más vieja.
     * Cuentan los locales con barrio y que no son cadenas. Null si no lo es en ninguno; si
     * lo es en varios, el primero por orden alfabético, para que sea siempre el mismo.
     */
    private String barrioDondeEsIntendente(Long id, List<Resenia> mias) {
        List<String> candidatos = mias.stream()
            .filter(r -> !r.cadena() && r.area() != null && !r.area().isBlank())
            .collect(java.util.stream.Collectors.groupingBy(Resenia::area, java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() >= 2)
            .map(java.util.Map.Entry::getKey)
            .sorted()
            .toList();
        List<String> donde = new ArrayList<>();
        for (String barrio : candidatos) {
            List<Object[]> todos = em.createQuery("""
                    select r.user.id, count(r), max(r.createdAt)
                    from Rating r join r.burgerJoint b
                    where b.area = :barrio and b.fastFood = false
                    group by r.user.id
                    """, Object[].class).setParameter("barrio", barrio).getResultList();
            Object[] ganador = todos.stream()
                .max(Comparator.<Object[], Long>comparing(f -> (Long) f[1])
                    .thenComparing(f -> (Instant) f[2], Comparator.reverseOrder()))
                .orElse(null);
            if (ganador != null && id.equals(ganador[0])) {
                donde.add(barrio);
            }
        }
        return donde.isEmpty() ? null : donde.get(0);
    }
}
