package settings.membership.jdbcDaos

import anorm._
import anorm.SqlParser._
import utils.JdbcTransacMode
import settings.membership.IconDao
import settings.membership.Organization
import settings.membership.Icon
import settings.membership.IconDto
import utils.UuidToStatement._
import anorm.ParameterValue.toParameterValue
import settings.membership.Role
import auth.models.User
import settings.account.Account
import utils.Transition
import utils.TransacMode
import settings.account.AccountDaoImpl
import settings.account.Account
import log.OrgaEvent
import settings.membership.RoleChangeEventDto
import log.LogDao
import javax.inject.Inject
import utils.TransacTransitionExec

/**
 * @author Gustavo
 */
class IconDaoImpl extends IconDao {

  override def insert(organizationId: Organization.Id, name: String, role: Role, accountId: Account.Id): Transition[TransacMode, Icon.Tag] = JdbcTransacMode.inConnection(implicit connection => {
    val sql = SQL"""
insert into orga_icon (organization_id, tag, name, present_role, holder_user_id, holder_account_tag)
values ($organizationId, default, $name, ${role.code}::role_code, ${accountId.userId}, ${accountId.tag})"""
    sql.executeInsert(get[Icon.Tag](2).single)
  })

  override def findByAccount(accountId: Account.Id): Transition[TransacMode, Option[Icon]] = JdbcTransacMode.inConnection { implicit connection =>
    val sql = SQL"""
select icon.*
from orga_membership ms
inner join orga_icon icon on (icon.organization_id = ms.organization_id AND icon.tag = ms.icon_tag)
where ms.user_id = ${accountId.userId} AND ms.account_tag = ${accountId.tag} AND ms.abandon_event_id is null"""
    val parser: RowParser[Icon] = {
      get[Organization.Id]("organization_id") ~ IconDaoImpl.iconDtoParser ~ AccountDaoImpl.pkParser("holder_user_id", "holder_account_tag") map {
        case organizationId ~ iconDto ~ holder =>
          assert(holder == accountId)
          Icon(organizationId, iconDto.tag, iconDto.name, iconDto.role, accountId)
      }
    }
    sql.as(parser.singleOpt)
  }

  override def findByHolder(organizationId: Organization.Id, holderId: Account.Id): Transition[TransacMode, Option[IconDto]] = JdbcTransacMode.inConnection(implicit connection => {
    val sql = SQL"""
select *
from orga_icon m
where m.organization_id = $organizationId AND m.holder_user_id = ${holderId.userId} AND m.holder_account_tag = ${holderId.tag}"""
    sql.as(IconDaoImpl.iconDtoParser.singleOpt)
  })

  override def insertRoleChangeEvent(roleChangeEventId: OrgaEvent.Id, organizationId: Organization.Id, affectedIconTag: Icon.Tag, newRole: Role, previousRole: Role, changerIconTag: Icon.Tag): Transition[TransacMode, Unit] =
    JdbcTransacMode.inTransaction(implicit connection => {
      val sql = SQL"""
update orga_icon set role_change_event_id = ${roleChangeEventId}, present_role = ${newRole.code}::role_code, previous_role = ${previousRole.code}::role_code, changer_icon_tag = $changerIconTag
where organization_id = $organizationId AND tag = $affectedIconTag"""
      sql.executeUpdate.ensuring(_ == 1)
      // For efficiency sake, an indexed key of the orga_icon register is put in the orga_event register. This will speed up the search of an embedded role change event given its role_change_event_id.
      SQL"update orga_event set med_fk1 = $organizationId, med_fk2 = $affectedIconTag where id = $roleChangeEventId".executeUpdate.ensuring(_ == 1)
    })

  def getRoleChangeEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[RoleChangeEventDto]] =
    JdbcTransacMode.inConnection(implicit connection => {
      val sql = """
select e.id, e.instant, i.tag, i.present_role, i.previous_role, i.changer_icon_tag
from orga_event e
inner join orga_icon i on (i.organization_id = e.med_fk1 AND i.tag = e.med_fk2 AND i.role_change_event_id = e.id)
where i.organization_id = {organizationId} AND i.changer_icon_tag != i.tag AND """

      val query = threshold match {
        case Left(edge) =>
          SQL(sql + "e.instant >= {edge}").on("edge" -> edge)
        case Right(secondsBefore) =>
          SQL(sql + "e.instant >= localtimestamp - make_interval(secs:={secondsBefore})").on("secondsBefore" -> secondsBefore)
      }
      query.on("organizationId" -> organizationId).as(IconDaoImpl.roleChangeEventDtoParser.*)
    })

}

object IconDaoImpl {
  val iconDtoParser: RowParser[IconDto] = {
    get[Icon.Tag]("tag") ~ str("name") ~ get[Char]("present_role") map {
      case tag ~ name ~ roleCode => IconDto(tag, name, Role.decode(roleCode))
    }
  }

  val roleChangeEventDtoParser: RowParser[RoleChangeEventDto] =
    get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) ~ get[Icon.Tag](3) ~ get[Role.Code](4) ~ get[Role.Code](5) ~ get[Icon.Tag](6) map {
      case id ~ instant ~ affectedIconTag ~ newRole ~ previousRole ~ changerIconTag =>
        RoleChangeEventDto(id, instant, affectedIconTag, Role.decode(newRole), Role.decode(previousRole), changerIconTag)
    }
}
