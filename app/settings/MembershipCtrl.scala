
package settings

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

class MembershipCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], membershipSrv: MembershipSrv, transacTransitionExec: TransacTransitionExec, accountService: AccountService)
	extends Silhouette[User, JWTAuthenticator] {

	def getMembershipStatusOf(accountTag: String) = SecuredAction.async { implicit request =>
		transacTransitionExec.autoFuture(simpleDbLookups) {
			membershipSrv.getMembershipStatusOf(request.identity.id, accountTag.toInt).map { response =>
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
			membershipSrv.cancelJoinRequest(request.identity.id, accountTag).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def leaveOrganization = SecuredAction.async(parse.json) { implicit request =>
		val accountTag = request.body.as[Account.Tag]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.leaveOrganization(request.identity.id, accountTag).map { response =>
				Ok(Json.toJson(response))
			}
		}
	}

	def createOrganization = SecuredAction.async(parse.json) { implicit request =>
		val createOrganizationCmd = request.body.as[CreateOrganizationCmd]
		transacTransitionExec.autoFuture(dbWriteOperations) {
			membershipSrv.createOrganization(request.identity.id, createOrganizationCmd).map { organization =>
				Ok(Json.toJson(organization))
			}
		}
	}
}

