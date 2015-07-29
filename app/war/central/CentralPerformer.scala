package war.central

import settings.membership.Icon
import common.typeAliases._
import common.AlreadyExistException
import utils.Transition
import scala.util.Success
import javax.inject.Inject
import scala.util.Try
import utils.TransacTransitionExec
import log.LogSrv
import scala.util.Failure
import common.NotFoundException

/**
 * Command part of the Command Query Responsibility Segregation pattern for the war central module.
 *
 * @author Gustavo
 */
class CentralPerformer @Inject() (centralKnower: CentralKnower, centralDao: CentralDao, logSrv: LogSrv) {

  def startPreparation(icon: Icon, cmd: StartPreparationCmd): TtTm[StartPreparationEvent] = {
    def insert(actorIcon: Icon): TiTac[StartPreparationEvent] = {
      for {
        newClashEvent <- centralDao.insertNewWarEvent(actorIcon.name)
        orgaEvent <- logSrv.newEvent()
        _ <- centralDao.insertStartPreparationEvent(newClashEvent.id, orgaEvent._1, actorIcon.organizationId, cmd.enemyClanName, cmd.enemyClanTag)
      } yield StartPreparationEvent(WarEventInfo(newClashEvent.id, newClashEvent.instant, actorIcon.name), orgaEvent._1, cmd.enemyClanName, cmd.enemyClanTag)
    }

    TransacTransitionExec.inTransaction {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap {
        case _: InPreparation => Transition.failure(new AlreadyExistException("This organisation is already in a preparation phase."))
        case _: InBattle      => Transition.failure(new AlreadyExistException("Can't start preparation during the battle phase."))
        case InPeace          => insert(icon).map(Success(_))
        case InPostWar(previousClashId, _, _) => for {
          spe <- insert(icon)
          _ <- centralDao.putNextClashMark(previousClashId, spe.wei.id)
        } yield Success(spe)
      }
    }
  }

  def addParticipant(icon: Icon, cmd: AddParticipantCmd): TtTm[AddParticipantEvent] =
    TransacTransitionExec.inTransaction {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap { phase =>
        phase.oStartPreparationEventId match {
          case None => Transition.unit(Failure(new NotFoundException("This organisation is not in war")))
          case Some(startPreparationEventId) =>
            for {
              warEvent <- Transition.toSuccessfulTransitionTry(centralDao.insertNewWarEvent(icon.name))
              _ <- centralDao.insertAddParticipantEvent(warEvent.id, startPreparationEventId, icon.tag, cmd.basePosition)
            } yield AddParticipantEvent(warEvent, icon.name, cmd.basePosition)
        }
      }
    }

  def startBattle(icon: Icon, cmd: StartBattleCmd): TtTm[StartBattleEvent] = {
    ???
  }

  def addGuess(icon: Icon, cmd: AddGuessCmd): TtTm[AddGuessEvent] = {
    ???
  }

  def addAttack(icon: Icon, cmd: AddAttackCmd): TtTm[AddAttackEvent] = {
    ???
  }

  def addDefense(icon: Icon, cmd: AddDefenseCmd): TtTm[AddDefenseEvent] = {
    ???
  }

  def endWar(icon: Icon, cmd: EndWarCmd): TtTm[EndWarEvent] = {
    ???
  }

  def undo(icon: Icon, cmd: UndoCmd): TtTm[UndoEvent] =
    for {
      eventToUndo <- centralKnower.getWarEvent(icon.organizationId, cmd.targetEventId, cmd.targetEventInstant)
      undoEvent <- eventToUndo.undoOp(this)(eventToUndo, icon)
    } yield undoEvent

  def undoStartPreparation(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoAddParticipant(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoStartBattle(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoAddGuess(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoAddAttack(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoAddDefense(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoEndWar(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }

  def undoUndo(eventToUndo: WarEvent, icon: Icon):TtTm[UndoEvent] = {
    ???
  }


}