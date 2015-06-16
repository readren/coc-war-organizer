/**
 *
 */
package settings.membership.jdbcDaos

import settings.membership.JoinRequestDao
import auth.models.User
import settings.account.Account
import settings.membership.JoinRequestStatus
import settings.membership.SendJoinRequestCmd
import utils.JdbcTransacMode
import anorm._
import utils.UuidToStatement._
import anorm.ParameterValue.toParameterValue
import anorm.SqlParser.str
import log.Event
import settings.membership.Organization
import utils.Transition
import utils.TransacMode
import log.events.joinRequest.JoinRequestEventDto
import java.sql.Timestamp
import settings.account.Account
import settings.account.AccountDaoImpl
import settings.membership.Icon
import log.events.joinRequest.JoinResponseEventDto
import anorm.SqlParser._
import log.events.joinRequest.JoinCancelEventDto
import log.events.joinRequest.JoinRequest
import log.events.joinRequest.JoinReject

/**
 * @author Gustavo
 *
 */
class JoinRequestDaoImpl extends JoinRequestDao {
	override def insert(accountId: Account.Id, organizationId: Organization.Id, requestEventId: Event.Id) = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"""
insert into orga_join_request (user_id, account_tag, organization_id, request_event_id)
values (${accountId.userId}, ${accountId.tag}, $organizationId, $requestEventId)"""
		sql.executeUpdate()
		JoinRequest(accountId, organizationId, requestEventId)
	}

	override def insertCancelEvent(accountId: Account.Id, cancelEventId: Event.Id, cancelEventInstant: Event.Instant, becauseAccepted:Boolean): Transition[TransacMode, Int] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
update orga_join_request set cancel_event_id = $cancelEventId, because_accepted = $becauseAccepted
where user_id = ${accountId.userId} AND account_tag = ${accountId.tag} AND cancel_event_id is null"""
			sql.executeUpdate() match {
				case 0 => 0
				case 1 =>
					// For efficiency sake, an indexed key of the orga_join_request register is put in the orga_event register. This will speed up the search of a cancel event given its cancel_event_id.
					SQL"update orga_event set user_id = ${accountId.userId}, med_fk1 = ${accountId.tag} where id = $cancelEventId".executeUpdate.ensuring(_ == 1)
				case _ => throw new AssertionError
			}
		}

	override def getJoinRequestStatusOf(accountId: Account.Id): Transition[TransacMode, Option[JoinRequestStatus]] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
select o.*, jr.rejection_msg
from orga_join_request jr
inner join orga_organization o on(o.id = jr.organization_id)
where jr.user_id = ${accountId.userId} AND jr.account_tag = ${accountId.tag} AND jr.cancel_event_id is null"""
			sql.as(JoinRequestDaoImpl.joinRequestStatusParser.singleOpt)
		}

	override def getJoinRequestEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinRequestEventDto]] =
		JdbcTransacMode.inConnection { implicit connection =>
			val eventId: Event.Id = oEventId.getOrElse(0L)
			val sql = SQL"""
select jr.request_event_id, e.instant, a.name, jr.rejection_msg
from orga_join_request jr
inner join orga_account a on (a.user_id = jr.user_id AND a.tag = jr.account_tag)
inner join orga_event e on (e.id = jr.request_event_id)
where jr.request_event_id > $eventId AND jr.organization_id = $organizationId"""
			sql.as(JoinRequestDaoImpl.joinRequestEventDtoParser.*)
		}

	override def findByEventId(eventId: Event.Id): Transition[TransacMode, Option[JoinRequest]] = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"select * from orga_join_request jr where jr.request_event_id = $eventId"
		sql.as(JoinRequestDaoImpl.joinRequestParser.singleOpt)
	}

	override def insertRejectionEvent(rejectionEventId: Event.Id, requesterAccountId: Account.Id, requestEventId: Event.Id, rejectionMsg: String, rejecterMemberTag: Icon.Tag): Transition[TransacMode, JoinReject] =
		JdbcTransacMode.inConnection { implicit connection =>
			val sql = SQL"""
update orga_join_request
	set rejection_msg = ${rejectionMsg}, rejection_event_id = ${rejectionEventId}, rejecter_icon_tag = ${rejecterMemberTag}
where user_id	= ${requesterAccountId.userId} AND account_tag = ${requesterAccountId.tag} AND request_event_id = $requestEventId"""
			val count = sql.executeUpdate()
			if (count > 1) throw new AssertionError
			else if (count == 1)
				SQL"update orga_event set user_id = ${requesterAccountId.userId}, med_fk1 = ${requesterAccountId.tag} where id = $rejectionEventId".executeUpdate.ensuring(_ == 1)
			JoinReject(rejectionEventId, requesterAccountId, requestEventId, rejectionMsg, rejecterMemberTag)
		}

	override def getJoinRejectionEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]] = {
		val eventId = oEventId.getOrElse(0L)
		val sql = SQL"""
select e.id, e.instant, jr.request_event_id, rejecterIcon.name, requesterAcc.name, jr.rejection_msg
from orga_event e
inner join orga_join_request jr on (jr.user_id = e.user_id AND jr.account_tag = e.med_fk1 AND jr.rejection_event_id = e.id)
inner join orga_icon rejecterIcon on (rejecterIcon.organization_id = jr.organization_id AND rejecterIcon.tag = jr.rejecter_icon_tag)
inner join orga_account requesterAcc on (requesterAcc.user_id = jr.user_id AND requesterAcc.tag = jr.account_tag)
where e.id > $eventId AND jr.organization_id = $organizationId"""
		val parser = get[Event.Id](1) ~ get[Event.Instant](2) ~ get[Event.Id](3) ~ str(4) ~ str(5) ~ str(6) map {
			case id ~ instant ~ affectedEvent ~ responderMemberName ~ requesterAccountName ~ rejectionMsg =>
				JoinResponseEventDto(id, instant, Seq(affectedEvent), responderMemberName, requesterAccountName, None, Some(rejectionMsg))
		}
		JdbcTransacMode.inConnection { implicit connection =>
			sql.as(parser.*)
		}
	}

	override def getJoinCancelEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinCancelEventDto]] = {
		val eventId = oEventId.getOrElse(0L)
		val sql = SQL"""
select e.id, e.instant, a.name, jr.request_event_id
from orga_event e
inner join orga_join_request jr on (jr.user_id = e.user_id AND jr.account_tag = e.med_fk1 AND jr.cancel_event_id = e.id)			  
inner join orga_account a on (a.user_id = e.user_id AND a.tag = e.med_fk1)
where e.id > $eventId AND organization_id = $organizationId AND jr.because_accepted = false AND jr.rejection_event_id is null"""
		val parser = get[Event.Id](1) ~ get[Event.Instant](2) ~ str(3) ~ get[Event.Id](4) map {
			case id ~ instant ~ accountName ~ affectedEvent => JoinCancelEventDto(id, instant, accountName, Seq(affectedEvent))
		}
		JdbcTransacMode.inConnection { implicit connection =>
			sql.as(parser.*)
		}
	}
}

object JoinRequestDaoImpl {
	import anorm.SqlParser._

	val joinRequestParser: RowParser[JoinRequest] = {
		AccountDaoImpl.pkParser("user_id", "account_tag") ~ get[Organization.Id]("organization_id") ~ get[Event.Id]("request_event_id") map {
			case accountId ~ organizationId ~ requestEventId => JoinRequest(accountId, organizationId, requestEventId)
		}
	}

	val joinRequestStatusParser: RowParser[JoinRequestStatus] = {
		OrganizationDaoImpl.organizationParser ~ str("rejection_msg").? map {
			case organization ~ rejectionMsg => JoinRequestStatus(organization, rejectionMsg)
		}
	}

	val joinRequestEventDtoParser: RowParser[JoinRequestEventDto] = {
		get[Event.Id](1) ~ get[Event.Instant](2) ~ str(3) ~ str(4).? map {
			case eventId ~ instant ~ accountName ~ rejectMsg => JoinRequestEventDto(eventId, instant, accountName, rejectMsg)
		}
	}

	//	val joinRejectParser: RowParser[JoinReject] = {
	//		str("rejection_msg") ~ get[Member.Tag]("rejecter_icon_tag") ~ get[Event.Id]("rejection_event_id") map {
	//			case rejectionMsg ~ rejecterMemberTag ~ rejectionEventId => JoinReject(rejectionEventId, requesterAccountId, requestEventId, rejectionMsg, rejecterMemberTag)
	//		}
	//	}

}