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

/**
 * @author Gustavo
 *
 */

case class GetEventsAfterCmd(eventId: Option[Event.Id], accountTag: Account.Tag)
object GetEventsAfterCmd {
	implicit val jsonFormat = Json.reads[GetEventsAfterCmd]

}

trait Event {
	val id: Event.Id
	val instant: Event.Instant
	def toJson: JsValue
}
object Event {
	type Id = Long
	type Instant = org.joda.time.DateTime
	implicit def jsonWrites: Writes[Event] = Writes[Event] ( _.toJson )
}

/**An event that affects the state of something initiated by a previous events.
 * Design decision: The way in which the state, of the thing that was initiated by previous event, is affected by a subsequent [[RetroEvent]] is determined by the affected event handler, and the effect depends on the [[RetroEvent]]  */
trait RetroEvent extends Event {
	/**Ids of the events that initiated the things whose states are affected by this event */
	val affectedEvents: Seq[Event.Id]
}

trait EventPrj {}

@ImplementedBy(classOf[LogSrvImpl])
trait LogSrv {
	def getEventsAfter(userId:User.Id, getEventsAfterCmd: GetEventsAfterCmd): Transition[TransacMode, Seq[Event]]
	def newEvent(): Transition[TransacMode, (Event.Id, Event.Instant)]
//	def log(eventPrj: EventPrj): Transition[TransacMode, Event]
}


class LogCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], transacTransitionExec: TransacTransitionExec, accountSrv: AccountSrv, logSrv: LogSrv)
	extends Silhouette[User, JWTAuthenticator] {

	def getEventsAfter = SecuredAction.async(parse.json) { implicit request =>
		val getEventsAfterCmd = request.body.as[GetEventsAfterCmd]
			transacTransitionExec.autoFuture(simpleDbLookups) {
				logSrv.getEventsAfter(request.identity.id, getEventsAfterCmd).map { response =>
					Ok(Json.toJson(response))
				}		
		}
	}
}