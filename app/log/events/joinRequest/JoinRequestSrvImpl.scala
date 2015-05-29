/**
 *
 */
package log.events.joinRequest

import auth.models.User
import javax.inject.Inject
import log.Event
import settings.account.Account
import settings.membership.JoinRequestDao
import settings.membership.MembershipSrv
import utils.TransacMode
import utils.Transition
import settings.membership.MemberDao
import settings.account.AccountSrv
import settings.account.AccountDao
import settings.membership.MembershipDao
import settings.membership.Novice
import scala.util.Try
import scala.util.Failure
import common.OwnershipFailedException
import common.NoPrivilegeException
import common.StoppedToExistException
import settings.account.AccountId
import log.LogSrv
import scala.util.Success
import settings.membership.Member
import play.api.Logger
import settings.membership.Organization
import settings.membership.MemberDto
import utils.TransacTransitionExec
import settings.membership.Role

/**
 * @author Gustavo
 *
 */
class JoinRequestSrvImpl @Inject() (transacTransitionExce: TransacTransitionExec, membershipSrv: MembershipSrv, joinRequestDao: JoinRequestDao, membershipDao: MembershipDao, memberDao: MemberDao, accountDao: AccountDao, logSrv: LogSrv) extends JoinRequestSrv {
	val logger = Logger(classOf[JoinRequestSrvImpl])

	case class CheckBonus(responderMember: MemberDto, joinRequest: JoinRequest, requesterAccount: Account)

	/**Checks if the responder satisfies all the following: the roleRestriction, the join request still exists, the requesters still exists, and the responders belongs to the organization the requesters ask to join to. */
	private def check(responderAccountId: AccountId, requestEventId: Event.Id, roleRestriction: Role => Boolean): Transition[TransacMode, Try[CheckBonus]] = {
		membershipDao.getMemberOf(responderAccountId).flatMap {
			case Some(responderMember) if roleRestriction(responderMember.role) =>
				joinRequestDao.findByEventId(requestEventId).flatMap {
					case None => Transition.failure(new StoppedToExistException("The join request has expired"))
					case Some(joinRequest) => {
						if(joinRequest.organizationId == responderMember.organizationId) {
							accountDao.findById(joinRequest.accountId).flatMap {
								case None => Transition.failure(new StoppedToExistException("The requester account stopped to exist")) //  Should never happen
								case Some(requesterAccount) => Transition.success(CheckBonus(responderMember, joinRequest, requesterAccount))
							}
						} else {
							logger.warn(s"Possible malicius attack of $responderAccountId. He accepted/rejected despite he belons to another organization")
							Transition.failure(new OwnershipFailedException())
						}
					}
				}
			case None => {
				logger.warn(s"Possible malicius attack of $responderAccountId. He accepted/rejected despite he has no organization")
				Transition.failure(new OwnershipFailedException())
			}
			case _ => Transition.failure(new NoPrivilegeException("Your current role lacks the privilege of accepting/rejecting join request"))
		}
	}

	override def accept(userId: User.Id, acceptCmd: JoinRespondCmd): Transition[TransacMode, Try[JoinResponseEventDto]] = transacTransitionExce.inTransaction {
		check(AccountId(userId, acceptCmd.responderAccountTag), acceptCmd.requestEventId, _.canAcceptJoinRequests).flatMap {
			case Failure(exception) => Transition.failure(exception)
			case Success(CheckBonus(responderMember, joinRequest, requesterAccount)) =>
				memberDao.findByHolder(joinRequest.accountId).flatMap {
					case Some(requesterMember) =>
						memberDao.update(requesterMember.copy(role = Novice))
					case None => {
						memberDao.insert(responderMember.organizationId, requesterAccount.name, Novice, Some(joinRequest.accountId))
					}
				}.flatMap { requesterMember =>
					logSrv.newEvent().flatMap { responseEvent =>
						membershipDao.insert(responderMember.organizationId, requesterMember.tag, joinRequest.accountId, acceptCmd.requestEventId, responseEvent._1, responderMember.tag).map { membership =>
							Success(JoinResponseEventDto(responseEvent._1, responseEvent._2, Seq(acceptCmd.requestEventId), responderMember.name, requesterAccount.name, Some(requesterMember.name), None))
						}
					}
				}
		}
	}
	
	override def reject(userId: User.Id, rejectCmd: JoinRespondCmd): Transition[TransacMode, Try[JoinResponseEventDto]] = transacTransitionExce.inTransaction {
		val rejecterAccountId = AccountId(userId, rejectCmd.responderAccountTag)
		check(rejecterAccountId, rejectCmd.requestEventId, _.canRejectJoinRequests).flatMap {
			case Failure(exception) => Transition.failure(exception)
			case Success(CheckBonus(responderMember, joinRequest, requesterAccount)) =>
				logSrv.newEvent().flatMap { rejectionEvent =>
					joinRequestDao.insertRejectionEvent(rejectionEvent._1, joinRequest.accountId, rejectCmd.requestEventId, rejectCmd.rejectionMsg.getOrElse("nothing(?)"), responderMember.tag).map { joinReject =>
						Success(JoinResponseEventDto(rejectionEvent._1, rejectionEvent._2, Seq(rejectCmd.requestEventId), responderMember.name, requesterAccount.name, None, rejectCmd.rejectionMsg))
					}
				}
		}
	}

	override def getEventsAfter(eventId: Option[Event.Id], organizationId:Organization.Id): Transition[TransacMode, Seq[Event]] =
		for {
			joinRequestEvents <- joinRequestDao.getJoinRequestEventsAfter(eventId, organizationId)
			joinRejectionEvents <- joinRequestDao.getJoinRejectionEventsAfter(eventId, organizationId)
			joinCancelEvents <- joinRequestDao.getJoinCancelEventsAfter(eventId, organizationId)
		} yield (joinRequestEvents ++ joinRejectionEvents ++ joinCancelEvents)

}