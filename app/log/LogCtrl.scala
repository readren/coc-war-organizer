package log

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import settings.account.AccountSrv
import utils.TransacMode
import utils.TransacTransitionExec
import utils.Transition
import utils.executionContexts.simpleDbLookups
import settings.account.Account
import com.google.inject.ImplementedBy
import utils.executionContexts._
import common.TypicalActions
import common.Command

/**
 * @author Gustavo
 *
 */

case class GetEventsAfterCmd(eventInstant: Option[OrgaEvent.Instant], actor: Account.Tag) extends Command
object GetEventsAfterCmd {
	implicit val jsonFormat = Json.reads[GetEventsAfterCmd]
}

trait OrgaEvent {
	val id: OrgaEvent.Id
	val instant: OrgaEvent.Instant
	def toJson: JsValue
}
object OrgaEvent {
	type Id = Long
	type Instant = org.joda.time.DateTime
	implicit def jsonWrites: Writes[OrgaEvent] = Writes[OrgaEvent] ( _.toJson )
}

/**An event that affects the state of some client side independent machine conceived by the effect of an event sent previously.
 * Design decision: The way in which the state of an independent machine that was initiated by an event is affected by a subsequent [[RetroEvent]] is determined by the a handler of the affected independent machine. Said handler receives the [[RetroEvent]] to know how to react.  */
trait RetroEvent extends OrgaEvent {
	/**Ids of the events that have been previously sent to the client and have conceived a independent machine (in the client) whose state should be eventually updated after the action announced by this event is committed. */
	val affectedEvents: Seq[OrgaEvent.Id]
}



@ImplementedBy(classOf[LogSrvImpl])
trait LogSrv {
	def getEventsAfter(accountId:Account.Id, getEventsAfterCmd: GetEventsAfterCmd): Transition[TransacMode, Seq[OrgaEvent]]
	def newEvent(): Transition[TransacMode, (OrgaEvent.Id, OrgaEvent.Instant)]
}


class LogCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], val tte: TransacTransitionExec, logSrv: LogSrv)
	extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {

	def getEventsAfter = typicalAction(simpleDbLookups, logSrv.getEventsAfter _)
}