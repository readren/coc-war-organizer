package log

import utils.Transition
import auth.models.User
import utils.TransacMode
import javax.inject.Inject
import java.sql.Timestamp
import settings.membership.Organization
import settings.membership.MembershipSrv
import utils.TransacTransitionExec
import com.google.inject.ImplementedBy
import settings.account.Account

@ImplementedBy(classOf[LogDaoImpl])
trait LogDao {
	def insert(): Transition[TransacMode, (Event.Id, Event.Instant)]
}

/**
 * @author Gustavo
 *
 */
class LogSrvImpl @Inject() (membershipSrv: MembershipSrv, logDao: LogDao, transacTransitionExec: TransacTransitionExec, eventsSourcesKnower: EventsSourcesKnower) extends LogSrv {

	override def getEventsAfter(userId: User.Id, getEventsAfterCmd: GetEventsAfterCmd): Transition[TransacMode, Seq[Event]] = transacTransitionExec.inTransaction {
		val accountId = Account.Id(userId, getEventsAfterCmd.accountTag)
		membershipSrv.getOrganizationOf(accountId).flatMap {
			case None => Transition.unit(Seq())
			case Some(organizationId) =>
				val seqTrans = for (eventsSource <- eventsSourcesKnower.eventsSources) yield {
					for (event <- eventsSource.getEventsAfter(getEventsAfterCmd.eventId, organizationId)) yield event
				}
				Transition.sequence(seqTrans.toList).map { _.reduce(_ ++ _) }
		}
	}

	override def newEvent(): Transition[TransacMode, (Event.Id, Event.Instant)] = return logDao.insert()

}
