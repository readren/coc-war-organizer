package settings.membership.jdbcDaos

import anorm._
import anorm.SqlParser._
import utils.JdbcTransacMode
import org.postgresql.util.PSQLException
import java.util.UUID
import common.AlreadyExistException
import settings.membership.Organization
import settings.membership.OrganizationDao
import settings.membership.SearchOrganizationsCmd
import settings.membership.CreateOrganizationCmd
import utils.UuidToStatement._
import anorm.ParameterValue.toParameterValue

/**
 * @author Gustavo
 */
class OrganizationDaoImpl extends OrganizationDao {
	
	override def search(cmd: SearchOrganizationsCmd) = JdbcTransacMode.inConnection(implicit connection => {
		val sql = SQL"""
select *
from orga_organization org
where (${cmd.clanName.isEmpty} or org.clan_name = ${cmd.clanName})
		and (${cmd.clanTag.isEmpty} or org.clan_tag = ${cmd.clanTag})
		and (${cmd.description.isEmpty} or org.description = ${cmd.description})
"""
		sql.as(OrganizationDaoImpl.organizationParser.*)
	})

	override def get(organizationId: Organization.Id) = JdbcTransacMode.inConnection(implicit connection => {
		val sql = SQL"select * from orga_organization o where o.id = ${organizationId}"
		sql.as(OrganizationDaoImpl.organizationParser.singleOpt)
	})

	override def insert(cmd: CreateOrganizationCmd) = JdbcTransacMode.inConnection(implicit connection => {
		val id = UUID.randomUUID()
		val sql = SQL"insert into orga_organization (id, clan_name, clan_tag, description) values ($id, ${cmd.clanName}, ${cmd.clanTag}, ${cmd.description})"
		try {
			sql.executeUpdate()
			new Organization(id, cmd.clanName, cmd.clanTag, cmd.description)
		} catch {
			case are: PSQLException if are.getSQLState() == "23505" => // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
				throw new AlreadyExistException("An organization for the same clan and with the same description already exists", are)
		}
	})

}

object OrganizationDaoImpl {
	val organizationParser: RowParser[Organization] = {
		get[Organization.Id]("id") ~ str("clan_name") ~ str("clan_tag") ~ str("description").? map {
			case id ~ clanName ~ clanTag ~ description => Organization(id, clanName, clanTag, description)
		}
	}
	
}
