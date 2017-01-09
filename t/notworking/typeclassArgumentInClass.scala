
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

// Version that works:
// class MonoidUser[A: Monoid, B: Semigroup](val monoid: A, val semi: B) {
// Version that doesn't work (because of the ordering)
class MonoidUser[B: Semigroup, A: Monoid](val monoid: A, val semi: B) {
  val double: A = monoid mappend monoid
  val empty: A = mempty
  val doubleSemi: B = semi mappend semi
  def triple(): A = double mappend monoid
}
