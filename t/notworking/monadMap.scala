import dotty.annotation._

@typeclass trait Functor[F[_]] {
  @infix def map[A,B](a: F[A], f: A => B): F[B]
}

@typeclass trait Applicative[F[_]] extends Functor[F] {
  def pure[A](a: A): F[A]
  @infix def ap[A,B](fa: F[A], f: F[A => B], b: F[B]): F[B]
}

@typeclass trait Monad[F[_]] extends Applicative[F] {
  def unit[A](a: A): F[A] = pure(a)
  @infix def flatMap[A,B](fa: F[A], f: A => F[B]): F[B]
}

object OptionMonad extends Monad[Option] {
  override def flatMap[A,B](fa: Option[A], f: A => Option[B]): Option[B] = fa match {
    case Some(x) => f(x)
    case None => None
  }

  override def ap[A, B](fa: Option[A], f: Option[A => B], b: Option[B]): Option[B] = ???
  override def pure[A](a: A): Option[A] = ???
  override def map[A, B](a: Option[A], f: A => B): Option[B] = ???
}

object Use {
  implicit val op: Monad[Option] = OptionMonad

  def monadMap[A, F[_]: Monad](f: F[A]): F[A] = {
    f map identity
    // Workaround: Monad[F].map(f, identity)
  }

  def main(args: Array[String]) = {
    val x: Option[_] = Some(10)
    println(monadMap(x))
  }
}
