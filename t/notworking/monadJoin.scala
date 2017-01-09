
import dotty.annotation._

@typeclass trait Monad[F[_]] {
  def pure[A](a: A): F[A]
  @infix def flatMap[A,B](fa: F[A], f: A => F[B]): F[B]
  @infix def join[A,X](f: F[X])(implicit ev: X <:< F[A]): F[A] = f.flatMap(ev)
}

object OptionMonad extends Monad[Option] {
  override def pure[A](a: A): Option[A] = Some(a)
  override def flatMap[A,B](fa: Option[A], f: A => Option[B]): Option[B] = fa match {
    case Some(x) => f(x)
    case None => None
  }
}

object Use {
  implicit val op: Monad[Option] = OptionMonad

  def monadJoin[A, F[_]: Monad](f: F[A]): F[A] = {
    pure(f).join
  }

  def main(args: Array[String]) = {
    val x: Option[_] = Some(10)
    println(monadJoin(x))
  }
}
