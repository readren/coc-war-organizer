package war.central

import play.api.mvc.Controller
import play.api.mvc.Action
import scala.concurrent.Future
import settings.membership.Icon
import settings.account.Account
import settings.membership.Organization
import common.typeAliases._
import play.api.libs.json.Json

case class UndoCmd(actor: Account.Tag, targetEventId: WarEvent.Id) extends Command
case class UndoEvent(wei: WarEventInfo, undoneEventId: WarEvent.Id) extends WarEventBase(wei)

case class StartPreparationCmd(actor: Account.Tag, enemyClanName: String, enemyClanTag: String) extends Command
case class StartPreparationEvent(wei: WarEventInfo, enemyClanName: String, enemyClanTag: String) extends WarEventBase(wei)

case class AddParticipantCmd(actor: Account.Tag, iconTag: Icon.Tag, basePosition: Position) extends Command
case class AddParticipantEvent(wei: WarEventInfo, iconName: String, basePosition: Position) extends WarEventBase(wei)

case class StartBattleCmd(actor: Account.Tag) extends Command
case class StartBattleEvent(wei: WarEventInfo) extends WarEventBase(wei)

case class GuessInfo(evaluatedParticipantId: ParticipantId, targetPosition: Position, oneStarForecast: Forecast, twoStarsForecast: Forecast, threeStarsForecast: Forecast)
case class AddGuessCmd(actor: Account.Tag, guessInfo: GuessInfo)
case class AddGuessEvent(wei: WarEventInfo, guessInfo: GuessInfo)

case class GetScheduleCmd(actor: Account.Tag) extends Command
case class ScheduleDto(schedule: IndexedSeq[Map[Suwe, ParticipantId]])

case class FightInfo(suwe: Suwe, stars: Stars, destruction: Percentage)
case class AttackInfo(attackerId: ParticipantId, defenderPosition: Position, fightInfo: FightInfo)
case class AddAttackCmd(actor: Account.Tag, attackInfo: AttackInfo) extends Command
case class AddAttackEvent(wei: WarEventInfo, attackInfo: AttackInfo) extends WarEventBase(wei)

case class DefenseInfo(defenderId: ParticipantId, attackerPosition: Position, fightInfo: FightInfo)
case class AddDefenseCmd(actor: Account.Tag, defenseInfo: DefenseInfo) extends Command
case class AddDefenseEvent(wei: WarEventInfo, defenseInfo: DefenseInfo) extends WarEventBase(wei)

case class EndWarCmd(actor: Account.Tag) extends Command
case class EndWarEvent(wei: WarEventInfo) extends WarEventBase(wei)

/**
 * @author Gustavo
 */
class CentralCtrl extends Controller {

	def startPreparation = Action.async(parse.json) { request =>
		Future.successful(Ok(Json.toJson("")))
	}

	def startBattle = Action.async(parse.json) { request =>
		Future.successful(Ok(""))
	}

	def endWar = Action.async(parse.json) { request =>
		Future.successful(Ok(""))
	}

	def addParticipant = Action.async(parse.json) { request =>
		Future.successful(Ok(""))
	}

	def removeParticipant = Action.async(parse.json) { request =>
		Future.successful(Ok("")))
	}

}