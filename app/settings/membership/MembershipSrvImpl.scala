package settings.membership

import java.util.UUID

import com.google.inject.ImplementedBy

import auth.models.User
import javax.inject.Inject
import log.Event
import log.LogSrv
import log.LogSrvImpl
import log.events.joinRequest.JoinCancelEventDto
import log.events.joinRequest.JoinReject
import log.events.joinRequest.JoinRequest
import log.events.joinRequest.JoinRequestEventDto
import log.events.joinRequest.JoinResponseEventDto
import play.api.Logger
import settings.account.AccountId
import settings.account.AccountSrv
import utils.TransacMode
import utils.TransacTransitionExec
import utils.Transition

trait MembershipDao {
	def getMemberOf(accountId: AccountId): Transition[TransacMode, Option[MemberDto]]
	def insert(organizationId: Organization.Id, memberTag: Member.Tag, accountId: AccountId, requestEventId: Event.Id, acceptedEventId: Event.Id, accepterMemberTag: Member.Tag): Transition[TransacMode, Membership]
	def insertAbandonEvent(accountId: AccountId, abandonEventId: Event.Id): Transition[TransacMode, Option[AbandonEvent]]
	def getJoinAcceptedEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getAbandonEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]]
}

case class JoinRequestStatus(organization: Organization, rejectMsg: Option[String])
trait JoinRequestDao {
	def getJoinRequestStatusOf(accountId: AccountId): Transition[TransacMode, Option[JoinRequestStatus]]
	def insert(accountId: AccountId, organizationId: Organization.Id, eventId: Event.Id): Transition[TransacMode, JoinRequest]
	def insertCancelEvent(accountId: AccountId, cancelEventId: Event.Id, cancelEventInstant: Event.Instant): Transition[TransacMode, Int]
	def getJoinRequestEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinRequestEventDto]]
	def getJoinRejectionEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getJoinCancelEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinCancelEventDto]]
	def findByEventId(eventId: Event.Id): Transition[TransacMode, Option[JoinRequest]]
	def insertRejectionEvent(rejectionEventId: Event.Id, requesterAccountId: AccountId, requestEventId: Event.Id, rejectionMsg: String, rejecterMemberTag: Member.Tag): Transition[TransacMode, JoinReject]
}

trait MemberDao {
	def insert(organizationId: Organization.Id, name: String, role: Role, accountId: Option[AccountId]): Transition[TransacMode, Member]
	def findByHolder(holderAccountId: AccountId): Transition[TransacMode, Option[Member]]
	def update(updatedMember: Member): Transition[TransacMode, Member]
}

trait OrganizationDao {
	/**Gives all the [[Organization]]s that fulfill the received criteria.*/
	def search(criteria: SearchOrganizationsCmd): Transition[TransacMode, Seq[Organization]]
	/**Gives the [[Organization]] with the received id */
	def find(organizationId: Organization.Id): Transition[TransacMode, Option[Organization]]
	/**
	 * Inserts a new [[Organization]] into the underlying DB table and gives it.
	 * @return the new [[Organization]]
	 */
	def insert(project: CreateOrganizationCmd): Transition[TransacMode, Organization]
	/**Gives the [[Organization]] id of the received user [[Account]] */
	def findOf(accountId: AccountId): Transition[TransacMode, Option[Organization.Id]]
}

class MembershipSrvImpl @Inject() (transacTransitionExec: TransacTransitionExec, logSrv: LogSrv, organizationDao: OrganizationDao, membershipDao: MembershipDao, joinRequestDao: JoinRequestDao, memberDao: MemberDao, accountSrv: AccountSrv)
	extends MembershipSrv {
	val logger = Logger(this.getClass())

	override def getMembershipStatusOf(accountId: AccountId): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		membershipDao.getMemberOf(accountId).flatMap {
			case sm @ Some(memberDto) =>
				organizationDao.find(memberDto.organizationId).map(MembershipStatusDto(_, sm, None))
			case None =>
				joinRequestDao.getJoinRequestStatusOf(accountId).map {
					case None => MembershipStatusDto(None, None, None)
					case Some(jrs) => MembershipStatusDto(Some(jrs.organization), None, jrs.rejectMsg)
				}
		}
	}

	override def searchOrganizations(criteria: SearchOrganizationsCmd) = organizationDao.search(criteria)

	override def sendJoinRequest(userId: UUID, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		val accountId = AccountId(userId, sendJoinRequestCmd.accountTag)
		getMembershipStatusOf(accountId).flatMap {
			case MembershipStatusDto(None, None, None) => organizationDao.find(sendJoinRequestCmd.organizationId).flatMap {
				case None =>
					Transition.unit(MembershipStatusDto(None, None, Some("the organization stoped to exists")))
				case so @ Some(organization) =>
					logSrv.newEvent().flatMap { event =>
						memberDao.findByHolder(accountId).flatMap {
							case Some(member) if member.role.canJoinDirectly =>
								membershipDao.insert(organization.id, accountId.tag, accountId, event._1, event._1, accountId.tag).map { membership =>
									MembershipStatusDto(so, Some(MemberDto(member.tag, member.name, organization.id, member.role)), None)
								}
							case _ =>
								joinRequestDao.insert(accountId, sendJoinRequestCmd.organizationId, event._1).map { joinRequest =>
									MembershipStatusDto(so, None, None)
								}
						}
					}
			}
			case msd @ _ => Transition.unit(msd)
		}
	}

	override def cancelJoinRequest(accountId: AccountId): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		// check if he belongs to an organization (can happen if he cancels from an old not actualized client instance after having joined using other client instance)
		organizationDao.findOf(accountId).flatMap {
			// if it belongs to an organization, ignore the cancel command
			case Some(_) => getMembershipStatusOf(accountId)
			// else create the cancel event if the request haven't expired. The cancel event is necessary to update the join request events seen by members 
			case None =>
				logSrv.newEvent().flatMap { cancelEvent =>
					joinRequestDao.insertCancelEvent(accountId, cancelEvent._1, cancelEvent._2).map {
						case x if x <= 1 => MembershipStatusDto(None, None, None)
					}
				}
		}
	}

	override def leaveOrganization(accountId: AccountId): Transition[TransacMode, MembershipStatusDto] = {
		logSrv.newEvent().flatMap { abandonEvent =>
			membershipDao.insertAbandonEvent(accountId, abandonEvent._1).flatMap {
				case Some(_) => Transition.unit(MembershipStatusDto(None, None, None))
				case None => getMembershipStatusOf(accountId)
			}
		}
	}

	override def createOrganization(userId: User.Id, createOrganizationCmd: CreateOrganizationCmd): Transition[TransacMode, (Organization, Member, Membership)] =
		transacTransitionExec.inTransaction {
			organizationDao.insert(createOrganizationCmd).flatMap { organization =>
				val accountId = AccountId(userId, createOrganizationCmd.accountTag)
				memberDao.insert(organization.id, createOrganizationCmd.memberName.getOrElse(createOrganizationCmd.accountName), Leader, Some(accountId)).flatMap { member =>
					logSrv.newEvent().flatMap { event =>
						membershipDao.insert(organization.id, member.tag, accountId, event._1, event._1, member.tag).map { membership =>
							(organization, member, membership)
						}
					}
				}
			}
		}

	override def getOrganizationOf(accountId: AccountId): Transition[TransacMode, Option[Organization.Id]] = organizationDao.findOf(accountId)

	def getEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[Event]] =
		for {
			joinAcceptedEvents <- membershipDao.getJoinAcceptedEventsAfter(eventId, organizationId)
			abandonEvents <- membershipDao.getAbandonEventsAfter(eventId, organizationId)
		} yield (joinAcceptedEvents ++ abandonEvents)

}
