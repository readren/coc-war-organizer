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
	def insert(): Transition[TransacMode, (OrgaEvent.Id, OrgaEvent.Instant)]
}

/**
 * @author Gustavo
 *
 */
object LogSrvImpl {
	val TIME_SHIFT_MARGIN = 5
}
class LogSrvImpl @Inject() (membershipSrv: MembershipSrv, logDao: LogDao, transacTransitionExec: TransacTransitionExec, eventsSourcesKnower: EventsSourcesKnower) extends LogSrv {

	override def getEventsAfter(accountId:Account.Id, getEventsAfterCmd: GetEventsAfterCmd): Transition[TransacMode, Seq[OrgaEvent]] = transacTransitionExec.inTransaction {
		membershipSrv.getOrganizationOf(accountId).flatMap {
			case None => Transition.unit(Seq())
			case Some(organizationId) =>
				val seqTrans = for (eventsSource <- eventsSourcesKnower.eventsSources) yield {
					val threshold: Either[OrgaEvent.Instant, Int] = getEventsAfterCmd.eventInstant match {
						case Some(eventInstant) => Left(eventInstant.minusSeconds(LogSrvImpl.TIME_SHIFT_MARGIN))
						case None => Right(300*60) // 300 minutes in seconds
					}
					for (event <- eventsSource.getEventsAfter(threshold, organizationId)) yield event
				}
				Transition.sequence(seqTrans.toList).map { _.reduce(_ ++ _) }
		}
	}

	override def newEvent(): Transition[TransacMode, (OrgaEvent.Id, OrgaEvent.Instant)] = return logDao.insert()

}
