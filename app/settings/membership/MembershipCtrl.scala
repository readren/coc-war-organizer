
package settings.membership

import javax.inject.Inject
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import utils.TransacTransitionExec
import scala.concurrent.Future
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import utils.Transition
import utils.TransacMode
import utils.executionContexts._
import play.api.libs.json.JsError
import settings.account.AccountSrv
import settings.account.Account
import common.AlreadyExistException
import settings.account.AccountId
import log.Event


case class MemberDto(tag: Member.Tag, name: String, organizationId: Organization.Id, role: Role)
object MemberDto {
	implicit val jsonFormat = Json.writes[MemberDto]
}

case class CreateOrganizationCmd(accountTag: Account.Tag, accountName: String, clanName: String, clanTag: String, description: Option[String], memberName: Option[String])
object CreateOrganizationCmd {
	implicit val jsonFormat = Json.reads[CreateOrganizationCmd]
}

case class SearchOrganizationsCmd(clanName: Option[String], clanTag: Option[String], description: Option[String])
object SearchOrganizationsCmd {
	implicit val jsonFormat = Json.reads[SearchOrganizationsCmd]
}

case class SendJoinRequestCmd(accountTag: Account.Tag, organizationId: Organization.Id)
object SendJoinRequestCmd {
	implicit val jsonFormat = Json.reads[SendJoinRequestCmd]
}

/**Tells the membership status of an account. There are four possibilities:
 * "alone" when all vals are empty (includes canceled requests),
 * "waiting join request response" when only organization is defined,
 * "rejected" when only memberDto is empty,
 * "already joined" when only rejectionMsg is empty.*/
case class MembershipStatusDto(organization: Option[Organization], memberDto: Option[MemberDto], rejectionMsg: Option[String])
object MembershipStatusDto {
	implicit val jsonFormat = Json.writes[MembershipStatusDto]
}



class MembershipCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], membershipSrv: MembershipSrv, transacTransitionExec: TransacTransitionExec, accountSrv: AccountSrv)
	extends Silhouette[User, JWTAuthenticator] {

	def getMembershipStatusOf(accountTag: Int) = SecuredAction.async { implicit request =>
		transacTransitionExec.autoFuture(simpleDbLookups) {
			membershipSrv.getMembershipStatusOf(AccountId(request.identity.id, accountTag)).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	/**
	 * gives all the accounts of the current user.
	 */
	def searchOrganizations = SecuredAction.async(parse.json) { implicit request =>
		val searchOrganizationsCmd = request.body.as[SearchOrganizationsCmd]
		transacTransitionExec.autoFuture(simpleDbLookups) {
			membershipSrv.searchOrganizations(searchOrganizationsCmd).map { searchResult =>
				Ok(Json.toJson(searchResult))
			}
		}
	}

	def sendJoinRequest = SecuredAction.async(parse.json) { implicit request =>
		val sendJoinRequestCmd = request.body.as[SendJoinRequestCmd]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.sendJoinRequest(request.identity.id, sendJoinRequestCmd).map { result =>
				Ok(Json.toJson(result))
			}
		}
	}

	def cancelJoinRequest = SecuredAction.async(parse.json) { implicit request =>
		val accountTag = request.body.as[Account.Tag]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.cancelJoinRequest(AccountId(request.identity.id, accountTag)).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def leaveOrganization = SecuredAction.async(parse.json) { implicit request =>
		val accountTag = request.body.as[Account.Tag]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.leaveOrganization(AccountId(request.identity.id, accountTag)).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def createOrganization = SecuredAction.async(parse.json) { implicit request =>
		val createOrganizationCmd = request.body.as[CreateOrganizationCmd]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.createOrganization(request.identity.id, createOrganizationCmd).map { omm =>
				Ok(Json.toJson(MembershipStatusDto(Some(omm._1), Some(MemberDto(omm._2.tag, omm._2.name, omm._1.id, Leader)), None)))
			}
		}.recover {
			case are: AlreadyExistException => Conflict(are.getMessage())
		}

	}
}

