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
	def autoFuture[X](ec:ExecutionContext)(block: Transition[TransacMode, X]): Future[X] = Future(auto(block))(ec)

	/**Wraps the received block in a transaction */
	def withTransaction[X](block: Transition[TransacMode, X]): X = {
		
		val inTransacMode = initialTransacMode.begin
		try {
			val TransitionResult(s, x) = block.run(inTransacMode)
			s.asInstanceOf[InTransacMode].commit
			x
		} catch {
			case NonFatal(e) =>
				inTransacMode.rollback
				throw e
		}
	}

	/**Wraps the received block in a transaction if */
	def inTransaction[X](block: Transition[TransacMode, X]): Transition[TransacMode, X] = Transition[TransacMode, X] {
		case in: InTransacMode => block.run(in)
		case out: OutTransacMode =>
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
}