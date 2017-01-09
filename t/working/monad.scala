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
  @infix def join[A,X](f: F[A])(implicit ev: A <:< F[X]): F[X] = flatMap(f, x => identity(x))
}

object OptionMonad extends Monad[Option] {
  override def flatMap[A,B](fa: Option[A], f: A => Option[B]): Option[B] = fa match {
    case Some(x) => f(x)
    case None => None
  }

  override def pure[A](a: A): Option[A] = Some(a)
  override def map[A, B](a: Option[A], f: A => B): Option[B] = a.flatMap(x => Some(f(x)))
  override def ap[A, B](fa: Option[A], f: Option[A => B], b: Option[B]): Option[B] = ???
}

object Use {
  implicit val op: Monad[Option] = OptionMonad

  def monadUnit[A, F[_]: Monad](a: A): F[A] = {
    unit(a)
  }

  def monadFlatMap[A, F[_]: Monad](f: F[A]): F[A] = {
    f flatMap pure
  }

  def monadJoin[A, F[_]: Monad](f: F[F[A]]): F[A] = {
    f.join()
  }

  def monadMap[A, F[_]: Monad](f: F[A]): F[A] = {
    // Workaround for `f map identity` (infix use of functor)
    Monad[F].map(f, identity)
  }

  def functorMap[A, F[_]: Functor](f: F[A]): F[A] = {
    f map identity
  }

  def main(args: Array[String]) = {
    val x: Option[_] = Some(10)
    println(monadFlatMap(x))
    println(monadMap(x))
    println(functorMap(x))
    println(monadUnit(20)) // only one implicit Monad object in the scope so no ambiguity
    val sx: Option[Option[_]] = Some(x)
    println(monadJoin(sx)(op))
    val y: Option[_] = None
    val sy: Option[Option[_]] = Some(y)
    println(monadFlatMap(y))
    println(monadMap(y))
    println(monadJoin(sy)(op))
    println(monadJoin(None)(op))
  }
}
