import dotty.annotation._

@typeclass trait X[A] {
  @infix def map(x: A, f: A => A): A
  def pure(x: A): X[A]
}

@typeclass trait Y[A] extends X[A] {
  @infix def map(x: A, f: A => A): A = {
    println("Y[A]")
    x
  }
  def pure(x: A): Y[A] = ???
}

@typeclass trait Y2[A] extends X[A] {
  @infix def map(x: A, f: A => A): A = {
    println("Y2[A]")
    x
  }
  def pure(x: A): Y2[A] = ???
}

@typeclass trait Z[A] extends Y[A] with Y2[A] {
  @infix override def map(x: A, f: A => A): A = {
    println("Z[A]")
    x
  }
  override def pure(x: A): Z[A] = ???
}

object Use {
  def f2[B: Y, A: Y2](x: A, y: B): Unit = {
    x.map(identity(_))
    y.map(identity(_))
  }

  def diamond[Q: Z](q: Q): Unit = {
    q.map(identity(_))
  }
}
