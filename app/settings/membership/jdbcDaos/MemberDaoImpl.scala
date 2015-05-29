package settings.membership.jdbcDaos

import anorm._
import anorm.SqlParser._
import utils.JdbcTransacMode
import settings.membership.MemberDao
import settings.membership.Organization
import settings.membership.Member
import settings.membership.MemberDto
import utils.UuidToStatement._
import anorm.ParameterValue.toParameterValue
import settings.membership.Role
import auth.models.User
import settings.account.Account
import utils.Transition
import utils.TransacMode
import settings.account.AccountDaoImpl
import settings.account.AccountId

/**
 * @author Gustavo
 */
class MemberDaoImpl extends MemberDao {

	override def insert(organizationId: Organization.Id, name: String, role: Role, accountId: Option[AccountId]) = JdbcTransacMode.inConnection(implicit connection => {
		val sql = SQL"""
insert into orga_member (organization_id, tag, name, role, holder_user_id, holder_account_tag)
values ($organizationId, default, $name, ${role.code}::role_code, ${accountId.map(_.userId)}, ${accountId.map(_.tag)})"""
		val tag = sql.executeInsert(get[Member.Tag](2).single)
		Member(organizationId, tag, name, role, accountId)
	})

	override def findByHolder(holderPk: AccountId): Transition[TransacMode, Option[Member]] = JdbcTransacMode.inConnection(implicit connection => {
		val sql = SQL"select * from orga_member m where m.holder_user_id = ${holderPk.userId} and m.holder_account_tag = ${holderPk.tag}"
		sql.as(MemberDaoImpl.memberParser.singleOpt)
	})

  def update(updatedMember: Member): Transition[TransacMode, Member] = JdbcTransacMode.inConnection(implicit connection => {
	 	val sql = SQL"""
update orga_member set name = ${updatedMember.name}, role = ${updatedMember.role.code}::role_code
where organization_id = ${updatedMember.organizationId} and tag = ${updatedMember.tag}"""
	 	sql.executeUpdate
	 	updatedMember
	})
}

object MemberDaoImpl {
	val memberDtoParser: RowParser[MemberDto] = {
		get[Member.Tag]("tag") ~ str("name") ~ get[Organization.Id]("organization_id") ~ get[Char]("role") map {
			case tag ~ name ~ organizationId ~ roleCode => MemberDto(tag, name, organizationId, Role.decode(roleCode))
		}
	}

	val memberParser: RowParser[Member] = {
		get[Organization.Id]("organization_id") ~ memberDtoParser ~ AccountDaoImpl.pkParser("holder_user_id", "holder_account_tag").? map {
			case organizationId ~ memberDto ~ holderPk => Member(organizationId, memberDto.tag, memberDto.name, memberDto.role, holderPk)
		}
	}
}
