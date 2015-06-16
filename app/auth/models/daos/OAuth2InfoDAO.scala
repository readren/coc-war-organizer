package auth.models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import auth.models.daos.OAuth2InfoDAO._
import scala.concurrent.Future
import play.api.Play.current
import play.api.db.DB
import anorm._
import anorm.SqlParser._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.executionContexts._

/**
 * The DAO to store the OAuth2 information.
 */
class OAuth2InfoDAO extends DelegableAuthInfoDAO[OAuth2Info] {

	/**
	 * Saves the OAuth2 info.
	 *
	 * @param loginInfo The login info for which the auth info should be saved.
	 * @param authInfo The OAuth2 info to save.
	 * @return The saved OAuth2 info.
	 */
	def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {

		find(loginInfo).map {
			case Some(info) =>
				if (info == authInfo) authInfo
				else {
					val sql = SQL"""
update auth_oauth2_info set	access_token = ${authInfo.accessToken}, token_type = ${authInfo.tokenType}, expires_in = ${authInfo.expiresIn}, refresh_token=${authInfo.refreshToken}, params=${paramsToText(authInfo.params)} 
where provider_id = ${loginInfo.providerID} and provider_key = ${loginInfo.providerKey}"""
					val count = DB.withConnection { implicit connection =>
						sql.executeUpdate()
					}
					if (count == 1) authInfo
					else throw new AssertionError(s"oauth2 info update failed: count=$count, loginInfo=$loginInfo");
				}
			case None =>
				val sql = SQL"""
insert into auth_oauth2_info (provider_id, provider_key, access_token, token_type, expires_in, refresh_token, params)
values (${loginInfo.providerID}, ${loginInfo.providerKey}, ${authInfo.accessToken}, ${authInfo.tokenType}, ${authInfo.expiresIn}, ${authInfo.refreshToken}, ${paramsToText(authInfo.params)})"""
				val count = DB.withConnection { implicit connection =>
					sql.executeUpdate()
				}
				if (count == 1) authInfo
				else throw new AssertionError(s"oauth2 info insert failed: count=$count, loginInfo=$loginInfo");
		}(dbWriteOperations)
	}

	/**
	 * Finds the OAuth2 info which is linked with the specified login info.
	 *
	 * @param loginInfo The linked login info.
	 * @return The retrieved OAuth2 info or None if no OAuth2 info could be retrieved for the given login info.
	 */
	def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
		Future {
			val sql = SQL"""
select *
from auth_oauth2_info oi
where oi.provider_id = ${loginInfo.providerID} and oi.provider_key = ${loginInfo.providerKey}"""
			DB.withConnection { implicit connection =>
				sql.as(oauthInfoParser.singleOpt)
			}
		}(simpleDbLookups)
	}
}

/**
 * The companion object.
 */
object OAuth2InfoDAO {

	lazy val oauthInfoParser = {
		str("access_token") ~ //text not null
			str("token_type").? ~ //text
			int("expires_in").? ~ //integer
			str("refresh_token").? ~ //text
			str("params").? // text
	}.map {
		case accessToken ~ oTokenType ~ oExpiresIn ~ oRefreshToken ~ oParamsRaw => {
			OAuth2Info(accessToken, oTokenType, oExpiresIn, oRefreshToken, textToParams(oParamsRaw))
		}
	}

	def paramsToText(oParams: Option[Map[String, String]]): String = oParams match {
		case Some(params) => params.map(x => "(" + x._1 + "," + x._2 + ")").reduce(_ + _)
		case None => null
	}

	import scala.util.parsing.combinator.Parsers
	import scala.util.parsing.input.{ PagedSeqReader, Reader, CharSequenceReader }

	def textToParams(oText: Option[String]): Option[Map[String, String]] =
		for (text <- oText) yield paramsParser.params(new CharSequenceReader(text)).get

	/**
	 * Parser que convierte un texto con la forma "(llave1,valor1)(llave2,valor2)..." a un Map("llave1"->"valor1","llave2"->"valor2")
	 */
	private object paramsParser extends Parsers {
		override type Elem = Char

		private def contenidoAbierto: Parser[String] = acceptIf(c => c != '(' && c != ')')("unexpected " + _).+ ^^ (_.mkString)
		private def contenidoCerrado: Parser[String] = accept('(') ~ contenido.* ~ ')' ^? { case '(' ~ c ~ ')' => "(" + c.reduce(_ + _) + ")" }
		private def contenido: Parser[String] = contenidoAbierto | contenidoCerrado
		private def valor: Parser[String] = contenido.* ^^ (x => x.reduce(_ ++ _).mkString)

		private def llave: Parser[String] = acceptIf(c => c != ',')("unexpected " + _).+ ^^ (_.mkString)
		private def entrada: Parser[(String, String)] = '(' ~> llave ~ ',' ~ valor ~ ')' ^^ { case k ~ ',' ~ v ~ ')' => (k, v) }
		def params: Parser[Map[String, String]] = entrada.* ^^ { _.toMap }
	}

	//	implicit def rowToStringArray: Column[Array[String]] = Column.nonNull { (value, meta) =>
	//		val MetaDataItem(qualified, nullable, clazz) = meta
	//		value match {
	//			case o: java.sql.Array => Right(o.getArray().asInstanceOf[Array[String]])
	//			case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass))
	//		}
	//	}
	//
	//	import scala.language.implicitConversions
	//	implicit object arrayToStatement extends ToStatement[java.sql.Array] {
	//		def set(s: PreparedStatement, i: Int, a: java.sql.Array): Unit = s.setArray(i, a)
	//	}
	//	implicit object arrayToSql extends ToSql[java.sql.Array] {
	//		def fragment(value: java.sql.Array): (String, Int) = ("?", 1)
	//	}
	//	implicit def arrayToParameterValue(array: java.sql.Array): ParameterValue = {
	//		ParameterValue[java.sql.Array](array, arrayToSql, arrayToStatement)
	//	}
	//
	//	def authInfoParamsToArray(oParams: Option[Map[String, String]])(implicit connection: java.sql.Connection) = {
	//
	//		val paramsRaw = for (params <- oParams)
	//			yield for (entry <- params)
	//			yield entry._1 + "," + entry._2
	//		paramsRaw.fold[java.sql.Array](null)(x => connection.createArrayOf("text", x.toArray.asInstanceOf[Array[java.lang.Object]]))
	//	}

}
