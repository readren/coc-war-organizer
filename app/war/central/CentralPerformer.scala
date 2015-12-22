package war.central

import settings.membership.Icon
import common.typeAliases._
import common.AlreadyExistException
import utils.Transition
import scala.util.Success
import javax.inject.Inject
import scala.util.Try
import utils.TransacTransitionExec
import utils.TransactionalTransition._
import log.LogSrv
import scala.util.Failure
import common.NotFoundException
import com.google.inject.ImplementedBy
import log.OrgaEvent
import settings.membership.Organization
import utils.TransacMode
import common.OutdatedStateException
import utils.TransitionTry
import common.NoPrivilegeException

@ImplementedBy(classOf[CentralPerformerRepoImpl])
trait CentralPerformerRepo {

  def insertNewWarEvent(actorIconName: String): TiTac[WarEventInfo]

  /**@throws AlreadyExistException if the organisation is already in war */
  def insertStartPreparationEvent(warEventId: WarEvent.Id, orgaEventId: OrgaEvent.Id, organizationId: Organization.Id, enemyClanName: String, enemyClanTag: String): TiTac[Unit]
  def putNextClashMark(previousClashId: WarEvent.Id, newClashId: Option[WarEvent.Id]): TiTac[Unit]
  def insertUndoStartPreparationEvent(undoEventId: WarEvent.Id, undoneEventId: WarEvent.Id): TtTm[Unit]

  def insertAddParticipantEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, iconTag: Icon.Tag, basePosition: Position): TtTm[Unit]
  def insertUndoAddParticipantEvent(undoEventId: WarEvent.Id, undoneEventId: WarEvent.Id): TtTm[Unit]

  def insertStartBattleEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TtTm[Unit]
  def putCurrentBattleMark(clashId: WarEvent.Id, currentBattleId: WarEvent.Id): TiTac[Unit]

  def insertAddGuessEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: AddGuessCmd): TtTm[Unit]
  def getSchedule(clashId: WarEvent.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto]
  def insertFightEvent(warEventId: WarEvent.Id, battleId: WarEvent.Id, attackInfo: FightInfo, kind: FightKind): TtTm[Unit]
  def insertEndWarEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TiTac[Unit]

}

/**
 * Command part of the Command Query Responsibility Segregation pattern for the war central module.
 *
 * @author Gustavo
 */
class CentralPerformer @Inject() (centralKnower: CentralKnower, centralPerformerRepo: CentralPerformerRepo, logSrv: LogSrv) {

  def startPreparation(icon: Icon, cmd: StartPreparationCmd): TtTm[StartPreparationEvent] = {
    def insert(actorIcon: Icon): TiTac[StartPreparationEvent] = {
      for {
        newClashEvent <- centralPerformerRepo.insertNewWarEvent(actorIcon.name)
        orgaEvent <- logSrv.newEvent()
        _ <- centralPerformerRepo.insertStartPreparationEvent(newClashEvent.id, orgaEvent._1, actorIcon.organizationId, cmd.enemyClanName, cmd.enemyClanTag)
      } yield StartPreparationEvent(WarEventInfo(newClashEvent.id, newClashEvent.instant, actorIcon.name), orgaEvent._1, cmd.enemyClanName, cmd.enemyClanTag)
    }

    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap {
        case _: InPreparation => Transition.failure(new AlreadyExistException("This organisation is already in the preparation phase."))
        case _: InBattle      => Transition.failure(new AlreadyExistException("Can't start preparation during the battle phase."))
        case InPeace          => insert(icon).map(Success(_))
        case InPostWar(previousClashId, _, _) => for {
          spe <- insert(icon)
          _ <- centralPerformerRepo.putNextClashMark(previousClashId, Some(spe.wei.id))
        } yield Success(spe)
      }
    }
  }

  def undoStartPreparation(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap { phase =>
        phase.oStartPreparationEventId match {
          case None => Transition.failure(new AlreadyExistException("Can't start preparation during the battle phase."))
          case Some(currentClashId) if currentClashId == eventToUndo.id =>
            for {
              wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
              _ <- centralPerformerRepo.insertUndoStartPreparationEvent(wei.id, currentClashId)
              oPreviousClashId <- centralKnower.getPreviousClashId(icon.organizationId, currentClashId)
              _ <- oPreviousClashId match {
                case None                  => Transition.unit[TransacMode, Unit](())
                case Some(previousClashId) => centralPerformerRepo.putNextClashMark(previousClashId, None)
              }
            } yield UndoEvent(wei, currentClashId)
          case _ => Transition.failure(new OutdatedStateException())
        }
      }
    }

  def addParticipant(icon: Icon, cmd: AddParticipantCmd): TtTm[AddParticipantEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap { phase =>
        phase.oStartPreparationEventId match {
          case None => Transition.failure(new NotFoundException("This organisation is not in war"))
          case Some(clashId) =>
            for {
              wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
              _ <- centralPerformerRepo.insertAddParticipantEvent(wei.id, clashId, icon.tag, cmd.basePosition)
            } yield AddParticipantEvent(wei, icon.name, cmd.basePosition)
        }
      }
    }

  def undoAddParticipant(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId) flatMap { phase =>
        phase.oStartPreparationEventId match {
          case None => Transition.failure(new NotFoundException("The preparation phase was not started yet"))
          case Some(clashId) =>
            for {
              wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
              _ <- centralPerformerRepo.insertUndoAddParticipantEvent(wei.id, eventToUndo.id)
            } yield UndoEvent(wei, eventToUndo.id)
        }
      }
    }

  def startBattle(icon: Icon, cmd: StartBattleCmd): TtTm[StartBattleEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap {
        case _: InBattle  => Transition.failure(new AlreadyExistException("This organisation is already in the battle phase."))
        case _: InPostWar => Transition.failure(new AlreadyExistException("Can't start battle during the post war phase. You should start the preparation phase first."))
        case InPeace      => Transition.failure(new NotFoundException("Can't start battle during the peace phase. You should start the preparation phase first."))
        case InPreparation(clashId) =>
          for {
            wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
            _ <- centralPerformerRepo.insertStartBattleEvent(wei.id, clashId)
            _ <- centralPerformerRepo.putCurrentBattleMark(clashId, wei.id)
          } yield StartBattleEvent(wei)
      }
    }

  def addGuess(icon: Icon, cmd: AddGuessCmd): TtTm[AddGuessEvent] = {
    ???
  }

  def addAttack(icon: Icon, cmd: AddAttackCmd): TtTm[AddAttackEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap { phase =>
        phase.oStartBattleEventId match {
          case None => Transition.unit(Failure(new NotFoundException("This organisation is not in the battle phase")))
          case Some(battleId) =>
            for {
              wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
              _ <- centralPerformerRepo.insertFightEvent(wei.id, battleId, cmd.attackInfo, ATTACK)
            } yield AddAttackEvent(wei, cmd.attackInfo)
        }
      }
    }

  def addDefense(icon: Icon, cmd: AddDefenseCmd): TtTm[AddDefenseEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap { phase =>
        phase.oStartBattleEventId match {
          case None => Transition.unit(Failure(new NotFoundException("This organisation is not in the battle phase")))
          case Some(battleId) =>
            for {
              wei <- Transition.toSuccessfulTransitionTry(centralPerformerRepo.insertNewWarEvent(icon.name))
              _ <- centralPerformerRepo.insertFightEvent(wei.id, battleId, cmd.defenseInfo, DEFENSE)
            } yield AddDefenseEvent(wei, cmd.defenseInfo)
        }
      }
    }

  def endWar(icon: Icon, cmd: EndWarCmd): TtTm[EndWarEvent] =
    TransacTransitionExec.inTransactionTry {
      centralKnower.getWarPhaseInfo(icon.organizationId).flatMap {
        case _: InPostWar     => Transition.failure(new AlreadyExistException("This organisation is already in the post war phase."))
        case InPeace          => Transition.failure(new NotFoundException("Can't end a war during the peace phase."))
        case _: InPreparation => Transition.failure(new NotFoundException("Can't end the war during the preparation phase. You should star the battle first."))
        case InBattle(clashId, battleId) =>
          for {
            wei <- centralPerformerRepo.insertNewWarEvent(icon.name)
            _ <- centralPerformerRepo.insertEndWarEvent(wei.id, clashId)
            _ <- centralPerformerRepo.putCurrentBattleMark(clashId, wei.id)
          } yield Success(EndWarEvent(wei))
      }
    }

  def undo(icon: Icon, cmd: UndoCmd): TtTm[UndoEvent] =
    for {
      eventToUndo <- centralKnower.getWarEvent(icon.organizationId, cmd.targetEventId, cmd.targetEventInstant)
      undoEvent <- if (icon.role.canUndo(eventToUndo, icon)) eventToUndo.undoOp(this)(eventToUndo, icon)
      else TransitionTry.fail[TransacMode, UndoEvent](new NoPrivilegeException)
    } yield undoEvent

  def undoStartBattle(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

  def undoAddGuess(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

  def undoAddAttack(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

  def undoAddDefense(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

  def undoEndWar(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

  def undoUndo(eventToUndo: WarEvent, icon: Icon): TtTm[UndoEvent] = {
    ???
  }

}