package war

import settings.membership.Organization
import settings.membership.Icon
import common.typeAliases._
import war.central.WarEvent

/**
 * @author Gustavo
 */

object Clash {
	type Id = WarEvent.Id
}
case class Clash(preparationStartEvent: WarEvent, battleStartEvent: WarEvent, warEndEvent: WarEvent,
		organizationId: Organization.Id, enemyClanName: String, enemyClanTag: String) {
	def id: Clash.Id = preparationStartEvent.id
}

object Participant {
	type Id = Long
}
case class Participant(addEvent: WarEvent, removeEvent: WarEvent, clashId: Clash.Id, memberTag: Icon.Tag, basePosition: Position) {
	def id = addEvent
}

object Reservation {
	type Id = WarEvent.Id
}
case class Reservation(addEvent: WarEvent, removeEvent: WarEvent, participantId: Participant.Id, targetPosition: Position) {
	def id: Reservation.Id = addEvent.id
}

object Guess {
	type Id = WarEvent.Id
}
case class Guess(addEvent: WarEvent, removeEvent: WarEvent, clashId: Clash.Id,
		guesserMemberTag: Icon.Tag, judgedParticipantId: Participant.Id, targetPosition: Position,
		oneStarForecast: Forecast, twoStarsForecast: Forecast, threeStarsForecast: Forecast) {
	def id: Guess.Id = addEvent.id
}

object Fight {
	type Id = WarEvent.Id
	type Kind = Boolean
	val ATTACK: Kind = true
	val DEFFENSE: Kind = false
}
case class Fight(addEvent: WarEvent, removeEvent: WarEvent, clashId: Clash.Id, participantId: Participant.Id,
		opponentPosition: Position, kind: Fight.Kind, suwe: Suwe, stars: Short, destruction: Percentage) {
	def id: Fight.Id = addEvent.id
}

object Plan {
	type Id = WarEvent.Id
}
case class Plan(addEvent: WarEvent, removeEvent: WarEvent, clashId: Clash.Id, participantId: Participant.Id,
		targetPosition: Position, attackNumber: Short) {
	def id: Plan.Id = addEvent.id
}

object Queue {
	type Id = WarEvent.Id
}
case class Queue(addEvent: WarEvent, removeEvent: WarEvent, clashId: Clash.Id, targetPosition: Position, scheddule: Array[Suwe])
