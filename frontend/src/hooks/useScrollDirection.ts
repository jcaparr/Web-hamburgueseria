import { useEffect, useRef, useState } from 'react'

/** true once the user has scrolled down past `hideAfter` and is still scrolling down. */
export function useHideOnScroll(hideAfter = 64, threshold = 8) {
  const [hidden, setHidden] = useState(false)
  const lastY = useRef(0)

  useEffect(() => {
    lastY.current = window.scrollY

    function onScroll() {
      const y = window.scrollY
      const diff = y - lastY.current
      if (Math.abs(diff) < threshold) return

      if (y < hideAfter) {
        setHidden(false)
      } else {
        setHidden(diff > 0)
      }
      lastY.current = y
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [hideAfter, threshold])

  return hidden
}
