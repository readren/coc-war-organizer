package settings.membership

import javax.inject.Inject
import java.util.UUID
import utils.TransacTransitionExec
import utils.Transition
import utils.TransacMode
import auth.models.User
import play.api.Logger
import settings.account.Account
import settings.account.AccountService

trait MembershipDao {
	def getMemberAndOrganizationOf(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, Option[(MemberDto, Organization)]]
	def insert(organizationId: Organization.Id, memberTag: Member.Tag, userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, Membership]
	def delete(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, Int]
}

case class JoinRequestStatus(organization:Organization, rejectMsg: Option[String])
trait JoinRequestDao {
	def getJoinRequestStatusOf(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, Option[JoinRequestStatus]]
	def insert(userId:User.Id, sendJoinRequestCmd: SendJoinRequestCmd): Transition[TransacMode, JoinRequest]
	def delete(userId:User.Id, accountTag:Account.Tag):Transition[TransacMode, Int]
}

trait MemberDao {
	def insert(userId: User.Id, name: String): Transition[TransacMode, Member]
}

trait OrganizationDao {
	def search(criteria: SearchOrganizationsCmd): Transition[TransacMode, Seq[Organization]]
	def get(organizationId: Organization.Id): Transition[TransacMode, Option[Organization]]
	def insert(project: CreateOrganizationCmd): Transition[TransacMode, Organization]
}


class MembershipSrvImpl @Inject() (transacTransitionExec: TransacTransitionExec, organizationDao: OrganizationDao, membershipDao: MembershipDao, joinRequestDao: JoinRequestDao, memberDao: MemberDao, accountService:AccountService)
	extends MembershipSrv {
	val logger = Logger(this.getClass())

	override def getMembershipStatusOf(userId: User.Id, accountTag: Account.Tag) = 
		membershipDao.getMemberAndOrganizationOf(userId, accountTag).flatMap {
			case Some(mao) => Transition.unit(MembershipStatusDto(Some(mao._2), Some(mao._1), None))
			case None => joinRequestDao.getJoinRequestStatusOf(userId, accountTag).map { 
				case Some(jrs) =>	MembershipStatusDto(Some(jrs.organization), None, jrs.rejectMsg)
				case None => MembershipStatusDto(None, None, None)
			}
		}
	
	override def searchOrganizations(criteria: SearchOrganizationsCmd) = organizationDao.search(criteria)
	
	override def sendJoinRequest(userId: UUID, sendJoinRequestCmd: SendJoinRequestCmd) = transacTransitionExec.inTransaction {
		getMembershipStatusOf(userId, sendJoinRequestCmd.accountTag).flatMap { membershipStatusDto => 
			membershipStatusDto.organization match {
				case Some(organization) =>
					logger.warn(s"posible malicius attack of $userId")
					Transition.unit(membershipStatusDto)
				case None =>
					organizationDao.get(sendJoinRequestCmd.organizationId).flatMap {
						case so @ Some(organization) =>
							joinRequestDao.insert(userId, sendJoinRequestCmd).map { joinRequest =>
								MembershipStatusDto(so, None, None)
						}
						case None =>
							Transition.unit(MembershipStatusDto(None, None, Some("the organization stoped to exists")))
					}
			}
		}
	}
	
	override def cancelJoinRequest(userId: User.Id, accountTag: Account.Tag) =
		joinRequestDao.delete(userId, accountTag).flatMap { wasNotFound =>
			getMembershipStatusOf(userId, accountTag)
		}

	override def leaveOrganization(userId: User.Id, accountTag: Account.Tag): Transition[TransacMode, MembershipStatusDto] = {
		membershipDao.delete(userId, accountTag).map { x =>
			MembershipStatusDto(None, None, None)
		}
	}
	
	
	override def createOrganization(userId: User.Id, createOrganizationCmd: CreateOrganizationCmd) = transacTransitionExec.inTransaction {
		organizationDao.insert(createOrganizationCmd).flatMap { organization =>
			memberDao.insert(organization.id, createOrganizationCmd.memberName.getOrElse(createOrganizationCmd.accountName)).flatMap { member =>
				membershipDao.insert(organization.id, member.tag, userId, createOrganizationCmd.accountTag).map { membership =>
					organization
				}
			}
		}
	}
}
