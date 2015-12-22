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
import utils.TransactionalTransition._


@ImplementedBy(classOf[CentralKnowerRepoImpl])
trait CentralKnowerRepo {

  /**
   * Gives all the events of the current war of the received organisation after: the received threshold if present, or since the beginning of the war if threshold is absent.
   * The war phase info can be obtained from the received organisation id, nevertheless, it is asked as argument because it is needed and almost all callers already have it at hand.
   */
  def getWarEventsAfter(organizationId: Organization.Id, phase: WarPhaseInfo, oThreshold: Option[WarEvent.Instant]): TiTac[Seq[WarEvent]]

  //  def getClashIdOf(organizationId: Organization.Id): TiTac[Option[WarEvent.Id]]
  //  def getBattleIdOf(clashId: WarEvent.Id): TiTac[Option[WarEvent.Id]]
  //  def getEndIdOf(battleId: WarEvent.Id): TiTac[Option[WarEvent.Id]]

  def getWarPhaseInfo(organizationId: Organization.Id): TiTac[WarPhaseInfo]

  def getPreviousClashId(organizationId:Organization.Id, currentClashId: WarEvent.Id):TiTac[Option[WarEvent.Id]]

}



/**
 * Query part of the Command Query Responsibility Segregation pattern for the war central module.
 * For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
 * @author Gustavo
 */
class CentralKnower @Inject() (centralDao: CentralKnowerRepo) {

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
    
  /**Looks for the WarEvent whose id and instant are the received ones and belongs to the received organisation. If none is found this transition produces a Failure (TransitionResult.product.isFailure==true).*/
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
  
  /**Gives the clash id of the war immediately before the received clash id that belongs to the received organisation. */
  def getPreviousClashId(organizationId:Organization.Id, currentClashId:WarEvent.Id): TiTac[Option[WarEvent.Id]] =
    centralDao.getPreviousClashId(organizationId, currentClashId)
  

  def announceStartPreparation(ape: StartPreparationEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary. 
  def announceAddParticipant(ape: AddParticipantEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary. 
  def announceStartBattle(sbe: StartBattleEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddGuess(age: AddGuessEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddAttack(aae: AddAttackEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceAddDefense(aae: AddDefenseEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
  def announceEndWar(ewe: EndWarEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.

  def announceUndo(ewe: UndoEvent): TiTac[Unit] = Transition.unit(()) //TODO For now, no caching is implemented. Everything is asked to the DB. So, there is no need to receive change events. This is temporary.
}