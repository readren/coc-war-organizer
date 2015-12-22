/**
 *
 */
package settings.membership.jdbcDaos

import anorm._
import anorm.SqlParser._
import auth.models.User
import settings.account.Account
import settings.membership.IconDto
import settings.membership.MembershipDao
import settings.membership.Organization
import utils.JdbcTransacMode
import utils.UuidToStatement._
import settings.membership.Icon
import settings.account.Account
import log.OrgaEvent
import utils.Transition
import utils.TransacMode
import utils.TransactionalTransition._
import log.events.joinRequest.JoinResponseEventDto
import settings.membership.AbandonEventDto
import common.typeAliases._

/**
 * @author Gustavo
 *
 */
class MembershipDaoImpl extends MembershipDao {

	override def insert(organizationId: Organization.Id, iconTag: Icon.Tag, accountId: Account.Id, requestEventId: OrgaEvent.Id, acceptedEventId: OrgaEvent.Id, accepterMemberTag: Icon.Tag): Transition[TransacMode, Unit] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
insert into orga_membership (organization_id, icon_tag, user_id, account_tag, request_event_id, accepted_event_id, accepter_icon_tag, abandon_event_id)
values ($organizationId, $iconTag, ${accountId.userId}, ${accountId.tag}, $requestEventId, $acceptedEventId, $accepterMemberTag, null)"""
			sql.executeUpdate()
		}

	override def insertAbandonEvent(accountId: Account.Id, abandonEventId: OrgaEvent.Id): Transition[TransacMode, Int] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
update orga_membership set abandon_event_id = $abandonEventId
where user_id = ${accountId.userId} AND account_tag = ${accountId.tag} AND abandon_event_id is null"""
			sql.executeUpdate() match {
				case 1 =>
					// For efficiency sake, an indexed key of the orga_membership register is put in the orga_event register. This will speed up the search of an abandon event given its abandon_event_id.
					SQL"update orga_event set user_id = ${accountId.userId}, med_fk1 = ${accountId.tag} where id = $abandonEventId".executeUpdate.ensuring(_ == 1)
				case 0 => 0
				case _ => throw new AssertionError
			}
		}

	override def getJoinAcceptedEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]] = {
		val sql = """
select ms.accepted_event_id, e.instant, ms.request_event_id, responderIcon.name as resIName, requesterAcc.name as reqAName, requesterIcon.*
from orga_membership ms
inner join orga_account requesterAcc on (requesterAcc.user_id = ms.user_id AND requesterAcc.tag = ms.account_tag)
inner join orga_icon requesterIcon on (requesterIcon.organization_id = ms.organization_id AND requesterIcon.tag = ms.icon_tag)
inner join orga_icon responderIcon on (responderIcon.organization_id = ms.organization_id AND responderIcon.tag = ms.accepter_icon_tag)
inner join orga_event e on (e.id = ms.accepted_event_id)
where ms.organization_id = {organizationId} AND """

		val parser: RowParser[JoinResponseEventDto] =
			get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) ~ get[OrgaEvent.Id](3) ~ str(4) ~ str(5) ~ IconDaoImpl.iconDtoParser map {
				case responseEventId ~ instant ~ requestEventId ~ responderIconName ~ requesterAccountName ~ requesterIconDto =>
					JoinResponseEventDto(responseEventId, instant, Seq(requestEventId), responderIconName, requesterAccountName, Some(requesterIconDto), None)
			}

		val query = threshold match {
			case Left(edge) =>
				SQL(sql + "e.instant > {edge}").on("edge" -> edge)
			case Right(secondsBefore) =>
				SQL(sql + "e.instant >= localtimestamp - make_interval(secs:={secondsBefore})").on("secondsBefore" -> secondsBefore)
		}
		JdbcTransacMode.inConnection { implicit connection =>
			query.on("organizationId" -> organizationId).as(parser.*)
		}
	}

	override def getAbandonEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]] = {
		val sql = """
select ms.abandon_event_id, e.instant, i.*
from orga_event e
inner join orga_membership ms on (ms.user_id = e.user_id AND ms.account_tag = e.med_fk1 AND ms.abandon_event_id = e.id)
inner join orga_icon i on (i.organization_id = {organizationId} AND i.tag = ms.icon_tag)
where ms.organization_id = {organizationId} AND """

		val query = threshold match {
			case Left(edge) =>
				SQL(sql + "e.instant > {edge}").on("edge" -> edge)
			case Right(secondsBefore) =>
				SQL(sql + "e.instant >= localtimestamp - make_interval(secs:={secondsBefore})").on("secondsBefore" -> secondsBefore)
		}
		JdbcTransacMode.inConnection { implicit connection =>
			query.on("organizationId" -> organizationId).as(MembershipDaoImpl.abandonEventDtoParser.*)
		}
	}

	override def getBodyOfMembers(organizationId: Organization.Id): TiTac[Seq[IconDto]] = {
		val sql = SQL"""
select i.tag, i.name, i.present_role
from orga_membership ms
inner join orga_icon i on (i.tag = ms.icon_tag)
where ms.organization_Id = $organizationId and abandon_event_id is null"""
		JdbcTransacMode.inConnection { implicit connection =>
			sql.as(IconDaoImpl.iconDtoParser.*)
		}
	}
}

object MembershipDaoImpl {

	val abandonEventDtoParser: RowParser[AbandonEventDto] =
		get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) ~ IconDaoImpl.iconDtoParser map {
			case abandonEventId ~ instant ~ iconDto => AbandonEventDto(abandonEventId, instant, iconDto)
		}
}