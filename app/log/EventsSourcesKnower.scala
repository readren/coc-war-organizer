/**
 *
 */
package log

import auth.models.User
import utils.Transition
import utils.TransacMode
import javax.inject.Inject
import log.events.joinRequest.JoinRequestSrv
import settings.account.Account
import settings.account.Account
import settings.membership.MembershipSrv
import settings.membership.Organization

/**
 * @author Gustavo
 *
 */

trait EventsSource[+E] {
	def getEventsAfter(eventId: Option[Event.Id], organizationId:Organization.Id): Transition[TransacMode, Seq[E]]
}

class EventsSourcesKnower @Inject() (joinRequestSrv: JoinRequestSrv, membershipSrv: MembershipSrv) {
	val eventsSources:Seq[EventsSource[Event]] = Seq(joinRequestSrv, membershipSrv)
}