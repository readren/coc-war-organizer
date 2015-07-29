package war.central

import settings.membership.Organization
import common.typeAliases._
import javax.inject.Inject
import common.constants._
import com.google.inject.ImplementedBy
import utils.Transition
import common.NotFoundException
import scala.util.Failure
import scala.util.Success

/**
 * Query part of the Command Query Responsibility Segregation pattern for the war central module.
 * For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
 * @author Gustavo
 */
class CentralKnower @Inject() (centralDao: CentralDao) {

  def getWarPhaseInfo(organizationId: Organization.Id): TiTac[WarPhaseInfo] =
    centralDao.getWarPhaseInfo(organizationId)

  def getWarInitState(organizationId: Organization.Id): TiTac[Seq[WarEvent]] =
    for {
      phase <- getWarPhaseInfo(organizationId)
      events <- centralDao.getWarEventsAfter(organizationId, phase, None)
    } yield events

  def getWarEventsAfter(organizationId: Organization.Id, lastReceivedEventInstant: WarEvent.Instant): TiTac[Seq[WarEvent]] =
    for {
      phase <- getWarPhaseInfo(organizationId)
      events <- getWarEventsAfter(organizationId, phase, lastReceivedEventInstant)
    } yield events

  def getWarEventsAfter(organizationId: Organization.Id, phase: WarPhaseInfo, lastReceivedEventInstant: WarEvent.Instant): TiTac[Seq[WarEvent]] = {
    centralDao.getWarEventsAfter(organizationId, phase, Some(lastReceivedEventInstant.minusSeconds(TIME_SHIFT_MARGIN)))
  }
    
  def getWarEvent(organizationId: Organization.Id, eventId:WarEvent.Id, eventInstant:WarEvent.Instant): TtTm[WarEvent] = {
    for{
      events <- getWarEventsAfter(organizationId, eventInstant)
    } yield {
      events.find { e => e.id == eventId } match {
        case None => Failure(new NotFoundException)
        case Some(fe) => Success(fe)
      }
    }
  }

  def announceStartPreparation(ape: StartPreparationEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary. 
  def announceAddParticipant(ape: AddParticipantEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary. 
  def announceStartBattle(sbe: StartBattleEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddGuess(age: AddGuessEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddAttack(aae: AddAttackEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddDefense(aae: AddDefenseEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceEndWar(ewe: EndWarEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.

  def announceUndo(ewe: UndoEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
}