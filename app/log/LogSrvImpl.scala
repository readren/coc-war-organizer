package log

import utils.Transition
import auth.models.User
import utils.TransacMode
import javax.inject.Inject
import java.sql.Timestamp
import settings.membership.Organization
import settings.membership.MembershipSrv
import utils.TransacTransitionExec
import utils.TransactionalTransition._
import com.google.inject.ImplementedBy
import settings.account.Account
import common.typeAliases._
import common.constants._
import settings.membership.MembershipDao


@ImplementedBy(classOf[LogDaoImpl])
trait LogDao {
	def insert(): TiTac[(OrgaEvent.Id, OrgaEvent.Instant)]
}

/**
 * @author Gustavo
 *
 */
class LogSrvImpl @Inject() (membershipSrv: MembershipSrv, membershipDao: MembershipDao, logDao: LogDao, eventsSourcesKnower: EventsSourcesKnower) extends LogSrv {

	def getLogInitState(accountId: Account.Id, getLogInitStateCmd: GetLogInitStateCmd): TiTac[GetLogInitStateDto] = {
		membershipSrv.getOrganizationOf(accountId).flatMap {
			case None => Transition.unit(GetLogInitStateDto(Seq(), Seq()))
			case Some(organizationId) =>
				val seqTrans =
					for (eventsSource <- eventsSourcesKnower.eventsSources) yield {
						for (event <- eventsSource.getEventsAfter(Right(48* 60 * 60), organizationId)) yield event
					}

				for {
					events <- Transition.sequence(seqTrans.toList).map { _.reduce(_ ++ _) }
					members <- membershipDao.getBodyOfMembers(organizationId)
				} yield GetLogInitStateDto(events, members)
		}
	}

	override def getEventsAfter(accountId: Account.Id, getEventsAfterCmd: GetEventsAfterCmd): TiTac[Seq[OrgaEvent]] = TransacTransitionExec.inTransaction {
		membershipSrv.getOrganizationOf(accountId).flatMap {
			case None => Transition.unit(Seq())
			case Some(organizationId) =>
				val seqTrans =
					for (eventsSource <- eventsSourcesKnower.eventsSources) yield {
						for (event <- eventsSource.getEventsAfter(Left(getEventsAfterCmd.eventInstant.minusSeconds(TIME_SHIFT_MARGIN)), organizationId)) yield event
					}
				Transition.sequence(seqTrans.toList).map { _.reduce(_ ++ _) }
		}
	}

	override def newEvent(): TiTac[(OrgaEvent.Id, OrgaEvent.Instant)] = return logDao.insert()

}
