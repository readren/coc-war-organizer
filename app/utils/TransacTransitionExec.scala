/**
 *
 */
package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.Success
import scala.util.Failure
import scala.util.Try

import TransactionalTransition._

/**
 * @author Gustavo
 *
 */
/**A tool that helps to separate, from the rest of the program, effectful code that operates on a transactional information system  */
trait TransacTransitionExec {
  val initialTransacMode: OutTransacMode

  def auto[X](block: TiTac[X]): X = block.run(initialTransacMode).product
  def autoFuture[X](ec: ExecutionContext)(block: TiTac[X]): Future[X] = Future(auto(block))(ec)

  /**Wraps the received block in a transaction */
  def withTransactionStrict[X](block: TiInTac[X]): X = {
    TransacTransitionExec.wrapInTransactionStrict(initialTransacMode, block).product
  }

  /**Wraps the received block in a transaction */
  def withTransaction[X](block: TiTac[X]): X = {
    TransacTransitionExec.wrapInTransaction(initialTransacMode, block).product
  }
}

object TransacTransitionExec {

  private def wrapInTransactionStrict[X](out: OutTransacMode, block: TiInTac[X]): TransitionResult[OutTransacMode, X] = {
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

  /**
   * Wraps the received Transition in a transaction. The transaction is automatically rolled back only if the execution of the received Transition ends abruptly.
   * Note that the Transition is received by value (not by name), so, this operation is unable to manage exceptions thrown during its evaluation.
   */
  def withTransactionStrict[X](block: TiInTac[X]): TiOutTac[X] = Transition[OutTransacMode, X] { out =>
    wrapInTransactionStrict(out, block)
  }

  /**Wraps the received Transition in a transaction only if it isn't already wrapped. The transaction is automatically rolled back only if the execution of the received Transition ends abruptly*/
  def inTransactionStrict[X](block: TiInTac[X]): TiTac[X] = Transition[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransactionStrict(out, block)
  }

  /**
   * Wraps the received Transition in a transaction which is automatically rolled back only if the execution of the received Transition ends abruptly.
   * Note that the Transition is received by value (not by name). If it was by name and the evaluation of the received expression threw an exception, a fruitless transaction would be opened in vain.
   */
  private def wrapInTransaction[X](out: OutTransacMode, block: TiTac[X]): TransitionResult[OutTransacMode, X] = {
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

  /**
   * Wraps the received Transition in a transaction. The transaction is automatically rolled back only if the execution (run) of the received Transition ends abruptly.
   * Note that the Transition is received by value (not by name). If it was by name and the evaluation of the received expression threw an exception, a fruitless transaction would be opened in vain.
   */
  def withTransaction[X](block: TiTac[X]): TiOutTac[X] = Transition[OutTransacMode, X] {
    case in: InTransacMode =>
      in.rollback
      throw new IllegalStateException
    case out: OutTransacMode => wrapInTransaction(out, block)
  }

  /**
   * Wraps the received Transition in a transaction only if it isn't already wrapped. The transaction is automatically rolled back only if the execution (run) of the received Transition ends abruptly.
   * Note that the Transition is received by value (not by name). If it was by name and the evaluation of the received expression threw an exception, a fruitless transaction would be opened in vain.
   */
  def inTransaction[X](block: TiTac[X]): TiTac[X] = Transition[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransaction(out, block)
  }

  /**
   * Wraps the received block in a transaction which is automatically rolled back either if the execution (run) of the received TransitionTry ends abruptly or gives a TransitionResult(_, Failure(_)).
   * Note that the Transition is received by value (not by name). If it was by name and the evaluation of the received expression threw an exception, a fruitless transaction would be opened in vain.
   */
  private def wrapInTransactionTry[X](out: OutTransacMode, block: TtTm[X]): TransitionResult[OutTransacMode, Try[X]] = {
    val itm = out.begin
    try {
      val TransitionResult(ts2, tryX) = block.run(itm)
      val ts3 =
        if (tryX.isSuccess) ts2.asInstanceOf[InTransacMode].commit
        else ts2.asInstanceOf[InTransacMode].rollback
      TransitionResult(ts3, tryX)
    } catch {
      case NonFatal(e2) => {
        itm.rollback
        throw e2
      }
    }
  }

  /**
   * Wraps the received block in a transaction. The transaction is automatically rolled back either if the execution (run) of the received TransitionTry ends abruptly or gives a TransitionResult(_, Failure(_)).
   * Note that the Transition is received by value (not by name), so, this operation is unable to manage exceptions thrown during its evaluation.
   */
  def withTransactionTry[X](block: TtTm[X]): TtOutTm[X] = TransitionTry[OutTransacMode, X] {
    case in: InTransacMode =>
      in.rollback
      throw new IllegalStateException
    case out: OutTransacMode => wrapInTransactionTry(out, block)
  }

  /**
   * Wraps the received block in a transaction only if it isn't already wrapped. The transaction is automatically rolled back either if the execution of the received TransitionTry ends abruptly or gives a TransitionResult(_, Failure(_)).
   * Note that the Transition is received by value (not by name), so, this operation is unable to manage exceptions thrown during its evaluation.
   */
  def inTransactionTry[X](block: TtTm[X]): TtTm[X] = TransitionTry[TransacMode, X] {
    case in: InTransacMode   => block.run(in)
    case out: OutTransacMode => wrapInTransactionTry(out, block)
  }

}