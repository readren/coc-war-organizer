package utils

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext

/**
 * @author Gustavo
 */
object futuresFlowCtrl {
	/**Gives a future that completes with the result of the evaluator if the validator succeed. If the validator fails, the given future will complete with the failure of the first who has failed. */
  def validateConcurrently[E](validator:Future[Unit], evaluator:Future[E] )(implicit ec:ExecutionContext):Future[E] = {
		val promise = Promise[E]
		validator.onFailure {
			case e => promise.failure(e)
		}
		evaluator.onComplete {
			case Success(e) => validator.onSuccess {case _ => promise.success(e) }
			case Failure(f) => promise.failure(f)
		}
		
		promise.future
	}     
}