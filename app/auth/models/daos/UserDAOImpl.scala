package auth.models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import auth.models.User
import auth.models.daos.UserDAOImpl._

import scala.concurrent.Future
import anorm._
import play.api.Play.current
import play.api.db.DB
import anorm.SqlParser._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.executionContexts._


/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO {

	/**
	 * Finds a user by its login info.
	 *
	 * @param loginInfo The login info of the user to find.
	 * @return The found user or None if no user for the given login info could be found.
	 */
	def find(loginInfo: LoginInfo) = {
		Future {
			val sql = SQL"select * from auth_user u where u.provider_id = ${loginInfo.providerID} and u.provider_key = ${loginInfo.providerKey}"
			DB.withConnection { implicit connection =>
				sql.as(userParser.singleOpt)
			}
		}(simpleDbLookups)
	}

	/**
	 * Finds a user by its user ID.
	 *
	 * @param userID The ID of the user to find.
	 * @return The found user or None if no user for the given ID could be found.
	 */
	def find(userId: UUID) = {
		Future {
			val sql = SQL"select * from auth_user u	where u.user_id = ${userId}";
			DB.withConnection { implicit connection =>
				sql.as(userParser.singleOpt)
			}
		}(simpleDbLookups)
	}

	/**
	 * Saves a user.
	 *
	 * @param user The user to save.
	 * @return The saved user.
	 */
	def update(user: User) = {
		//users += (user.userID -> user)
		Future {
			val sql = SQL"""
update auth_user set
	provider_id = ${user.loginInfo.providerID},
	provider_key = ${user.loginInfo.providerKey},
	first_name = ${user.firstName.orNull: String},
	last_name = ${user.lastName.orNull: String},
	full_name = ${user.fullName.orNull: String},
	email = ${user.email.orNull: String},
	avatar_url = ${user.avatarURL.orNull: String}
where user_id = ${user.id}::uuid""";
			val count = DB.withConnection { implicit connection =>
				sql.executeUpdate()
			}
			if (count == 1) user
			else throw new AssertionError(s"User update failed: count=$count, user=$user");
		}(dbWriteOperations)
	}

	def insert(user: User) = {
		Future {
			val sql = SQL"""
insert into auth_user (
	user_id,
  provider_id,
	provider_key,
	first_name,
	last_name,
	full_name,
	email,
	avatar_url
) values (
	${user.id}::uuid,
  ${user.loginInfo.providerID},
	${user.loginInfo.providerKey},
	${user.firstName.orNull: String},
	${user.lastName.orNull: String},
	${user.fullName.orNull: String},
	${user.email.orNull: String},
	${user.avatarURL.orNull: String}
)""";
			val count = DB.withConnection { implicit connection =>
				sql.executeUpdate()
			}
			if (count == 1) user
			else throw new AssertionError(s"User insert failed: count=$count, user=$user");
		}(dbWriteOperations)
	}
}

/**
 * The companion object.
 */
object UserDAOImpl {

	lazy val userParser = {
		get[UUID]("user_id") ~ //UUID primary key (therefore indexed),
			str("provider_id") ~ // text not null,
			str("provider_key") ~ // text not null unique (therefore indexed),
			str("first_name").? ~ // text,
			str("last_name").? ~ // text,
			str("full_name").? ~ // text,
			str("email").? ~ // text,
			str("avatar_url").? map {
				case userId ~ providerId ~ providerKey ~ firstName ~ lastName ~ fullName ~ email ~ avatarUrl =>
					User(userId, LoginInfo(providerId, providerKey), firstName, lastName, fullName, email, avatarUrl)
			}
	}
}
