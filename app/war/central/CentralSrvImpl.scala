package war.central

import common.typeAliases._
import settings.account.Account
import utils.TransacTransitionExec
import javax.inject.Inject
import common.ParameterlessCmd
import settings.membership.MembershipSrv
import settings.membership.Organization
import utils.Transition
import common.Command
import scala.util.Try
import scala.util.Success
import settings.membership.IconDao
import common.NoPrivilegeException
import scala.util.Failure
import common.OwnershipFailedException
import common.constants._
import settings.membership.Icon
import log.LogDao
import log.LogSrv
import log.OrgaEvent
import log.LogSrvImpl
import scala.concurrent.Future
import com.google.inject.ImplementedBy
import common.NotFoundException
import common.AlreadyExistException
import settings.membership.Icon
import common.CommandThatUpdates

trait WarPhaseInfo {
  val oStartPreparationEventId: Option[WarEvent.Id]
  val oStartBattleEventId: Option[WarEvent.Id]
  val oEndWarEventId: Option[WarEvent.Id]
}
case object InPeace extends WarPhaseInfo {
  override val oStartPreparationEventId = None
  override val oStartBattleEventId = None
  override val oEndWarEventId = None
}
case class InPreparation(startPreparationEventId: WarEvent.Id) extends WarPhaseInfo {
  override val oStartPreparationEventId = Some(startPreparationEventId)
  override val oStartBattleEventId = None
  override val oEndWarEventId = None
}
case class InBattle(startPreparationEventId: WarEvent.Id, startBattleEventId: WarEvent.Id) extends WarPhaseInfo {
  override val oStartPreparationEventId = Some(startPreparationEventId)
  override val oStartBattleEventId = Some(startBattleEventId)
  override val oEndWarEventId = None
}
case class InPostWar(startPreparationEventId: WarEvent.Id, startBattleEventId: WarEvent.Id, endWarEventId: WarEvent.Id) extends WarPhaseInfo {
  override val oStartPreparationEventId = Some(startPreparationEventId)
  override val oStartBattleEventId = Some(startBattleEventId)
  override val oEndWarEventId = Some(endWarEventId)
}

@ImplementedBy(classOf[CentralDaoImpl])
trait CentralDao {

  def insertNewWarEvent(actorIconName: String): TiTac[WarEventInfo]

  /**
   * Gives all the events of the current war of the received organisation after: the received threshold if present, or since the beginning of the war if threshold is absent.
   * The war phase info can be obtained from the received organisation id, nevertheless, it is asked as argument because it is needed and almost all callers already have it at hand.
   */
  def getWarEventsAfter(organizationId: Organization.Id, phase: WarPhaseInfo, oThreshold: Option[WarEvent.Instant]): TiTac[Seq[WarEvent]]

  //  def getClashIdOf(organizationId: Organization.Id): TiTac[Option[WarEvent.Id]]
  //  def getBattleIdOf(clashId: WarEvent.Id): TiTac[Option[WarEvent.Id]]
  //  def getEndIdOf(battleId: WarEvent.Id): TiTac[Option[WarEvent.Id]]

  def getWarPhaseInfo(organizationId: Organization.Id): TiTac[WarPhaseInfo]
  //  def getWarPhaseInfo(organizationId: Organization.Id): TiTac[WarPhaseInfo]

  /**@throws AlreadyExistException if the organisation is already in war */
  def insertStartPreparationEvent(warEventId: WarEvent.Id, orgaEventId: OrgaEvent.Id, organizationId: Organization.Id, enemyClanName: String, enemyClanTag: String): TiTac[Unit]
  def putNextClashMark(previousClashId: WarEvent.Id, newClashId: WarEvent.Id): TiTac[Unit]

  def insertAddParticipantEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, iconTag: Icon.Tag, basePosition: Position): TtTm[Unit]
  def insertStartBattleEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TtTm[Unit]
  def insertAddGuessEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: AddGuessCmd): TtTm[Unit]
  def getSchedule(clashId: WarEvent.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto]
  def insertFightEvent(warEventId: WarEvent.Id, battleId: WarEvent.Id, attackInfo: FightInfo, kind: FightKind): TtTm[Unit]
  def insertEndWarEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: EndWarCmd): TtTm[Unit]

  def insertUndoEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: UndoCmd): TtTm[Unit]

}

/**
 * @author Gustavo
 */
class CentralSrvImpl @Inject() (centralKnower: CentralKnower, centralPerformer: CentralPerformer, centralDao: CentralDao, membershipSrv: MembershipSrv, iconDao: IconDao, logSrv: LogSrv) extends CentralSrv {

  private case class CheckBonus(icon: Icon)

  /**Checks if the account is member of an organisation and its member icon is permitted to perform the received command */
  private def check(accountId: Account.Id, cmd: Command): TtTm[CheckBonus] =
    for (oIcon <- iconDao.findByAccount(accountId)) yield oIcon match {
      case None => Failure(new OwnershipFailedException)
      case Some(icon) => if (icon.role.canDo(cmd, icon)) Success(CheckBonus(icon))
      else Failure(new NoPrivilegeException)
    }

  //  private def getClashIdOf(accountId: Account.Id): TiTac[Option[WarEvent.Id]] = {
  //    membershipSrv.getOrganizationOf(accountId).flatMap {
  //      case None                 => Transition.unit(None)
  //      case Some(organizationId) => centralDao.getClashIdOf(organizationId)
  //    }
  //  }

  override def getWarInitState(accountId: Account.Id, cmd: ParameterlessCmd): TiTac[Seq[WarEvent]] =
    membershipSrv.getOrganizationOf(accountId).flatMap {
      case None                 => Transition.unit(Seq())
      case Some(organizationId) => centralKnower.getWarInitState(organizationId)
    }

  override def getWarEventsAfter(accountId: Account.Id, cmd: GetWarEventsAfterCmd): TiTac[Seq[WarEvent]] =
    membershipSrv.getOrganizationOf(accountId).flatMap {
      case None                 => Transition.unit(Seq())
      case Some(organizationId) => centralKnower.getWarEventsAfter(organizationId, cmd.lastReceivedEventInstant)
    }

  private def perform[E <: WarEventBase, Cmd <: CommandThatUpdates](accountId: Account.Id,
                                                                    cmd: Cmd,
                                                                    perform: (Icon, Cmd) => TtTm[E],
                                                                    announce: E => TiTac[Unit]): TtTm[Seq[WarEvent]] =
    for {
      checkBonus <- check(accountId, cmd)
      event <- perform(checkBonus.icon, cmd)
      _ <- announce(event)
      events <- centralKnower.getWarEventsAfter(checkBonus.icon.organizationId, cmd.lastReceivedEventInstant)
    } yield events

  override def startPreparation(accountId: Account.Id, cmd: StartPreparationCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.startPreparation _, centralKnower.announceStartPreparation _)

  override def addParticipant(accountId: Account.Id, cmd: AddParticipantCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.addParticipant _, centralKnower.announceAddParticipant _)

  override def startBattle(accountId: Account.Id, cmd: StartBattleCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.startBattle _, centralKnower.announceStartBattle _)

  override def addGuess(accountId: Account.Id, cmd: AddGuessCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.addGuess _, centralKnower.announceAddGuess _)

  override def getSchedule(accountId: Account.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto] = {
    ???
  }

  override def addAttack(accountId: Account.Id, cmd: AddAttackCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.addAttack _, centralKnower.announceAddAttack _)

  override def addDefense(accountId: Account.Id, cmd: AddDefenseCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.addDefense _, centralKnower.announceAddDefense _)

  override def endWar(accountId: Account.Id, cmd: EndWarCmd): TtTm[Seq[WarEvent]] =
    perform(accountId, cmd, centralPerformer.endWar _, centralKnower.announceEndWar _)

  override def undo(accountId: Account.Id, cmd: UndoCmd): TtTm[Seq[WarEvent]] = {
    perform(accountId, cmd, centralPerformer.undo _, centralKnower.announceUndo _)
  }
}