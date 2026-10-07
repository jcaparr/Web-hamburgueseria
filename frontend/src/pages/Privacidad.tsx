import { Link } from 'react-router-dom'
import { Apartado, Correo, Lista, PaginaLegal } from '../components/PaginaLegal'
import { useDescripcion } from '../hooks/useMetadatos'
import { useTitulo } from '../hooks/useTitulo'
import { DOMICILIO, RESPONSABLE } from '../utils/legal'

/**
 * La política de privacidad, como pide la Ley 25.326 de Protección de los Datos
 * Personales: quién es responsable, qué datos se juntan y para qué, con quién se
 * comparten y cómo ejercer los derechos sobre ellos.
 *
 * Dice solo lo que la app hace de verdad. Si algo de esto cambia en el código —un dato
 * nuevo, un proveedor nuevo, una cookie más—, este texto cambia con él.
 */
export function Privacidad() {
  useTitulo('Política de privacidad')
  useDescripcion('Qué datos guarda Burgómetro, para qué los usa y cómo podés verlos, corregirlos o borrarlos.')

  return (
    <PaginaLegal
      titulo="Política de privacidad"
      bajada="Qué datos guardamos, para qué y qué podés hacer con ellos. Sin letra chica: no vendemos datos ni mostramos publicidad."
    >
      <Apartado titulo="Quién es responsable">
        <p>
          Burgómetro es un proyecto independiente de {RESPONSABLE}, con domicilio en {DOMICILIO}, que es
          responsable de la base de datos de usuarios. Para cualquier consulta sobre tus datos escribinos a{' '}
          <Correo />.
        </p>
      </Apartado>

      <Apartado titulo="Qué datos guardamos">
        <p>
          Para mirar las hamburgueserías, el ranking o armar un recorrido no hace falta cuenta, y no guardamos nada
          que te identifique: solo contamos la visita, de forma anónima (ver «Estadísticas de visitas», más abajo).
        </p>
        <p>Si creás una cuenta, guardamos:</p>
        <Lista>
          <li>
            <strong>Tu mail y tu nombre de usuario.</strong> Si entrás con Google, también el identificador de tu
            cuenta de Google. No recibimos tu contraseña de Google, tus contactos ni nada más.
          </li>
          <li>
            <strong>Tu contraseña</strong>, si elegís una, guardada con un cifrado que no se puede revertir: ni
            nosotros podemos leerla.
          </li>
          <li>
            <strong>Lo que hacés en la app:</strong> tus reseñas (la nota, el comentario y la foto), las
            hamburgueserías que guardás, tus recorridos, a quién seguís, a quién bloqueás y la hamburguesa de tu
            avatar.
          </li>
        </Lista>
        <p>
          Las fotos que subís se vuelven a guardar sin sus datos ocultos: una foto de celular suele llevar adentro
          el lugar y la hora en que se sacó, y eso se borra antes de guardarla.
        </p>
        <p>
          Si usás «Cerca mío» al armar un recorrido, tu ubicación se usa en ese momento para elegir las paradas y
          no queda guardada en tu cuenta.
        </p>
      </Apartado>

      <Apartado titulo="Para qué los usamos">
        <Lista>
          <li>Para que puedas entrar a tu cuenta y usar la app.</li>
          <li>Para mostrar tus reseñas y tu perfil a los demás.</li>
          <li>Para mandarte los códigos de verificación y de cambio de contraseña. No mandamos promociones.</li>
          <li>Para cuidar la app: frenar a quien intente adivinar contraseñas o abusar del servicio.</li>
          <li>Para saber cuánta gente usa Burgómetro y qué pantallas mira, con estadísticas anónimas.</li>
        </Lista>
        <p>No vendemos ni alquilamos tus datos, y no los usamos para publicidad.</p>
      </Apartado>

      <Apartado titulo="Qué ven los demás">
        <p>
          Cualquiera que entre a Burgómetro puede ver tu nombre de usuario, tu avatar y tus reseñas. Quienes tienen
          cuenta además pueden ver tu perfil: cuántas reseñas tenés, tu promedio, a cuánta gente seguís y quiénes te
          siguen.
        </p>
        <p>Tu mail, las hamburgueserías que guardaste y tus recorridos no los ve nadie más que vos.</p>
      </Apartado>

      <Apartado titulo="Con quién se comparten">
        <p>Con nadie para su propio uso. Para funcionar usamos estos servicios, que tratan los datos solo para darnos ese servicio:</p>
        <Lista>
          <li>
            <strong>Oracle Cloud</strong>, donde están el servidor y la base de datos, en São Paulo, Brasil. Al
            usar Burgómetro aceptás que tus datos se guarden ahí.
          </li>
          <li>
            <strong>Google</strong>, si entrás con tu cuenta de Google.
          </li>
          <li>
            <strong>Brevo</strong>, que envía los mails con los códigos: recibe tu mail y el mensaje que te
            mandamos, nada más.
          </li>
          <li>
            <strong>Cloudflare</strong>, que maneja la dirección del sitio y reenvía los mails que nos escribís a{' '}
            <Correo />.
          </li>
        </Lista>
        <p>Solo entregaríamos datos a una autoridad si una ley o un juez nos lo exigen.</p>
      </Apartado>

      <Apartado titulo="Estadísticas de visitas">
        <p>
          Contamos las visitas con Umami, una herramienta que corre en nuestro propio servidor: los datos no pasan
          por ninguna otra empresa. No usa cookies y no guarda tu dirección IP.
        </p>
        <p>
          De cada visita queda la pantalla que se vio (sin lo que hayas escrito en el buscador), desde qué sitio
          llegaste, el navegador, el sistema, el tipo de dispositivo, el tamaño de la pantalla, el idioma y el país
          y la ciudad aproximados. Para no contar dos veces a la misma persona usa un identificador anónimo que
          cambia todos los meses, así que no hay forma de seguir a nadie de un mes a otro ni de saber qué visitas
          son tuyas.
        </p>
        <p>Si tu navegador tiene activado «No rastrear» (Do Not Track), tus visitas no se cuentan.</p>
      </Apartado>

      <Apartado titulo="Cookies y lo que se guarda en tu navegador">
        <p>
          Usamos solo lo necesario para que la app funcione. No hay cookies de publicidad, de seguimiento ni de
          estadísticas.
        </p>
        <Lista>
          <li>
            <strong>Dos cookies de sesión</strong>, para que no tengas que entrar cada vez: una dura 15 minutos y
            la otra hasta 30 días, o hasta que cierres la sesión. El navegador no deja que ningún script las lea.
          </li>
          <li>
            <strong>En tu navegador</strong> quedan el tema que elegiste (claro u oscuro) y si querés ver las
            cadenas de comida rápida en Explorar.
          </li>
          <li>
            <strong>El botón de Google</strong>, en las pantallas de entrar y crear cuenta, pone sus propias
            cookies. Las maneja Google, según su política.
          </li>
        </Lista>
      </Apartado>

      <Apartado titulo="Cuánto tiempo los guardamos">
        <p>
          Mientras tengas la cuenta. Si pedís borrarla, borramos tu cuenta, tus reseñas y tus fotos. Las copias de
          seguridad se pisan solas a los 14 días, así que ahí desaparece también cualquier resto.
        </p>
        <p>
          El servidor anota cada pedido que recibe, con su dirección IP, para poder investigar abusos y fallas.
          Esos registros no se usan para otra cosa.
        </p>
      </Apartado>

      <Apartado titulo="Tus derechos">
        <p>Podés pedirnos en cualquier momento:</p>
        <Lista>
          <li>ver qué datos tuyos tenemos,</li>
          <li>corregirlos o actualizarlos,</li>
          <li>borrar tu cuenta y todo lo que publicaste.</li>
        </Lista>
        <p>
          Escribinos a <Correo /> desde el mail de tu cuenta, para que sepamos que sos vos. Te contestamos dentro de
          los plazos de la ley: 10 días corridos para darte acceso a tus datos y 5 días hábiles para corregirlos o
          borrarlos.
        </p>
        <p className="text-sm text-base-content/80">
          El titular de los datos personales tiene la facultad de ejercer el derecho de acceso a los mismos en forma
          gratuita a intervalos no inferiores a seis meses, salvo que se acredite un interés legítimo al efecto
          conforme lo establecido en el artículo 14, inciso 3 de la Ley N° 25.326. La Agencia de Acceso a la
          Información Pública, en su carácter de Órgano de Control de la Ley N° 25.326, tiene la atribución de
          atender las denuncias y reclamos que interpongan quienes resulten afectados en sus derechos por
          incumplimiento de las normas vigentes en materia de protección de datos personales.
        </p>
      </Apartado>

      <Apartado titulo="Cómo los cuidamos">
        <p>
          Toda la comunicación con Burgómetro va cifrada (HTTPS), las contraseñas se guardan con un cifrado que no
          se puede revertir y la base de datos no es accesible desde internet. Ningún sistema es infalible: si
          alguna vez hubiera un problema que afecte tus datos, te vamos a avisar.
        </p>
      </Apartado>

      <Apartado titulo="Menores de edad">
        <p>Burgómetro no está pensado para menores de 13 años, y no les pedimos que creen una cuenta.</p>
      </Apartado>

      <Apartado titulo="Cambios en esta política">
        <p>
          Si cambiamos algo importante, lo vas a ver en la app antes de que se aplique. La fecha de arriba dice
          cuándo se actualizó por última vez. Las reglas para usar Burgómetro están en los{' '}
          <Link to="/terminos" className="link">
            términos y condiciones
          </Link>
          .
        </p>
      </Apartado>
    </PaginaLegal>
  )
}
