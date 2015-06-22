package war.central

import common.typeAliases._
import settings.account.Account
import utils.TransacTransitionExec
import javax.inject.Inject

trait CentralDao {

}

/**
 * @author Gustavo
 */
class CentralSrvImpl @Inject() (tte: TransacTransitionExec) extends CentralSrv {

  def getWarStatus(accountId: Account.Id, cmd: GetWarStatusCmd): TiTac[GetWarStatusDto] = {
	  ???
	}
  
	override def startPreparation(accountId: Account.Id, cmd: StartPreparationCmd): TiTac[StartPreparationEvent] =
		tte.inTransaction {

			???
		}

  def getCurrentStatus(accountId: Account.Id, cmd: GetWarStatusCmd): TiTac[GetWarStatusDto] = {
	  ???
	}

	def addParticipant(accountId: Account.Id, cmd: AddParticipantCmd): TiTac[AddParticipantEvent] = {
		???
	}

	def startBattle(accountId: Account.Id, cmd: StartBattleCmd): TiTac[StartBattleEvent] = {
		???
	}

	def addGuess(accountId: Account.Id, cmd: AddGuessCmd): TiTac[AddGuessEvent] = {
		???
	}

  def getSchedule(accountId: Account.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto] = {
	  ???
	}

	def addAttack(accountId: Account.Id, cmd: AddAttackCmd): TiTac[AddAttackEvent] = {
		???
	}

	def addDefense(accountId: Account.Id, cmd: AddDefenseCmd): TiTac[AddDefenseEvent] = {
		???
	}

	def endWar(accountId: Account.Id, cmd: EndWarCmd): TiTac[EndWarEvent] = {
		???
	}

	def undo(accountId: Account.Id, cmd: UndoCmd): TiTac[UndoEvent] = {
		???
	}

}