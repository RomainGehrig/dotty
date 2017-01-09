import dotty.annotation._

@typeclass trait Monad[M[_]] {
  @infix def flatMap3[B,A](x: M[A], f: A => M[B]): M[B] // Error because of 1st param stripped in infix methods
  @infix def unit1[B](a: B): M[B] // Error because of type param is set to A in infix methods
}
