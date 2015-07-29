/**
 *
 */
package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
 * @author Gustavo
 *
 */
/**A tool that helps to separate effectful code that operates with the DB from the rest of the program  */
trait TransacTransitionExec {
  val initialTransacMode: OutTransacMode

  def auto[X](block: Transition[TransacMode, X]): X = block.run(initialTransacMode).product
  def autoFuture[X](ec: ExecutionContext)(block: Transition[TransacMode, X]): Future[X] = Future(auto(block))(ec)

  /**Wraps the received block in a transaction */
  def withTransactionStrict[X](block: Transition[InTransacMode, X]): X = {
    TransacTransitionExec.wrapInTransactionStrict(initialTransacMode, block).product
  }

  /**Wraps the received block in a transaction */
  def withTransaction[X](block: Transition[TransacMode, X]): X = {
    TransacTransitionExec.wrapInTransaction(initialTransacMode, block).product
  }
}

object TransacTransitionExec {

  private def wrapInTransactionStrict[X](out: OutTransacMode, block: Transition[InTransacMode, X]): TransitionResult[OutTransacMode, X] = {
    val itm = out.begin
    try {
      val TransitionResult(ts3, x) = block.run(itm)
      TransitionResult(ts3.commit, x)
    } catch {
      case NonFatal(e) => {
        itm.rollback
        throw e
      }
    }
  }

  /**Wraps the received block in a transaction */
  def withTransactionStrict[X](block: Transition[InTransacMode, X]): Transition[OutTransacMode, X] = Transition[OutTransacMode, X] { out =>
     wrapInTransactionStrict(out, block)
  }

  /**Wraps the received block in a transaction only if it isn't already wrapped.*/
  def inTransactionStrict[X](block: Transition[InTransacMode, X]): Transition[TransacMode, X] = Transition[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransactionStrict(out, block)
  }
  

  private def wrapInTransaction[X](out: OutTransacMode, block: Transition[TransacMode, X]): TransitionResult[OutTransacMode, X] = {
    val itm = out.begin
    try {
      val TransitionResult(ts3, x) = block.run(itm)
      TransitionResult(ts3.asInstanceOf[InTransacMode].commit, x)
    } catch {
      case NonFatal(e) => {
        itm.rollback
        throw e
      }
    }
  }

  /**Wraps the received block in a transaction */
  def withTransaction[X](block: Transition[TransacMode, X]): Transition[OutTransacMode, X] = Transition[OutTransacMode, X] {
    case in: InTransacMode =>
      in.rollback
      throw new IllegalStateException
    case out: OutTransacMode => wrapInTransaction(out, block)
  }

  /**Wraps the received block in a transaction only if it isn't already wrapped.*/
  def inTransaction[X](block: Transition[TransacMode, X]): Transition[TransacMode, X] = Transition[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransaction(out, block)
  }
  
  /**Wraps the received block in a transaction */
  def withTransactionTry[X](block: TransitionTry[TransacMode, X]): TransitionTry[OutTransacMode, X] = TransitionTry[OutTransacMode, X] {
    case in: InTransacMode =>
      in.rollback
      throw new IllegalStateException
    case out: OutTransacMode => wrapInTransaction(out, block)
  }

  /**Wraps the received block in a transaction only if it isn't already wrapped.*/
  def inTransactionTry[X](block: TransitionTry[TransacMode, X]): TransitionTry[TransacMode, X] = TransitionTry[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransaction(out, block)
  }
  

}