package com.hamburguesas.service;

import com.hamburguesas.auth.Usernames;
import com.hamburguesas.exception.UsernameTakenException;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Los nombres de usuario contra lo que ya está tomado.
 *
 * Las reglas de forma viven en {@link Usernames}, que no sabe de base de datos. Acá
 * está lo único que necesita mirarla: si queda libre.
 */
@Service
@RequiredArgsConstructor
public class UsernameService {

    /**
     * Cuántas variantes se prueban antes de aflojar.
     *
     * Es un número chico a propósito. Si con veinte no se encontró ninguna libre, el
     * problema no es la suerte sino que ese nombre es demasiado popular, y conviene
     * que la persona elija otro antes que darle "juan173".
     */
    private static final int VARIANTES = 20;

    private final UserRepository userRepository;

    /**
     * Normaliza lo que escribió y se asegura de que pueda quedárselo.
     *
     * @return el nombre tal como hay que guardarlo.
     */
    public String reservar(String crudo) {
        String nombre = Usernames.normalizar(crudo);

        if (!Usernames.tieneFormaValida(nombre)) {
            throw new UsernameTakenException(
                "Entre 3 y 20 caracteres, solo letras, números y guión bajo");
        }
        if (Usernames.esReservado(nombre) || userRepository.existsByUsername(nombre)) {
            // El mismo mensaje para los dos casos: que un nombre esté reservado no es
            // asunto de quien lo pidió, y para él la salida es la misma.
            throw new UsernameTakenException("Ese nombre de usuario ya está en uso");
        }
        return nombre;
    }

    /** Sin excepciones, para el campo del formulario mientras se escribe. */
    public boolean estaLibre(String crudo) {
        String nombre = Usernames.normalizar(crudo);
        return Usernames.tieneFormaValida(nombre)
            && !Usernames.esReservado(nombre)
            && !userRepository.existsByUsername(nombre);
    }

    /**
     * Uno libre parecido al que pidió, o vacío si no se encontró ninguno cerca.
     *
     * Sirve para proponerle algo a quien entra con Google, que no eligió nada todavía,
     * y para acompañar el "ya está en uso" con una salida en vez de solo la traba.
     */
    public String sugerirCerca(String base) {
        String raiz = Usernames.normalizar(base);
        if (estaLibre(raiz)) {
            return raiz;
        }
        for (int n = 2; n <= VARIANTES; n++) {
            String candidato = raiz + n;
            if (estaLibre(candidato)) {
                return candidato;
            }
        }
        return "";
    }
}
