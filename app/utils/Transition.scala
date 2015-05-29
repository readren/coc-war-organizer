package utils

import scala.util.Failure
import scala.util.Try
import scala.util.Success
import scala.language.implicitConversions

case class TransitionResult[+S, +P](state: S, product: P)

/**
 * This class is a tool that helps to factor out and postpone all effectful operations. This allows to make all the business be pure (referentially transparent), and the side effect be isolated.
 * @param run is the operation that this transition performs. It receives an arbitrary state and gives the next state and a product, which uses to be the the goal of the transition.
 */
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
	
	def filter(p: P => Boolean): Option[P] = {
		???
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

	/** */
	def tryout[S, A](t: Transition[S, Try[A]]): Try[Transition[S, A]] = Try { t.map(_.get) }
	/**A shorthand of <code>unit(Failure(exception))</code> */
	def failure[S, A](exception: Throwable): Transition[S, Try[A]] = unit(Failure(exception))
	/**A shorthand of <code>unit(Success(a))</code> */
	def success[S, A](a: A): Transition[S, Try[A]] = unit(Success(a))

	def mapTry[S, A, B](z: Transition[S, Try[A]])(f: A => B): Transition[S, Try[B]] = z.map(_.map(f))

	def flatMapTry[S, A, B](z: Transition[S, Try[A]])(f: A => Transition[S, B]): Transition[S, Try[B]] = Transition[S, Try[B]] { s1 =>
		val TransitionResult(s2, tryA) = z.run(s1)
		tryA match {
			case Success(a) => f(a).map(Success(_)).run(s2)
			case Failure(e) => TransitionResult(s2, Failure[B](e))
		}
	}

	implicit def toTransitionTry[S, A](t: Transition[S, Try[A]]): TransitionTry[S, A] = TransitionTry(t.run(_))
}

case class TransitionTry[S, +A](run: S => TransitionResult[S, Try[A]]) {
	def map[B](f: A => B): TransitionTry[S, B] = TransitionTry[S, B] { s1 =>
		val TransitionResult(s2, tryA) = run(s1)
		TransitionResult(s2, tryA.map(f))
	}

	def flatMap[B](f: A => TransitionTry[S, B]): TransitionTry[S, B] = TransitionTry[S, B] { s1 =>
		val TransitionResult(s2, tryA) = run(s1)
		tryA match {
			case Success(a) => f(a).run(s2)
			case Failure(e) => TransitionResult(s2, Failure[B](e))
		}
	}
}

object TransitionTry {

	def unit[S, A](a: A): TransitionTry[S, A] = TransitionTry { s1 => TransitionResult(s1, Success(a)) }

	implicit def toPlainTransition[S, A](p: TransitionTry[S, A]): Transition[S, Try[A]] = Transition(p.run(_))
}