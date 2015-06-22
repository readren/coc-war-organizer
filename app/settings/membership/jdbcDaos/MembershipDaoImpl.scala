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
import log.events.joinRequest.JoinResponseEventDto
import settings.membership.AbandonEventDto

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
select ms.accepted_event_id, e.instant, ms.request_event_id, responderMem.name, requesterAcc.name, requesterMem.name
from orga_membership ms
inner join orga_account requesterAcc on (requesterAcc.user_id = ms.user_id AND requesterAcc.tag = ms.account_tag)
inner join orga_icon requesterMem on (requesterMem.organization_id = ms.organization_id AND requesterMem.tag = ms.icon_tag)
inner join orga_icon responderMem on (responderMem.organization_id = ms.organization_id AND responderMem.tag = ms.accepter_icon_tag)
inner join orga_event e on (e.id = ms.accepted_event_id)
where ms.organization_id = {organizationId} AND """

		val query = threshold match {
			case Left(edge) =>
				SQL(sql + "e.instant > {edge}").on("edge" -> edge)
			case Right(secondsBefore) =>
				SQL(sql + "e.instant >= localtimestamp - make_interval(secs:={secondsBefore})").on("secondsBefore" -> secondsBefore)
		}
		JdbcTransacMode.inConnection { implicit connection =>
			query.on("organizationId" -> organizationId).as(MembershipDaoImpl.joinResponseEventDtoParser.*)
		}
	}

	override def getAbandonEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]] = {
		val sql = """
select ms.abandon_event_id, e.instant, i.name
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
}

object MembershipDaoImpl {
	val joinResponseEventDtoParser: RowParser[JoinResponseEventDto] =
		get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) ~ get[OrgaEvent.Id](3) ~ str(4) ~ str(5) ~ str(6) map {
			case responseEventId ~ instant ~ requestEventId ~ responderMemberName ~ requesterAccountName ~ requesterMemberName =>
				JoinResponseEventDto(responseEventId, instant, Seq(requestEventId), responderMemberName, requesterAccountName, Some(requesterMemberName), None)
		}

	val abandonEventDtoParser: RowParser[AbandonEventDto] =
		get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) ~ str(3) map {
			case abandonEventId ~ instant ~ memberName => AbandonEventDto(abandonEventId, instant: OrgaEvent.Instant, memberName: String)
		}
}