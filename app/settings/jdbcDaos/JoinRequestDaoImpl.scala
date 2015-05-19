/**
 *
 */
package settings.jdbcDaos

import settings.JoinRequestDao
import auth.models.User
import settings.Account
import utils.Transition
import utils.TransacMode
import settings.JoinRequestStatus
import settings.JoinRequest
import settings.SendJoinRequestCmd
import utils.JdbcTransacMode
import anorm._
import settings.Organization
import utils.UuidToStatement._
/**
 * @author Gustavo
 *
 */
class JoinRequestDaoImpl extends JoinRequestDao {
	def insert(userId: User.Id, sendJoinRequestCmd: SendJoinRequestCmd) = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"insert into orga_join_request (user_id, account_tag, organization_id, rejection_msg) values ($userId, ${sendJoinRequestCmd.accountTag}, ${sendJoinRequestCmd.organizationId}, null)"
		sql.executeUpdate()
		JoinRequest(userId, sendJoinRequestCmd.accountTag, sendJoinRequestCmd.organizationId, None)
	}

	def delete(userId: User.Id, accountTag: Account.Tag) = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"delete from orga_join_request where user_id = $userId and account_tag = $accountTag"
		sql.executeUpdate()
	}

	def getJoinRequestStatusOf(userId: User.Id, accountTag: Account.Tag) = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"""
select *
from orga_join_request jr
inner join orga_organization o on(o.id = jr.organization_id)
where jr.user_id = $userId and jr.account_tag = $accountTag"""
		sql.as(JoinRequestDaoImpl.joinRequestStatusParser.singleOpt)
	}
}

object JoinRequestDaoImpl {
	import anorm.SqlParser._

	val joinRequestStatusParser: RowParser[JoinRequestStatus] = {
		OrganizationDaoImpl.organizationParser ~ str("reject_Msg").? map {
			case organization ~ rejectMsg => JoinRequestStatus(organization, rejectMsg)
		}
	}
}