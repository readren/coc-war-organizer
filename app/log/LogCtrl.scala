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
import utils.TransactionalTransition._
import utils.Transition
import utils.executionContexts.simpleDbLookups
import settings.account.Account
import com.google.inject.ImplementedBy
import utils.executionContexts._
import common.TypicalActions
import common.Command
import settings.membership.IconDto
import common.typeAliases._
import locJsonConverters._


/**
 * @author Gustavo
 *
 */
case class GetLogInitStateCmd(actor: Account.Tag) extends Command
case class GetLogInitStateDto(events:Seq[OrgaEvent], members: Seq[IconDto])

case class GetEventsAfterCmd(eventInstant: OrgaEvent.Instant, actor: Account.Tag) extends Command

object locJsonConverters {
	implicit val getLogInitStateCmdReads = Json.reads[GetLogInitStateCmd]
	implicit val getLogInitStateDtoWrites= Json.writes[GetLogInitStateDto]
	
	implicit val getEventsAfterCmdReads = Json.reads[GetEventsAfterCmd]
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
	def getLogInitState(accountId:Account.Id, getLogInitStateCmd: GetLogInitStateCmd): TiTac[GetLogInitStateDto]
	def getEventsAfter(accountId:Account.Id, getEventsAfterCmd: GetEventsAfterCmd): TiTac[Seq[OrgaEvent]]
	def newEvent(): TiTac[(OrgaEvent.Id, OrgaEvent.Instant)]
}


class LogCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], logSrv: LogSrv, val tte:TransacTransitionExec)
	extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {

	def getLogInitState = typicalAction(simpleDbLookups, logSrv.getLogInitState _)(getLogInitStateCmdReads, getLogInitStateDtoWrites)
	def getEventsAfter = typicalAction(simpleDbLookups, logSrv.getEventsAfter _)
}