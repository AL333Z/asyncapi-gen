package asyncapigen

trait Printer[-T] {
  def print(t: T): String
}

object Printer {
  def apply[A](implicit instance: Printer[A]): Printer[A] =
    instance

  def print[A](f: A => String): Printer[A] =
    (a: A) => f(a)
}
