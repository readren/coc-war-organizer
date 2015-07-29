package common

import settings.account.Account
import play.api.libs.json.Json
import war.central.WarEvent


/**
 * @author Gustavo
 */
trait Command {
	val actor: Account.Tag
}

trait CommandThatUpdates extends Command {
  val lastReceivedEventInstant: WarEvent.Instant
}

case class ParameterlessCmd(actor: Account.Tag) extends Command
object ParameterlessCmd {
  implicit val reads = Json.reads[ParameterlessCmd]
}
