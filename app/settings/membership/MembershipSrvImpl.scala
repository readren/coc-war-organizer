package settings.membership

import java.util.UUID

import com.google.inject.ImplementedBy

import auth.models.User
import javax.inject.Inject
import log.OrgaEvent
import log.LogSrv
import log.LogSrvImpl
import log.events.joinRequest.JoinCancelEventDto
import log.events.joinRequest.JoinReject
import log.events.joinRequest.JoinRequest
import log.events.joinRequest.JoinRequestEventDto
import log.events.joinRequest.JoinResponseEventDto
import play.api.Logger
import settings.account.Account
import settings.account.AccountSrv
import utils.TransacMode
import utils.TransacTransitionExec
import utils.Transition
import common.typeAliases._

trait MembershipDao {
	def insert(organizationId: Organization.Id, memberTag: Icon.Tag, accountId: Account.Id, requestEventId: OrgaEvent.Id, acceptedEventId: OrgaEvent.Id, accepterMemberTag: Icon.Tag): Transition[TransacMode, Unit]
	def insertAbandonEvent(accountId: Account.Id, abandonEventId: OrgaEvent.Id): Transition[TransacMode, Int]
	def getJoinAcceptedEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getAbandonEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]]
}

case class JoinRequestStatus(organization: Organization, rejectMsg: Option[String])
trait JoinRequestDao {
	def getJoinRequestStatusOf(accountId: Account.Id): Transition[TransacMode, Option[JoinRequestStatus]]
	def insert(accountId: Account.Id, organizationId: Organization.Id, eventId: OrgaEvent.Id): Transition[TransacMode, JoinRequest]
	def insertCancelEvent(accountId: Account.Id, cancelEventId: OrgaEvent.Id, cancelEventInstant: OrgaEvent.Instant, becauseAccepted: Boolean): Transition[TransacMode, Int]
	def getJoinRequestEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinRequestEventDto]]
	def getJoinRejectionEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getJoinCancelEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinCancelEventDto]]
	def findByEventId(eventId: OrgaEvent.Id): Transition[TransacMode, Option[JoinRequest]]
	def insertRejectionEvent(rejectionEventId: OrgaEvent.Id, requesterAccountId: Account.Id, requestEventId: OrgaEvent.Id, rejectionMsg: String, rejecterMemberTag: Icon.Tag): Transition[TransacMode, JoinReject]
}

trait IconDao {
	def insert(organizationId: Organization.Id, name: String, role: Role, accountId: Account.Id): Transition[TransacMode, Icon.Tag]
	/** Gives the icon held by the received account in his current organization */
	def findByAccount(accountId: Account.Id): Transition[TransacMode, Option[IconDto]]
	def findByHolder(organizationId: Organization.Id, holderAccountId: Account.Id): Transition[TransacMode, Option[IconDto]]
	def insertRoleChangeEvent(roleChangeEventId: OrgaEvent.Id, organizationId: Organization.Id, affectedIconTag: Icon.Tag, newRole: Role, previousRole: Role, changerIconTag: Icon.Tag): Transition[TransacMode, Unit]
	def getRoleChangeEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[RoleChangeEventDto]]
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
	def findOf(accountId: Account.Id): Transition[TransacMode, Option[Organization.Id]]
}

class MembershipSrvImpl @Inject() (transacTransitionExec: TransacTransitionExec, logSrv: LogSrv, organizationDao: OrganizationDao, membershipDao: MembershipDao, joinRequestDao: JoinRequestDao, iconDao: IconDao, accountSrv: AccountSrv)
		extends MembershipSrv {
	val logger = Logger(this.getClass())

	override def getMembershipStatusOf(accountId: Account.Id): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		iconDao.findByAccount(accountId).flatMap {
			case sm @ Some(iconDto) =>
				organizationDao.find(iconDto.organizationId).map(MembershipStatusDto(_, sm, None))
			case None =>
				joinRequestDao.getJoinRequestStatusOf(accountId).map {
					case None => MembershipStatusDto(None, None, None)
					case Some(jrs) => MembershipStatusDto(Some(jrs.organization), None, jrs.rejectMsg)
				}
		}
	}

	override def searchOrganizations(criteria: SearchOrganizationsCmd) = organizationDao.search(criteria)

	override def sendJoinRequest(userId: UUID, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		val accountId = Account.Id(userId, sendJoinRequestCmd.accountTag)
		getMembershipStatusOf(accountId).flatMap {
			case MembershipStatusDto(None, None, None) => organizationDao.find(sendJoinRequestCmd.organizationId).flatMap {
				case None =>
					Transition.unit(MembershipStatusDto(None, None, Some("the organization stoped to exists")))
				case so @ Some(organization) =>
					logSrv.newEvent().flatMap { event =>
						iconDao.findByHolder(organization.id, accountId).flatMap {
							case Some(icon) if icon.role.canJoinDirectly =>
								for (_ <- membershipDao.insert(organization.id, icon.tag, accountId, event._1, event._1, icon.tag))
									yield MembershipStatusDto(so, Some(icon), None)
							case _ =>
								for (_ <- joinRequestDao.insert(accountId, sendJoinRequestCmd.organizationId, event._1))
									yield MembershipStatusDto(so, None, None)
						}
					}
			}
			case msd @ _ => Transition.unit(msd)
		}
	}

	override def cancelJoinRequest(accountId: Account.Id, becauseAccepted: Boolean): Transition[TransacMode, MembershipStatusDto] = transacTransitionExec.inTransaction {
		// check if he belongs to an organization (can happen if he cancels from an old not actualized client instance after having joined using other client instance)
		organizationDao.findOf(accountId).flatMap {
			// if it belongs to an organization, ignore the cancel command
			case Some(_) => getMembershipStatusOf(accountId)
			// else create the cancel event if the request haven't expired. The cancel event is necessary to update the join request events seen by members 
			case None =>
				logSrv.newEvent().flatMap { cancelEvent =>
					joinRequestDao.insertCancelEvent(accountId, cancelEvent._1, cancelEvent._2, becauseAccepted).map {
						case x if x <= 1 => MembershipStatusDto(None, None, None)
					}
				}
		}
	}

	override def leaveOrganization(accountId: Account.Id): Transition[TransacMode, MembershipStatusDto] = {
		logSrv.newEvent().flatMap { abandonEvent =>
			membershipDao.insertAbandonEvent(accountId, abandonEvent._1).flatMap {
				case 1 => Transition.unit(MembershipStatusDto(None, None, None))
				case 0 => getMembershipStatusOf(accountId)
			}
		}
	}

	override def createOrganization(userId: User.Id, createOrganizationCmd: CreateOrganizationCmd): Transition[TransacMode, (Organization, IconDto)] =
		transacTransitionExec.inTransaction {
			for {
				organization <- organizationDao.insert(createOrganizationCmd)
				accountId = Account.Id(userId, createOrganizationCmd.accountTag)
				name = createOrganizationCmd.iconName.getOrElse(createOrganizationCmd.accountName)
				iconTag <- iconDao.insert(organization.id, name, Leader, accountId)
				event <- logSrv.newEvent()
				_ <- membershipDao.insert(organization.id, iconTag, accountId, event._1, event._1, iconTag)
			} yield (organization, IconDto(iconTag, name, organization.id, Leader))
		}

	override def getOrganizationOf(accountId: Account.Id): Transition[TransacMode, Option[Organization.Id]] = organizationDao.findOf(accountId)

	override def getEventsAfter(threshold: Either[OrgaEvent.Instant, Int], organizationId: Organization.Id): Transition[TransacMode, Seq[OrgaEvent]] =
		for {
			joinAcceptedEvents <- membershipDao.getJoinAcceptedEventsAfter(threshold, organizationId)
			abandonEvents <- membershipDao.getAbandonEventsAfter(threshold, organizationId)
			roleChangeEvents <- iconDao.getRoleChangeEventsAfter(threshold, organizationId)
		} yield (joinAcceptedEvents ++ abandonEvents ++ roleChangeEvents)

	override def getOrgaStatus(accountId: Account.Id, cmd: GetOrgaStatusCmd): TiTac[GetOrgaStatusDto] = {
		???
	}

}
