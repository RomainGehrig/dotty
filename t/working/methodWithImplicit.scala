
import dotty.annotation._

@typeclass trait Foo[F[_]] {
  def map[A,B](a: F[A], f: A => B)(implicit x: F[A], y: F[B]): F[B]
  @infix def map1[A,B](a: F[A], f: A => B)(implicit x: F[A], y: F[B]): F[B]
}
