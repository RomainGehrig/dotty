import dotty.annotation._

@typeclass trait Semigroup[A] {
  @infix def mappend(x: A, y: A): A
}

@typeclass trait Monoid[A] extends Semigroup[A] {
  def mempty(): A
}

object IntMonoid extends Monoid[Int] {
  def mappend(x: Int, y: Int): Int = x + y
  def mempty(): Int = 0
}

object Use {
  def fold[A: Monoid](xs: List[A]): A =
    xs.fold(mempty)(_ mappend _)

  def fold1[A: Semigroup](xs: List[A]): A =
    xs.reduce(_ mappend _)

  def main(args: Array[String]): Unit = {
    implicit val mon: Monoid[Int] = IntMonoid
    println(fold(List(1,2,4,5,6,7,8,9,10)))
  }
}
