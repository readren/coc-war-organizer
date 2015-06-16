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
import settings.membership.Membership
import settings.membership.Icon
import settings.account.Account
import log.Event
import utils.Transition
import utils.TransacMode
import log.events.joinRequest.JoinResponseEventDto
import settings.membership.AbandonEventDto

/**
 * @author Gustavo
 *
 */
class MembershipDaoImpl extends MembershipDao {

	override def insert(organizationId: Organization.Id, iconTag: Icon.Tag, accountId: Account.Id, requestEventId: Event.Id, acceptedEventId: Event.Id, accepterMemberTag: Icon.Tag): Transition[TransacMode, Membership] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
insert into orga_membership (organization_id, icon_tag, user_id, account_tag, request_event_id, accepted_event_id, accepter_icon_tag, abandon_event_id)
values ($organizationId, $iconTag, ${accountId.userId}, ${accountId.tag}, $requestEventId, $acceptedEventId, $accepterMemberTag, null)"""
			sql.executeUpdate()
			Membership(organizationId, iconTag, accountId, requestEventId, acceptedEventId, accepterMemberTag)
		}

	override def insertAbandonEvent(accountId: Account.Id, abandonEventId: Event.Id): Transition[TransacMode, Int] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
update orga_membership set abandon_event_id = $abandonEventId
where user_id = ${accountId.userId} AND account_tag = ${accountId.tag} AND abandon_event_id is null"""
			sql.executeUpdate() match {
				case 1 =>
					// For efficiency sake, an indexed key of the orga_membership register is put in the orga_event register. This will speed up the search of an abandon event given its abandon_event_id.
					SQL"update orga_event set user_id = ${accountId.userId}, med_fk1 = ${accountId.tag} where id = $abandonEventId".executeUpdate.ensuring(_==1)
				case 0 => 0
				case _ => throw new AssertionError
			}
		}

	override def getJoinAcceptedEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]] = JdbcTransacMode.inConnection { implicit connection =>
		val eventId: Event.Id = oEventId.getOrElse(0L)
		val sql = SQL"""
select ms.accepted_event_id, e.instant, ms.request_event_id, responderMem.name, requesterAcc.name, requesterMem.name
from orga_membership ms
inner join orga_account requesterAcc on (requesterAcc.user_id = ms.user_id AND requesterAcc.tag = ms.account_tag)
inner join orga_icon requesterMem on (requesterMem.organization_id = ms.organization_id AND requesterMem.tag = ms.icon_tag)
inner join orga_icon responderMem on (responderMem.organization_id = ms.organization_id AND responderMem.tag = ms.accepter_icon_tag)
inner join orga_event e on (e.id = ms.accepted_event_id)
where ms.accepted_event_id > $eventId AND ms.organization_id = $organizationId"""
		sql.as(MembershipDaoImpl.joinResponseEventDtoParser.*)
	}

	override def getAbandonEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]] =
		JdbcTransacMode.inConnection { implicit connection =>
			val eventId: Event.Id = oEventId.getOrElse(0L)
			val sql = SQL"""
select ms.abandon_event_id, e.instant, i.name
from orga_event e
inner join orga_membership ms on (ms.user_id = e.user_id AND ms.account_tag = e.med_fk1 AND ms.abandon_event_id = e.id)
inner join orga_icon i on (i.organization_id = $organizationId AND i.tag = ms.icon_tag)
where e.id > $eventId AND ms.organization_id = $organizationId"""
			sql.as(MembershipDaoImpl.abandonEventDtoParser.*)
		}
}

object MembershipDaoImpl {
	val joinResponseEventDtoParser: RowParser[JoinResponseEventDto] =
		get[Event.Id](1) ~ get[Event.Instant](2) ~ get[Event.Id](3) ~ str(4) ~ str(5) ~ str(6) map {
			case responseEventId ~ instant ~ requestEventId ~ responderMemberName ~ requesterAccountName ~ requesterMemberName =>
				JoinResponseEventDto(responseEventId, instant, Seq(requestEventId), responderMemberName, requesterAccountName, Some(requesterMemberName), None)
		}

	val abandonEventDtoParser: RowParser[AbandonEventDto] =
		get[Event.Id](1) ~ get[Event.Instant](2) ~ str(3) map {
			case abandonEventId ~ instant ~ memberName => AbandonEventDto(abandonEventId, instant: Event.Instant, memberName: String)
		}
}