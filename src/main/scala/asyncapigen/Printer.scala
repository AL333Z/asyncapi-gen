package asyncapigen

trait Printer[-T] {
  def print(t: T): String
}

object Printer {
  def apply[A](implicit instance: Printer[A]): Printer[A] =
    instance

  def print[A](f: A => String): Printer[A] =
    (a: A) => f(a)

  trait Ops[A] {
    def typeClassInstance: Printer[A]
    def self: A
    def print: String = typeClassInstance.print(self)
  }

  trait ToPrinterOps {
    implicit def toPrinter[A](target: A)(implicit tc: Printer[A]): Ops[A] =
      new Ops[A] {
        val self: A                       = target
        val typeClassInstance: Printer[A] = tc
      }
  }

  implicit def listPrinter[A: Printer]: Printer[List[A]] =
    Printer.print[List[A]](_.map(Printer[A].print(_)).mkString("\n"))

  object syntax extends ToPrinterOps
}
