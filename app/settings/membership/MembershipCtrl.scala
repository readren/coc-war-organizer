
package settings.membership

import scala.annotation.implicitNotFound

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

import auth.models.User
import common.AlreadyExistException
import common.Command
import common.TypicalActions
import javax.inject.Inject
import log.OrgaEvent
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import settings.account.Account
import settings.account.AccountSrv
import utils.TransacTransitionExec
import utils.executionContexts.dbWriteOperations
import utils.executionContexts.default
import utils.executionContexts.simpleDbLookups
import membershipJsonConverters._

case class IconDto(tag: Icon.Tag, name: String, role: Role)
object IconDto {
	implicit val iconDtoWrites = Json.writes[IconDto]
}

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

case class AbandonEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, iconDto: IconDto) extends OrgaEvent {
	def toJson: JsValue = abandonEventDtoWrites.writes(this)
}

case class RoleChangeEventDto(id: OrgaEvent.Id, instant: OrgaEvent.Instant, affectedIconTag: Icon.Tag, newRole: Role, previousRole: Role, changerIconTag: Icon.Tag) extends OrgaEvent {
	def toJson: JsValue = roleChangeEventDtoWrites.writes(this)
}


object membershipJsonConverters {
	
	implicit val createOrganizationCmdReads = Json.reads[CreateOrganizationCmd]
	implicit val searchOrganizationsCmdReads = Json.reads[SearchOrganizationsCmd]
	implicit val sendJoinRequestCmdReads = Json.reads[SendJoinRequestCmd]
	implicit val membershipStatusDtoWrites = Json.writes[MembershipStatusDto]

	implicit val abandonEventDtoWrites = Json.writes[AbandonEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("abandon")) }
	implicit val roleChangeEventDtoWrites = Json.writes[RoleChangeEventDto].transform { x => x.as[JsObject] + ("type" -> JsString("roleChange")) }
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

}

