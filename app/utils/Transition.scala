package utils

case class TransitionResult[+S, +P](state: S, product: P)

/**This class is a tool that helps to factor out and postpone all the data base operations. This allows to make all the business be pure (referentially transparent), and the side effect be isolated.  
 * @param run is the operation that this transition performs. It receives an arbitrary state and gives the next state and the product, which uses to be the the goal of the transition.*/
case class Transition[S, +P](run: S => TransitionResult[S, P]) {

	def map[B](f: P => B) = Transition[S, B](s1 => {
		val TransitionResult(s2, a) = run(s1)
		TransitionResult(s2, f(a))
	})

	def flatMap[B](g: P => Transition[S, B]) = Transition[S, B](s1 => {
		val TransitionResult(s2, a) = run(s1)
		g(a).run(s2)
	})

	def map2[B, C](s: Transition[S, B])(f: (P, B) => C): Transition[S, C] = {
		flatMap[C](a => s.map(b => f(a, b)))
	}

}

object Transition {

	def unit[S, A](a: A) = Transition[S, A](s => TransitionResult(s, a))
	
	def getState[S]: Transition[S, S] =
		Transition((s: S) => TransitionResult(s, s))

	def setState[S](s: S): Transition[S, Unit] =
		Transition[S, Unit](_ => TransitionResult(s, ()))

	def modify[S](f: S => S): Transition[S, Unit] =
		for {
			s <- getState[S]
			_ <- setState(f(s))
		} yield ()

	def sequence[S, A](l: List[Transition[S, A]]): Transition[S, List[A]] =
		l.foldRight(Transition[S, List[A]](s => TransitionResult(s, Nil)))((sa, sas) => sa.map2(sas)(_ :: _))

}