package settings.account

import auth.models.User
import scala.concurrent.Future
import play.api.libs.json.Json
import javax.inject.Inject
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import org.postgresql.util.PSQLException
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import java.util.UUID
import common.AlreadyExistException
import utils.Transition
import utils.TransacMode
import utils.JdbcTransacMode

/**
 * @author Gustavo
 */

case class AccountId(userId: User.Id, tag: Account.Tag)

case class Account(userId: UUID, tag: Account.Tag, name: String, description: Option[String])
object Account {
	type Tag = Int
	implicit val jsonFormat = Json.format[Account]
}

case class AccountPrj(name: String, description: Option[String])
object AccountPrj {
	implicit val jsonFormat = Json.format[AccountPrj]
}

trait AccountSrv {
	def getAll(userId: User.Id): Future[Seq[Account]]
	def create(userId: User.Id, project: AccountPrj): Future[Account]
}

class AccountSrvImpl @Inject() (val accountDao: AccountDao)
	extends AccountSrv {

	override def getAll(userId: User.Id) = accountDao.getAll(userId);
	override def create(userId: User.Id, account: AccountPrj) = accountDao.insert(userId, account);
}

trait AccountDao {
	def getAll(userId: User.Id): Future[Seq[Account]]
	def insert(userId: User.Id, account: AccountPrj): Future[Account]
	def findById(accountId: AccountId): Transition[TransacMode, Option[Account]]
}

class AccountDaoImpl extends AccountDao {

	import play.api.Play.current
	import play.api.db.DB
	import anorm._
	import anorm._
	import anorm.SqlParser._
	import utils.executionContexts._
	import utils.UuidToStatement._

	override def getAll(userId: User.Id) = Future {
		val sql = SQL"select * from orga_account account where account.user_id = ${userId}"
		DB.withConnection { implicit connection =>
			sql.as(AccountDaoImpl.accountParser.*)
		}
	}(simpleDbLookups)

	override def insert(userId: User.Id, accountPrj: AccountPrj) = Future {
		val sql = SQL"insert into orga_account (user_id, tag, name, description) values (${userId}, default, ${accountPrj.name}, ${accountPrj.description})"
		val tag = DB.withConnection { implicit connection =>
			sql.executeInsert(get[Account.Tag](2).single)
		}
		Account(userId, tag, accountPrj.name, accountPrj.description)
	}(dbWriteOperations) recoverWith {
		case are: PSQLException if are.getSQLState() == "23505" // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
		=> Future.failed(new AlreadyExistException("An account with that name already exists", are))
	}
	
	override def findById(accountId:AccountId): Transition[TransacMode, Option[Account]] = JdbcTransacMode.inConnection { 
		SQL"select * from orga_account where user_id = ${accountId.userId} and tag = ${accountId.tag}".as(AccountDaoImpl.accountParser.singleOpt)(_)
	}
}

object AccountDaoImpl {
	import anorm.SqlParser._
	import anorm._

	def pkParser(userIdFieldName: String, accountTagFieldName: String): RowParser[AccountId] = {
		get[UUID](userIdFieldName) ~ get[Account.Tag](accountTagFieldName) map {
			case userId ~ accountTag => AccountId(userId, accountTag)
		}
	}

	val accountParser: RowParser[Account] = {
		pkParser("user_id", "tag") ~ str("name") ~ str("description").? map {
			case accountId ~ name ~ description => Account(accountId.userId, accountId.tag, name, description)
		}
	}
}

class AccountCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], accountSrv: AccountSrv)
	extends Silhouette[User, JWTAuthenticator] {

	import scala.concurrent.ExecutionContext.Implicits.global
	import play.api.libs.json.JsString

	/**
	 * gives all the accounts of the current user.
	 */
	def getAll = SecuredAction.async { implicit request =>
		accountSrv.getAll(request.identity.id).map(x => Ok(Json.toJson(x)))
	}

	def create = SecuredAction.async(parse.json) { implicit request =>
		val accountPrj = request.body.as[AccountPrj]
		accountSrv.create(request.identity.id, accountPrj).map(x => Ok(Json.toJson(x)))
			.recover {
				case are: AlreadyExistException => Conflict(are.getMessage())
			}

	}

}
