package auth.models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import auth.models.daos.OAuth1InfoDAO._

import scala.concurrent.Future
import anorm._
import play.api.Play.current
import play.api.db.DB
import anorm.SqlParser._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The DAO to store the OAuth1 information.
 */
class OAuth1InfoDAO extends DelegableAuthInfoDAO[OAuth1Info] {

	/**
	 * Saves the OAuth1 info.
	 *
	 * @param loginInfo The login info for which the auth info should be saved.
	 * @param authInfo The OAuth1 info to save.
	 * @return The saved OAuth1 info.
	 */
	def save(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
		find(loginInfo).map {
			case Some(info) =>
				if (info == authInfo) authInfo
				else {
					val sql = SQL"""
update auth_oauth1_info set	token = ${authInfo.token}, secret = ${authInfo.secret}
where provider_id = ${loginInfo.providerID} and provider_key = ${loginInfo.providerKey}"""
					val count = DB.withConnection { implicit connection =>
						sql.executeUpdate()
					}
					if (count == 1) authInfo
					else throw new AssertionError(s"oauth1 info update failed: count=$count, loginInfo=$loginInfo");
				}
			case None =>
				val sql = SQL"""
insert into auth_oauth1_info (provider_id, provider_key, token, secret)
values (${loginInfo.providerID}, ${loginInfo.providerKey}, ${authInfo.token}, ${authInfo.secret})"""
				val count = DB.withConnection { implicit connection =>
					sql.executeUpdate()
				}
				if (count == 1) authInfo
				else throw new AssertionError(s"oauth1 info insert failed: count=$count, loginInfo=$loginInfo");
		}
	}

	/**
	 * Finds the OAuth1 info which is linked with the specified login info.
	 *
	 * @param loginInfo The linked login info.
	 * @return The retrieved OAuth1 info or None if no OAuth1 info could be retrieved for the given login info.
	 */
	def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]] = {
		Future {
			val sql = SQL"""
select *
from auth_oauth1_info oi
where oi.provider_id = ${loginInfo.providerID} and oi.provider_key = ${loginInfo.providerKey}
			  """
			DB.withConnection { implicit connection =>
				sql.as(auth1InfoParser.singleOpt)
			}
		}
	}
}

/**
 * The companion object.
 */
object OAuth1InfoDAO {

	lazy val auth1InfoParser = {
		str("token") ~ // text not null
			str("secret") // text not null
	}.map { case token ~ secret => OAuth1Info(token, secret) }
}
