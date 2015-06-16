package auth.models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import auth.models.daos.PasswordInfoDAO._

import scala.collection.mutable
import scala.concurrent.Future
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import utils.executionContexts._


/**
 * The DAO to store the password information.
 */
class PasswordInfoDAO extends DelegableAuthInfoDAO[PasswordInfo] {

	/**
	 * Saves the password info.
	 *
	 * @param loginInfo The login info for which the auth info should be saved.
	 * @param authInfo The password info to save.
	 * @return The saved password info.
	 */
	def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
		find(loginInfo).map {
			case Some(oldAuthInfo) =>
				if (oldAuthInfo == authInfo) authInfo
				else {
					
					val sql = SQL"""
update table auth_password_info set
	provider_id=,
	provider_key=,
	hasher=${authInfo.hasher},
	password=${authInfo.hasher}, salt=${authInfo.salt}
where provider_id=${loginInfo.providerID} and provider_key=${loginInfo.providerKey}"""

					val count = DB.withConnection { implicit connection =>
						sql.executeUpdate()
					}
					if (count != 1) throw new AssertionError(s"loginInfo=${loginInfo}, count=$count")
					else authInfo
				}
			case None =>
				
				val sql = SQL"""
insert into auth_password_info (provider_id, provider_key, hasher, password, salt)
values (${loginInfo.providerID}, ${loginInfo.providerKey}, ${authInfo.hasher}, ${authInfo.password}, ${authInfo.salt})"""
	
				val count = DB.withConnection { implicit connection =>
					sql.executeUpdate()
				}
				if (count != 1) throw new AssertionError(s"loginInfo=${loginInfo}, count=${count}")
				else authInfo
		}(dbWriteOperations)

	/**
	 * Finds the password info which is linked with the specified login info.
	 *
	 * @param loginInfo The linked login info.
	 * @return The retrieved password info or None if no password info could be retrieved for the given login info.
	 */
	def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = Future {
		val sql = SQL"""
select hasher, password, salt
from auth_password_info
where provider_id=${loginInfo.providerID} and provider_key=${loginInfo.providerKey}"""
		DB.withConnection { implicit connection =>
			sql.as(authPasswordParser.singleOpt)
		}
	}(simpleDbLookups)
}

/**
 * The companion object.
 */
object PasswordInfoDAO {

	lazy val authPasswordParser = {
		str("hasher") ~ // text not null
			str("password") ~ // text not null
			str("salt").? // text
	}.map { case hasher ~ password ~ salt => PasswordInfo(hasher, password, salt) }
}
