package common

import auth.models.User
import scala.concurrent.Future
import play.api.libs.json.Json
import javax.inject.Inject
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import org.postgresql.util.PSQLException

/**
 * @author Gustavo
 */

case class Account(name: String, description: Option[String])

object Account {
	implicit val jsonFormat = Json.format[Account]
}

trait AccountService {
	def getAll(user: User): Future[Seq[Account]]
	def add(user: User, account: Account): Future[Account]
}

class AccountServiceImpl @Inject() (val accountDao: AccountDao)
	extends AccountService {

	override def getAll(user: User) = accountDao.getAll(user);
	override def add(user: User, account: Account) = accountDao.add(user, account);
}

trait AccountDao {
	def getAll(user: User): Future[Seq[Account]]
	def add(user: User, account: Account): Future[Account]
}

class AccountDaoImpl extends AccountDao {

	import scala.concurrent.ExecutionContext.Implicits.global
	import play.api.Play.current
	import play.api.db.DB
	import anorm._

	override def getAll(user: User) = Future {
		val sql = SQL"select * from orga_account account where account.user_id = ${user.userID}::UUID"
		DB.withConnection { implicit connection =>
			sql.as(AccountDaoImpl.accountParser.*)
		}
	}

	override def add(user: User, account: Account) = Future {
		val sql = SQL"insert into orga_account (user_id, name, description) values (${user.userID}::UUID, ${account.name}, ${account.description})"
		DB.withConnection { implicit connection =>
			sql.executeUpdate()
		}
		account
	} recoverWith {
		case are: PSQLException if are.getSQLState() == "23505" // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
				=> Future.failed(new AlreadyExistException("An account with that name already exists", are))
	}

}

object AccountDaoImpl {
	import anorm.SqlParser._
	import anorm._

	lazy val accountParser: RowParser[Account] = {
		str("name") ~ str("description").? map {
			case name ~ description => Account(name, description)
		}
	}
}

class AccountCtrl @Inject() (implicit val env: Environment[User, JWTAuthenticator], accountService: AccountService)
	extends Silhouette[User, JWTAuthenticator] {

	import scala.concurrent.ExecutionContext.Implicits.global
	import play.api.libs.json.JsString

	/**
	 * gives all the accounts of the current user.
	 */
	def accounts = SecuredAction.async { implicit request =>
		accountService.getAll(request.identity).map(x => Ok(Json.toJson(x)))
		.recover {
			case e:Exception => InternalServerError(e.getMessage());
		}
	}

	def addAccount = SecuredAction.async(parse.json) { implicit request =>
		val account = request.body.as[Account]
		accountService.add(request.identity, account).map(x => Ok(Json.toJson(x)))
		.recover {
			case are:AlreadyExistException => Conflict(are.getMessage())
			case e:Exception => InternalServerError(e.getMessage())
		}
	}

}
