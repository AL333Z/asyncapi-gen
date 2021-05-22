/*
 * Copyright (c) 2021 al333z
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
