
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
import settings.account.Account
import log.OrgaEvent
import common.TypicalActions
import common.Command

case class IconDto(tag: Icon.Tag, name: String, organizationId: Organization.Id, role: Role)

case class CreateOrganizationCmd(accountTag: Account.Tag, accountName: String, clanName: String, clanTag: String, description: Option[String], iconName: Option[String])

case class SearchOrganizationsCmd(clanName: Option[String], clanTag: Option[String], description: Option[String])

case class SendJoinRequestCmd(accountTag: Account.Tag, organizationId: Organization.Id)

/**
 * Tells the membership status of an account. There are four possibilities:
 * "alone" when all vals are empty (includes canceled requests),
 * "waiting join request response" when only organization is defined,
 * "rejected" when only memberDto is empty,
 * "already joined" when only rejectionMsg is empty.
 */
case class MembershipStatusDto(organization: Option[Organization], iconDto: Option[IconDto], rejectionMsg: Option[String])

case class MemberIconDto(tag: Icon.Tag, iconName: String)
case class GetOrgaStatusCmd(actor: Account.Tag) extends Command
case class GetOrgaStatusDto(members: Seq[MemberIconDto])

object membershipJsonConverters {
	implicit val iconDtoWrites = Json.writes[IconDto]
	implicit val createOrganizationCmdReads = Json.reads[CreateOrganizationCmd]
	implicit val searchOrganizationsCmdReads = Json.reads[SearchOrganizationsCmd]
	implicit val sendJoinRequestCmdReads = Json.reads[SendJoinRequestCmd]
	implicit val membershipStatusDtoWrites = Json.writes[MembershipStatusDto]
	implicit val memberIconDtoWrites = Json.writes[MemberIconDto]
	implicit val getOrgaStatusCmdReads = Json.reads[GetOrgaStatusCmd]
	implicit val getOrgaStatusDtoWrites = Json.writes[GetOrgaStatusDto]
	
}

class MembershipCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], membershipSrv: MembershipSrv, val tte: TransacTransitionExec, accountSrv: AccountSrv)
		extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {
	import membershipJsonConverters._

	def getMembershipStatusOf(accountTag: Int) = SecuredAction.async { implicit request =>
		tte.autoFuture(simpleDbLookups) {
			membershipSrv.getMembershipStatusOf(Account.Id(request.identity.id, accountTag)).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	/**
	 * gives all the accounts of the current user.
	 */
	def searchOrganizations = SecuredAction.async(parse.json) { implicit request =>
		val searchOrganizationsCmd = request.body.as[SearchOrganizationsCmd]
		tte.autoFuture(simpleDbLookups) {
			membershipSrv.searchOrganizations(searchOrganizationsCmd).map { searchResult =>
				Ok(Json.toJson(searchResult))
			}
		}
	}

	def sendJoinRequest = SecuredAction.async(parse.json) { implicit request =>
		val sendJoinRequestCmd = request.body.as[SendJoinRequestCmd]
		tte.autoFuture(dbWriteOperations) {
			membershipSrv.sendJoinRequest(request.identity.id, sendJoinRequestCmd).map { result =>
				Ok(Json.toJson(result))
			}
		}
	}

	def cancelJoinRequest = SecuredAction.async(parse.json) { implicit request =>
		val accountTag = request.body.as[Account.Tag]
		tte.autoFuture(dbWriteOperations) {
			membershipSrv.cancelJoinRequest(Account.Id(request.identity.id, accountTag), false).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def leaveOrganization = SecuredAction.async(parse.json) { implicit request =>
		val accountTag = request.body.as[Account.Tag]
		tte.autoFuture(dbWriteOperations) {
			membershipSrv.leaveOrganization(Account.Id(request.identity.id, accountTag)).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def createOrganization = SecuredAction.async(parse.json) { implicit request =>
		val createOrganizationCmd = request.body.as[CreateOrganizationCmd]
		tte.autoFuture(dbWriteOperations) {
			membershipSrv.createOrganization(request.identity.id, createOrganizationCmd).map { omm =>
				Ok(Json.toJson(MembershipStatusDto(Some(omm._1), Some(omm._2), None)))
			}
		}.recover {
			case are: AlreadyExistException => Conflict(are.getMessage())
		}
	}

	def getOrgaStatus = typicalAction(simpleDbLookups, membershipSrv.getOrgaStatus _)(getOrgaStatusCmdReads, getOrgaStatusDtoWrites)

}

