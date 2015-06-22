/**
 *
 */
package log.events.joinRequest

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import javax.inject.Inject
import log.OrgaEvent
import play.api.libs.json.Json
import settings.account.Account
import settings.account.AccountSrv
import utils.TransacMode
import utils.TransacTransitionExec
import utils.Transition
import utils.executionContexts._
import log.EventsSource
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import com.google.inject.ImplementedBy
import log.RetroEvent
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import settings.account.Account
import settings.membership.Organization
import settings.membership.Icon

/**
 * @author Gustavo
 *
 */

case class JoinRequestEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, accountName: String, message: Option[String]) extends OrgaEvent {
	override def toJson: JsValue = JoinRequestEventDto.jsonFormat.writes(this)
}
object JoinRequestEventDto {
	implicit val jsonFormat = Json.writes[JoinRequestEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinRequest")) }
}

case class JoinCancelEventDto(id:OrgaEvent.Id, instant: OrgaEvent.Instant, accountName:String, affectedEvents:Seq[OrgaEvent.Id]) extends RetroEvent {
	override def toJson: JsValue = JoinCancelEventDto.jsonFormat.writes(this)
}
object JoinCancelEventDto {
	implicit val jsonFormat = Json.writes[JoinCancelEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinCancel")) }
}

case class JoinRespondCmd(responderAccountTag: Account.Tag, requestEventId: OrgaEvent.Id, rejectionMsg: Option[String])
object JoinRespondCmd {
	implicit val jsonFormat = Json.reads[JoinRespondCmd]
}

/**@param affectedEvents are the ids of events that have been sent to the client previously and have conceived a independent machine (in the client) whose state should be updated when the action announced by this event is commited. */
case class JoinResponseEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, affectedEvents: Seq[OrgaEvent.Id], responderMemberName:String, requesterAccountName: String, requesterMemberName: Option[String], rejectionMsg:Option[String]) extends RetroEvent {
	override def toJson: JsValue = JoinResponseEventDto.jsonFormat.writes(this)
}
object JoinResponseEventDto {
	implicit val jsonFormat = Json.writes[JoinResponseEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinResponse")) }
}

case class JoinRequest(accountId: Account.Id, organizationId: Organization.Id, requestEventId: OrgaEvent.Id)

case class JoinReject(rejectionEventId: OrgaEvent.Id, requesterAccountId: Account.Id, requestEventId:OrgaEvent.Id, rejectionMsg:String, rejecterMemberTag:Icon.Tag)


@ImplementedBy(classOf[JoinRequestSrvImpl])
trait JoinRequestSrv extends EventsSource[OrgaEvent] {
	def accept(userId: User.Id, acceptCmd: JoinRespondCmd): Transition[TransacMode, Try[JoinResponseEventDto]]
	def reject(userId: User.Id, rejectCmd: JoinRespondCmd): Transition[TransacMode, Try[JoinResponseEventDto]]
}

class JoinRequestCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], transacTransitionExec: TransacTransitionExec, accountSrv: AccountSrv, joinRequestSrv: JoinRequestSrv)
	extends Silhouette[User, JWTAuthenticator] {

	def accept = SecuredAction.async(parse.json) { implicit request =>
		val acceptCmd = request.body.as[JoinRespondCmd]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			joinRequestSrv.accept(request.identity.id, acceptCmd).map { x => Ok(x.get.toJson) }
		}
	}

	def reject = SecuredAction.async(parse.json) { implicit request =>
		val rejectCmd = request.body.as[JoinRespondCmd]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			joinRequestSrv.reject(request.identity.id, rejectCmd).map { x => Ok(x.get.toJson) }
		}
	}
}