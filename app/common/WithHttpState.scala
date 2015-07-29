package common

/**
 * @author Gustavo
 */
trait WithHttpState {
	val stateCode: Int
}

object WithHttpState {
	def unapply(e: WithHttpState): Option[Int] = Some(e.stateCode)
}