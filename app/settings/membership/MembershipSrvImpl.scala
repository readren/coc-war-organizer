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
import settings.account.Account
import settings.account.AccountSrv
import utils.TransacMode
import utils.TransacTransitionExec
import utils.Transition

trait MembershipDao {
	def insert(organizationId: Organization.Id, memberTag: Icon.Tag, accountId: Account.Id, requestEventId: Event.Id, acceptedEventId: Event.Id, accepterMemberTag: Icon.Tag): Transition[TransacMode, Membership]
	def insertAbandonEvent(accountId: Account.Id, abandonEventId: Event.Id): Transition[TransacMode, Int]
	def getJoinAcceptedEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getAbandonEventsAfter(oEventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[AbandonEventDto]]
}

case class JoinRequestStatus(organization: Organization, rejectMsg: Option[String])
trait JoinRequestDao {
	def getJoinRequestStatusOf(accountId: Account.Id): Transition[TransacMode, Option[JoinRequestStatus]]
	def insert(accountId: Account.Id, organizationId: Organization.Id, eventId: Event.Id): Transition[TransacMode, JoinRequest]
	def insertCancelEvent(accountId: Account.Id, cancelEventId: Event.Id, cancelEventInstant: Event.Instant, becauseAccepted:Boolean): Transition[TransacMode, Int]
	def getJoinRequestEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinRequestEventDto]]
	def getJoinRejectionEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinResponseEventDto]]
	def getJoinCancelEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[JoinCancelEventDto]]
	def findByEventId(eventId: Event.Id): Transition[TransacMode, Option[JoinRequest]]
	def insertRejectionEvent(rejectionEventId: Event.Id, requesterAccountId: Account.Id, requestEventId: Event.Id, rejectionMsg: String, rejecterMemberTag: Icon.Tag): Transition[TransacMode, JoinReject]
}

trait IconDao {
	def insert(organizationId: Organization.Id, name: String, role: Role, accountId: Account.Id): Transition[TransacMode, Icon.Tag]
	/** Gives the icon held by the received account in his current organization */
	def findByAccount(accountId: Account.Id): Transition[TransacMode, Option[IconDto]]
	def findByHolder(organizationId:Organization.Id, holderAccountId: Account.Id): Transition[TransacMode, Option[IconDto]]
	def insertRoleChangeEvent(roleChangeEventId:Event.Id, organizationId:Organization.Id, affectedIconTag: Icon.Tag, newRole: Role, previousRole: Role, changerIconTag: Icon.Tag): Transition[TransacMode, Unit] 
  def getRoleChangeEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[RoleChangeEventDto]] 
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
								membershipDao.insert(organization.id, icon.tag, accountId, event._1, event._1, icon.tag).map { membership =>
									MembershipStatusDto(so, Some(icon), None)
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

	override def createOrganization(userId: User.Id, createOrganizationCmd: CreateOrganizationCmd): Transition[TransacMode, (Organization, IconDto, Membership)] =
		transacTransitionExec.inTransaction {
			organizationDao.insert(createOrganizationCmd).flatMap { organization =>
				val accountId = Account.Id(userId, createOrganizationCmd.accountTag)
				val name = createOrganizationCmd.iconName.getOrElse(createOrganizationCmd.accountName)
				iconDao.insert(organization.id, name, Leader, accountId).flatMap { iconTag =>
					logSrv.newEvent().flatMap { event =>
						membershipDao.insert(organization.id, iconTag, accountId, event._1, event._1, iconTag).map { membership =>
							(organization, IconDto(iconTag, name, organization.id, Leader), membership)
						}
					}
				}
			}
		}

	override def getOrganizationOf(accountId: Account.Id): Transition[TransacMode, Option[Organization.Id]] = organizationDao.findOf(accountId)

	def getEventsAfter(eventId: Option[Event.Id], organizationId: Organization.Id): Transition[TransacMode, Seq[Event]] =
		for {
			joinAcceptedEvents <- membershipDao.getJoinAcceptedEventsAfter(eventId, organizationId)
			abandonEvents <- membershipDao.getAbandonEventsAfter(eventId, organizationId)
			roleChangeEvents <- iconDao.getRoleChangeEventsAfter(eventId, organizationId)
		} yield (joinAcceptedEvents ++ abandonEvents ++ roleChangeEvents)

}
