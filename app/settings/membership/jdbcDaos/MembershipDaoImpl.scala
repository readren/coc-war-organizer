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
import settings.account.AccountId
import log.Event
import utils.Transition
import utils.TransacMode
import log.events.joinRequest.JoinResponseEventDto
import settings.membership.AbandonEvent
import settings.membership.AbandonEvent
import settings.membership.AbandonEventDto

/**
 * @author Gustavo
 *
 */
class MembershipDaoImpl extends MembershipDao {

	override def getMemberOf(accountId: AccountId): Transition[TransacMode, Option[MemberDto]] = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"""
select member.*
from orga_membership ms
inner join orga_member member on (member.organization_id = ms.organization_id and member.tag = ms.member_tag)
where ms.user_id = ${accountId.userId} and ms.account_tag = ${accountId.tag} and ms.abandon_event_id is null"""
		sql.as(MemberDaoImpl.memberDtoParser.singleOpt)
	}

	override def insert(organizationId: Organization.Id, memberTag: Member.Tag, accountId: AccountId, requestEventId: Event.Id, acceptedEventId: Event.Id, accepterMemberTag: Member.Tag): Transition[TransacMode, Membership] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
insert into orga_membership (organization_id, member_tag, user_id, account_tag, request_event_id, accepted_event_id, accepter_member_tag, abandon_event_id)
values ($organizationId, $memberTag, ${accountId.userId}, ${accountId.tag}, $requestEventId, $acceptedEventId, $accepterMemberTag, null)"""
			sql.executeUpdate()
			Membership(organizationId, memberTag, accountId, requestEventId, acceptedEventId, accepterMemberTag)
		}

	override def insertAbandonEvent(accountId: AccountId, abandonEventId: Event.Id): Transition[TransacMode, Option[AbandonEvent]] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
update orga_membership set abandon_event_id = $abandonEventId
where user_id = ${accountId.userId} and account_tag = ${accountId.tag} and abandon_event_id is null"""
			if (sql.executeUpdate() == 1)
				Some(AbandonEvent(abandonEventId, accountId))
			else None
		}

	override def getJoinAcceptedEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]] = JdbcTransacMode.inConnection { implicit connection =>
		val eventId: Event.Id = oEventId.getOrElse(0L)
		val sql = SQL"""
select ms.accepted_event_id, e.instant, ms.request_event_id, responderMem.name, requesterAcc.name, requesterMem.name
from orga_membership ms
inner join orga_account requesterAcc on (requesterAcc.user_id = ms.user_id and requesterAcc.tag = ms.account_tag)
inner join orga_member requesterMem on (requesterMem.organization_id = ms.organization_id and requesterMem.tag = ms.member_tag)
inner join orga_member responderMem on (responderMem.organization_id = ms.organization_id and responderMem.tag = ms.accepter_member_tag)
inner join orga_event e on (e.id = ms.accepted_event_id)
where ms.accepted_event_id > $eventId and ms.organization_id = $organizationId"""
		sql.as(MembershipDaoImpl.joinResponseEventDtoParser.*)
	}

	override def getAbandonEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]] =
		JdbcTransacMode.inConnection { implicit connection =>
			val eventId: Event.Id = oEventId.getOrElse(0L)
			val sql = SQL"""
select ms.abandon_event_id, e.instant, mi.name
from orga_membership ms
inner join orga_member mi on (mi.organization_id = ms.organization_id and mi.tag = ms.member_tag)
inner join orga_event e on(e.id = ms.abandon_event_id)
where ms.abandon_event_id > $eventId and ms.organization_id = $organizationId"""
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