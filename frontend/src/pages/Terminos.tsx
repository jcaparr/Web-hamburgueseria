import { Link } from 'react-router-dom'
import { Apartado, Correo, Lista, PaginaLegal } from '../components/PaginaLegal'
import { useDescripcion } from '../hooks/useMetadatos'
import { useTitulo } from '../hooks/useTitulo'
import { RESPONSABLE } from '../utils/legal'

/**
 * Las reglas para usar Burgómetro: qué es, qué se puede publicar y de qué se hace cargo
 * cada uno. Es el aviso legal de la web.
 *
 * Escrito para que se entienda de una leída. Lo que importa de verdad —que las
 * reseñas sean experiencias reales y que no se publique nada de nadie sin permiso— va
 * primero y con ejemplos, no al final en un párrafo que nadie lee.
 */
export function Terminos() {
  useTitulo('Términos y condiciones')
  useDescripcion('Las reglas para usar Burgómetro: qué se puede publicar, cómo se usan las reseñas y de qué se hace cargo cada uno.')

  return (
    <PaginaLegal
      titulo="Términos y condiciones"
      bajada="Las reglas para usar Burgómetro. Al crear una cuenta o publicar una reseña, las aceptás."
    >
      <Apartado titulo="Qué es Burgómetro">
        <p>
          Burgómetro es una guía de las hamburgueserías de Buenos Aires con las notas y reseñas de quienes fueron. Es
          un proyecto independiente de {RESPONSABLE}: no tiene relación con los locales que aparecen, no cobra por
          figurar ni por mejorar una nota, y no está asociado a Google.
        </p>
      </Apartado>

      <Apartado titulo="Tu cuenta">
        <Lista>
          <li>Para reseñar, guardar o seguir gente necesitás una cuenta. Para mirar, no.</li>
          <li>
            Elegí un nombre de usuario que no se haga pasar por otra persona, por un local o por Burgómetro.
          </li>
          <li>
            Cuidá tu contraseña: lo que se haga desde tu cuenta corre por tu cuenta. Si creés que alguien entró,
            cambiala y avisanos.
          </li>
          <li>
            Podés borrar tu cuenta cuando quieras escribiéndonos a <Correo />. Se borran tu cuenta, tus reseñas y
            tus fotos.
          </li>
        </Lista>
      </Apartado>

      <Apartado titulo="Lo que publicás">
        <p>Una reseña tiene que contar tu experiencia real en ese local. No se permite:</p>
        <Lista>
          <li>reseñar tu propio local, el de un familiar o el de la competencia;</li>
          <li>publicar reseñas a cambio de plata, comida gratis u otro beneficio, o reseñas inventadas;</li>
          <li>insultar, discriminar, amenazar o acosar a nadie, sea cliente, empleado o dueño;</li>
          <li>
            publicar datos personales de otras personas, o fotos en las que se las reconozca sin que hayan dado
            permiso;
          </li>
          <li>subir fotos que no sacaste vos o que no tenés derecho a usar;</li>
          <li>hacer publicidad, mandar spam o publicar enlaces a otros sitios;</li>
          <li>publicar cualquier cosa que sea ilegal.</li>
        </Lista>
        <p>
          Podemos sacar lo que no cumpla estas reglas y suspender o cerrar las cuentas que las incumplan, sin aviso
          previo si el caso es grave. Si ves algo que no debería estar, escribinos a <Correo />.
        </p>
        <p>
          Lo que publicás sigue siendo tuyo. Al publicarlo nos das permiso, gratis y sin exclusividad, para
          mostrarlo en Burgómetro mientras siga publicado. Si lo borrás, o borrás tu cuenta, ese permiso se
          termina.
        </p>
      </Apartado>

      <Apartado titulo="Los datos de los locales">
        <p>
          El nombre, la dirección, el horario y la foto de cada local salen de Google Maps y pueden tener errores o
          estar desactualizados. Antes de ir, conviene confirmar que esté abierto.
        </p>
        <p>
          Las notas y las reseñas son la opinión de cada persona que las escribió, no la de Burgómetro. Si tenés un
          local y algún dato está mal, escribinos y lo revisamos.
        </p>
      </Apartado>

      <Apartado titulo="De qué nos hacemos cargo">
        <p>
          Hacemos lo posible para que Burgómetro funcione bien y la información sea útil, pero la web se ofrece tal
          como está: puede tener errores o dejar de funcionar un rato. No somos responsables por lo que publiquen
          otros usuarios ni por tu experiencia en los locales.
        </p>
        <p>
          Nada de esto limita los derechos que te da la Ley 24.240 de Defensa del Consumidor ni otras leyes que no
          se puedan dejar de lado por un acuerdo.
        </p>
      </Apartado>

      <Apartado titulo="La marca y el contenido de la web">
        <p>
          El nombre Burgómetro, el logo, el diseño y el código de la web son de su titular. No se pueden copiar ni
          usar para otro sitio o aplicación sin permiso. Tampoco se permite bajar de forma automática el contenido
          de la web, ni usarla de una manera que la sobrecargue o la deje sin funcionar para los demás.
        </p>
      </Apartado>

      <Apartado titulo="Tus datos">
        <p>
          Cómo guardamos y usamos tus datos está en la{' '}
          <Link to="/privacidad" className="link">
            política de privacidad
          </Link>
          .
        </p>
      </Apartado>

      <Apartado titulo="Cambios y ley aplicable">
        <p>
          Si cambiamos estos términos, lo vas a ver en la app antes de que se apliquen. Si seguís usando Burgómetro
          después, quiere decir que los aceptás. Estos términos se rigen por las leyes de la República Argentina.
        </p>
      </Apartado>
    </PaginaLegal>
  )
}
