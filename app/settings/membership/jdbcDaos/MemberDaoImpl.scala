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

/**
 * @author Gustavo
 */
class MemberDaoImpl extends MemberDao {

	override def insert(organizationId: Organization.Id, name: String) = JdbcTransacMode.inConnection(implicit connection => {
		val sql = SQL"insert into orga_member (organization_id, tag, name) values ($organizationId, default, $name)"
		val tag = sql.executeInsert(get[Member.Tag](2).single)
		Member(organizationId, tag, name)
	})
}

object MemberDaoImpl {
	val memberDtoParser: RowParser[MemberDto] = {
		get[Member.Tag]("tag") ~ str("name") map {	
			case tag ~ name => MemberDto(tag, name)
		}
	}
	
	val memberParser: RowParser[Member] = {
		get[Organization.Id]("organization_id") ~ memberDtoParser map {
			case organizationId ~ memberDto => Member(organizationId, memberDto.tag, memberDto.name)
		}
	}
}
