package common

import com.mohiva.play.silhouette.api.Authenticator
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.Action
import auth.models.User
import common.typeAliases.TiTac
import settings.account.Account
import utils.TransacTransitionExec
import utils.executionContexts.dbWriteOperations
import utils.executionContexts.default
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.Failure
import scala.util.Success

/**
 * @author Gustavo
 */
trait TypicalActions[A <: Authenticator] {
	self: Silhouette[User, A] =>

	val tte: TransacTransitionExec

	protected def typicalAction[Cmd <: Command, X](
		ec: ExecutionContext,
		op: (Account.Id, Cmd) => TiTac[X]) //
		(implicit cmdReads: Reads[Cmd], eventWrites: Writes[X]) //
		: Action[JsValue] =
		SecuredAction.async(parse.json) { request =>
			val cmd = request.body.as[Cmd]
			val eventTiTac = op(Account.Id(request.identity.id, cmd.actor), cmd)
			for {
				x <- tte.autoFuture(ec)(eventTiTac)
			} yield Ok(Json.toJson(x))
		}

	protected def tryAction[Cmd <: Command, X](
		ec: ExecutionContext,
		op: (Account.Id, Cmd) => TiTac[Try[X]]) //
		(implicit cmdReads: Reads[Cmd], eventWrites: Writes[X]) //
		: Action[JsValue] = {
		SecuredAction.async(parse.json) { request =>
			val cmd = request.body.as[Cmd]
			val eventTiTac = op(Account.Id(request.identity.id, cmd.actor), cmd)
			for {
				tx <- tte.autoFuture(ec)(eventTiTac)
			} yield tx match {
        case Success(x) => Ok(Json.toJson(x))
        case Failure(e) => e match {
          case WithHttpState(statusCode) => new Status(statusCode)(e.getMessage)
          case _ => InternalServerError(e.getMessage)
        }
      }
		}
	}
  
}