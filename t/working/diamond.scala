import dotty.annotation._

@typeclass trait Functor[F[_]] {
  @infix def infixMap[A,B](fa: F[A], f: A => B): F[B]
  def map[A,B](fa: F[A], f: A => B): F[B]
}

@typeclass trait Applicative[F[_]] extends Functor[F] {
  override def infixMap[A,B](fa: F[A], f: A => B): F[B] = ???
  override def map[A,B](fa: F[A], f: A => B): F[B] = ???

  @infix def qq[A](fa: F[A]): F[A] = ???
  def qq2[A](fa: F[A]): F[A] = ???
}

@typeclass trait Monad[F[_]] extends Applicative[F] {
  def pure[A](x: A): F[A]
}

@typeclass trait Traversable[F[_]] extends Functor[F] {
  override def infixMap[A,B](fa: F[A], f: A => B): F[B] = ???
  override def map[A,B](fa: F[A], f: A => B): F[B] = ???
}

object Use {
  def foo[A, B, F[_]: Applicative, F2[_]: Traversable](x: F[A], y: F2[A], f1: A => B): Unit = {
    // No problem with infix as the corret implicit class is selected
    x.infixMap(f1)
    y.infixMap(f1)
    // Problem with non infix methods because of shadowing when importing.
    // Note: depending on the param order the situation can change
    Applicative[F].map(x, f1) // This one would no be needed because the correct map exists in scope
    Traversable[F2].map(y, f1) // This one was shadowed
  }

  // We have to fully specify which function we want
  def bar[F[_]: Monad: Traversable]: F[Int] = {
    Monad[F].map(Monad[F].pure(10), identity)
  }

  // check that both Ops class are available
  def bar2[F[_]: Applicative: Functor](x: F[Int]): F[Int] = {
    x.infixMap(identity).qq()
    x.qq().infixMap(identity)
  }
}
