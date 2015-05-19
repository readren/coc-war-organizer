package utils


case class TypedTransition[-S1, S2, +A](run: S1 => TransitionResult[S2,A]) {

  def map[B](f: A => B) = TypedTransition[S1, S2, B] (s1 => {
      val TransitionResult(s2, a) = run(s1)
      TransitionResult(s2, f(a))
    })

  def flatMap[B, S3](g: A => TypedTransition[S2, S3, B]) = TypedTransition[S1, S3, B] (s1 => {
      val TransitionResult(s2, a) = run(s1)
      g(a).run(s2)
    })

  def map2[B, C, S3](s: TypedTransition[S2, S3, B])(f: (A, B) => C): TypedTransition[S1, S3, C] = {
    flatMap[C,S3](a => s.map(b => f(a, b)))
  }

}

object TypedTransition {
  // Ejercicio 11
  def unit[A, S](a: A): TypedTransition[S, S, A] = TypedTransition((s:S) => TransitionResult(s,a))

  def getState[S]: TypedTransition[S, S, S] =
    TypedTransition((s:S) => TransitionResult(s, s))

  def setState[S](s: S): TypedTransition[S, S, Unit] =
    TypedTransition[S, S, Unit](_ => TransitionResult(s, ()))

  def modify[S](f: S => S): TypedTransition[S, S, Unit] =
    for {
      s <- getState[S]
      _ <- setState(f(s))
    } yield ()

  def sequence[S, A](l: List[TypedTransition[S, S, A]]): TypedTransition[S, S, List[A]] =
    l.foldRight(TypedTransition[S, S, List[A]](s => TransitionResult(s, Nil)))((sa, sas) => sa.map2(sas)(_ :: _))

}