/**
 *
 */
package settings.membership.jdbcDaos

import anorm._
import anorm.SqlParser._
import auth.models.User
import settings.account.Account
import settings.membership.MemberDto
import settings.membership.MembershipDao
import settings.membership.Organization
import utils.JdbcTransacMode
import utils.UuidToStatement._
import settings.membership.Membership
import settings.membership.Member

/**
 * @author Gustavo
 *
 */
class MembershipDaoImpl extends MembershipDao {
	def getMemberAndOrganizationOf(userId: User.Id, accountTag: Account.Tag) = JdbcTransacMode.inConnection {implicit connection =>
		val sql = SQL"""
select *
from orga_membership ms
inner join orga_member member on (member.organization_id = ms.organization_id and member.tag = ms.member_tag)
inner join orga_organization orga on (orga.id = ms.organization_id)
where ms.user_id = $userId and ms.account_tag = $accountTag"""
		sql.as(MembershipDaoImpl.memberDtoAndOrganizationParser.singleOpt)
	}
	
	def insert(organizationId: Organization.Id, memberTag: Member.Tag, userId: User.Id, accountTag: Account.Tag) = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"insert into orga_membership (organization_id, member_tag, user_id, account_tag) values ($organizationId, $memberTag, $userId, $accountTag)"
		sql.executeUpdate()
		Membership(organizationId, memberTag, userId, accountTag)
	}

  def delete(userId: User.Id, accountTag: Account.Tag) = JdbcTransacMode.inConnection { implicit connection =>
  	val sql = SQL"delete from orga_membership where user_id = $userId and account_tag = $accountTag"
  	sql.executeUpdate()
	}
	
	
}

object MembershipDaoImpl {
	val memberDtoAndOrganizationParser: RowParser[(MemberDto, Organization)] = {
		MemberDaoImpl.memberDtoParser ~ OrganizationDaoImpl.organizationParser map {
			case memberDto ~ organization => (memberDto, organization)
		}
	}
}