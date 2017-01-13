import dotty.annotation._

@typeclass trait Monad[F[_]] {
  @infix def join[X](f: F[F[X]]): F[X]
  @infix def flatMap[A,B](fa: F[A], f: A => F[B]): F[B]
}

object OptionMonad extends Monad[Option] {
  override def flatMap[A,B](fa: Option[A], f: A => Option[B]): Option[B] = fa match {
    case Some(x) => f(x)
    case None => None
  }
}

object Use {
  implicit val opt: Monad[Option] = OptionMonad
  def main(args: Array[String]): Unit = {
    val x: Option[Option[Int]] = Some(Some(20))
    println(opt.join(x))
  }
}
