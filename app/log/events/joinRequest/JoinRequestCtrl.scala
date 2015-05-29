/**
 *
 */
package log.events.joinRequest

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import javax.inject.Inject
import log.Event
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
import settings.account.AccountId
import settings.membership.Organization
import settings.membership.Member

/**
 * @author Gustavo
 *
 */

case class JoinRequestEventDto(id: Event.Id, instant: Event.Instant, accountName: String, message: Option[String]) extends Event {
	override def toJson: JsValue = JoinRequestEventDto.jsonFormat.writes(this)
}
object JoinRequestEventDto {
	implicit val jsonFormat = Json.writes[JoinRequestEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinRequest")) }
}

case class JoinCancelEventDto(id:Event.Id, instant: Event.Instant, accountName:String, affectedEvents:Seq[Event.Id]) extends RetroEvent {
	override def toJson: JsValue = JoinCancelEventDto.jsonFormat.writes(this)
}
object JoinCancelEventDto {
	implicit val jsonFormat = Json.writes[JoinCancelEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinCancel")) }
}

case class JoinRespondCmd(responderAccountTag: Account.Tag, requestEventId: Event.Id, rejectionMsg: Option[String])
object JoinRespondCmd {
	implicit val jsonFormat = Json.reads[JoinRespondCmd]
}

case class JoinResponseEventDto(id: Event.Id, instant: Event.Instant, affectedEvents: Seq[Event.Id], responderMemberName:String, requesterAccountName: String, requesterMemberName: Option[String], rejectionMsg:Option[String]) extends RetroEvent {
	override def toJson: JsValue = JoinResponseEventDto.jsonFormat.writes(this)
}
object JoinResponseEventDto {
	implicit val jsonFormat = Json.writes[JoinResponseEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("joinResponse")) }
}

case class JoinRequest(accountId: AccountId, organizationId: Organization.Id, requestEventId: Event.Id)

case class JoinReject(rejectionEventId: Event.Id, requesterAccountId: AccountId, requestEventId:Event.Id, rejectionMsg:String, rejecterMemberTag:Member.Tag)


@ImplementedBy(classOf[JoinRequestSrvImpl])
trait JoinRequestSrv extends EventsSource[Event] {
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