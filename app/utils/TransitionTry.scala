package utils

import scala.language.implicitConversions
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.annotation.tailrec

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

  def map2[B, C](tTryB: TransitionTry[S, B])(f: (A, B) => C): TransitionTry[S, C] = {
    flatMap { a => tTryB.map(b => f(a, b)) }
    //  Equivale a:
    //    TransitionTry[S, C] { s1 =>
    //      val TransitionResult(s2, tryA) = run(s1)
    //      tryA match {
    //        case Failure(e1) => TransitionResult(s2, Failure[C](e1))
    //        case Success(a) =>
    //          val TransitionResult(s3, tryB) = tTryB.run(s2)
    //          tryB match {
    //            case Failure(e2) => TransitionResult(s3, Failure[C](e2))
    //            case Success(b)  => TransitionResult(s3, Success(f(a, b)))
    //          }
    //      }
    //    }

  }
}

object TransitionTry {

  def unit[S, A](a: => A): TransitionTry[S, A] = TransitionTry { s1 => TransitionResult(s1, Try(a)) }

  def fail[S, A](e: Exception): TransitionTry[S, A] = TransitionTry { s1 => TransitionResult(s1, Failure(e)) }

  def sequence[S, A](l: List[TransitionTry[S, A]]): TransitionTry[S, List[A]] = {
    // Se podría haber implementado mas conciso así: l.foldLeft(TransitionTry[S, List[A]](s => TransitionResult(s, Success(Nil))))((sas, sa) => sa.map2(sas)(_ :: _)).map(_.reverse)
    // pero se prefirió la eficiencia
    TransitionTry[S, List[A]] { s1 =>
      @tailrec
      def loop(sp: S, acc: List[A], ltta: List[TransitionTry[S, A]]): TransitionResult[S, Try[List[A]]] = ltta match {
        case Nil => TransitionResult(sp, Success(acc))
        case h :: t =>
          val TransitionResult(sn, tryA) = h.run(sp)
          tryA match {
            case Failure(e) => TransitionResult(sn, Failure(e))
            case Success(a) => loop(sn, a :: acc, t)
          }
      }
      val TransitionResult(sf, x) = loop(s1, Nil, l)
      TransitionResult(sf, x.map(_.reverse))
    }
  }

  implicit def toPlainTransition[S, A](p: TransitionTry[S, A]): Transition[S, Try[A]] = Transition(p.run(_))
}