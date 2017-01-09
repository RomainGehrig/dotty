import dotty.annotation._

@typeclass trait Monad[M[_]] {
  def unit[B](a: B): M[B]
  def flatMap[A,B](x: M[A], f: A => M[B]): M[B]
  def flatMap1[B,A](x: M[A], f: A => M[B]): M[B]
  @infix def flatMap2[A,B](x: M[A], f: A => M[B]): M[B]
}
