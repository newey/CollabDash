package utilities

/**
 * Created by robert on 29/09/2014.
 */
object Timing {
  def apply[A](a: => A) : (A, Long) = {
    val now = System.nanoTime
    val result = a
    val micros = (System.nanoTime - now) / 1000
    (result, micros)
  }
}
