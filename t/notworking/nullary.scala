
import dotty.annotation._

@typeclass trait Foo {
  def toInt(): Int
  def run[A](x: => A): A
}
