package war.central

import scala.annotation.implicitNotFound
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import auth.models.User
import common.typeAliases._
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.Action
import settings.account.Account
import settings.membership.Icon
import utils.TransacTransitionExec
import utils.executionContexts.dbWriteOperations
import utils.executionContexts.default
import settings.membership.IconDto
import log.OrgaEvent
import common.TypicalActions
import common.Command
import centralJsonConverters._
import utils.executionContexts._
import play.api.libs.json.Format

case class GetWarStatusCmd(actor: Account.Tag) extends Command
case class GetWarStatusDto(nonUndoneWarEventsSinceLastInit: Seq[WarEvent]) // Inits occur at preparation start

case class StartPreparationCmd(actor: Account.Tag, enemyClanName: String, enemyClanTag: String) extends Command
case class StartPreparationEvent(wei: WarEventInfo, orgaEventId: OrgaEvent.Id, enemyClanName: String, enemyClanTag: String) extends WarEventBase(wei) {
	override def toJson = startPrepatationEventWrites.writes(this)
}

case class AddParticipantCmd(actor: Account.Tag, iconTag: Icon.Tag, basePosition: Position) extends Command
case class AddParticipantEvent(wei: WarEventInfo, iconName: String, basePosition: Position) extends WarEventBase(wei) {
	override def toJson = addParticipantEventWrites.writes(this)
}

case class StartBattleCmd(actor: Account.Tag) extends Command
case class StartBattleEvent(wei: WarEventInfo) extends WarEventBase(wei) {
	override def toJson = startBattleEventWrites.writes(this)
}

case class GuessInfo(evaluatedParticipantId: ParticipantId, targetPosition: Position, oneStarForecast: Forecast, twoStarsForecast: Forecast, threeStarsForecast: Forecast)
case class AddGuessCmd(actor: Account.Tag, guessInfo: GuessInfo)
case class AddGuessEvent(wei: WarEventInfo, guessInfo: GuessInfo) extends WarEventBase(wei) {
	override def toJson = addGuessEventWrites.writes(this)
}

case class QueueItem(suwe: Suwe, participantId: ParticipantId)
case class GetScheduleCmd(actor: Account.Tag) extends Command
case class ScheduleDto(schedule: IndexedSeq[Seq[QueueItem]])

case class FightInfo(suwe: Suwe, stars: Stars, destruction: Percentage)
case class AttackInfo(attackerId: ParticipantId, defenderPosition: Position, fightInfo: FightInfo)
case class AddAttackCmd(actor: Account.Tag, attackInfo: AttackInfo) extends Command
case class AddAttackEvent(wei: WarEventInfo, attackInfo: AttackInfo) extends WarEventBase(wei) {
	override def toJson = addAttackEventWrites.writes(this)
}

case class DefenseInfo(defenderId: ParticipantId, attackerPosition: Position, fightInfo: FightInfo)
case class AddDefenseCmd(actor: Account.Tag, defenseInfo: DefenseInfo) extends Command
case class AddDefenseEvent(wei: WarEventInfo, defenseInfo: DefenseInfo) extends WarEventBase(wei) {
	override def toJson = addDefenseEventWrites.writes(this)
}

case class EndWarCmd(actor: Account.Tag) extends Command
case class EndWarEvent(wei: WarEventInfo) extends WarEventBase(wei) {
	override def toJson = endWarEventWrites.writes(this)
}

case class UndoCmd(actor: Account.Tag, targetEventId: WarEvent.Id) extends Command
case class UndoEvent(wei: WarEventInfo, undoneEventId: WarEvent.Id) extends WarEventBase(wei) {
	override def toJson = undoEventWrites.writes(this)
}

object centralJsonConverters {

	implicit val getWarStatusCmdReads:Reads[GetWarStatusCmd] = Json.reads[GetWarStatusCmd]
	implicit val getWarStatusDtoWrites:Writes[GetWarStatusDto] = Json.writes[GetWarStatusDto]

	implicit val startPreparationCmdReads:Reads[StartPreparationCmd] = Json.reads[StartPreparationCmd]
	implicit val startPrepatationEventWrites:Writes[StartPreparationEvent] = Json.writes[StartPreparationEvent]

	implicit val addParticipantCmdReads:Reads[AddParticipantCmd] = Json.reads[AddParticipantCmd]
	implicit val addParticipantEventWrites:Writes[AddParticipantEvent] = Json.writes[AddParticipantEvent]

	implicit val startBattleCmdReads:Reads[StartBattleCmd] = Json.reads[StartBattleCmd]
	implicit val startBattleEventWrites:Writes[StartBattleEvent] = Json.writes[StartBattleEvent]

	implicit val addGuessInfoFormat:Format[GuessInfo] = Json.format[GuessInfo]
	implicit val addGuessCmdReads:Reads[AddGuessCmd] = Json.reads[AddGuessCmd]
	implicit val addGuessEventWrites:Writes[AddGuessEvent] = Json.writes[AddGuessEvent]

	implicit val queueItemWrites:Writes[QueueItem] = Json.writes[QueueItem]
	implicit val getScheduleCmdReads:Reads[GetScheduleCmd] = Json.reads[GetScheduleCmd]
	implicit val scheduleDtoWrites:Writes[ScheduleDto] = Json.writes[ScheduleDto]

	implicit val fightInfoFormat:Format[FightInfo] = Json.format[FightInfo]
	implicit val defenseInfoFormat:Format[DefenseInfo] = Json.format[DefenseInfo]
	implicit val addDefenseCmdReads:Reads[AddDefenseCmd] = Json.reads[AddDefenseCmd]
	implicit val addDefenseEventWrites:Writes[AddDefenseEvent] = Json.writes[AddDefenseEvent]

	implicit val attackInfoFormat:Format[AttackInfo] = Json.format[AttackInfo]
	implicit val addAttackCmdReads:Reads[AddAttackCmd] = Json.reads[AddAttackCmd]
	implicit val addAttackEventWrites:Writes[AddAttackEvent] = Json.writes[AddAttackEvent]

	implicit val endWarCmdReads:Reads[EndWarCmd] = Json.reads[EndWarCmd]
	implicit val endWarEventWrites:Writes[EndWarEvent] = Json.writes[EndWarEvent]

	implicit val undoCmdReads:Reads[UndoCmd] = Json.reads[UndoCmd]
	implicit val undoEventWrites:Writes[UndoEvent] = Json.writes[UndoEvent]
}

/**
 * @author Gustavo
 */
class CentralCtrl @Inject() (centralSrv: CentralSrv, val tte: TransacTransitionExec, implicit val env: Environment[User, JWTAuthenticator]) extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {

	def getWarStatus:Action[JsValue] = typicalAction(simpleDbLookups, centralSrv.getWarStatus _)
	def startPreparation:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.startPreparation _)
	def addParticipant:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.addParticipant _)
	def startBattle:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.startBattle _)
	def getSchedule:Action[JsValue] = typicalAction(simpleDbLookups, centralSrv.getSchedule _)
	def addGuess:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.startBattle _)
	def addAttack:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.addAttack _)
	def addDefense:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.addDefense _)
	def endWar:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.endWar _)
	def undo:Action[JsValue] = typicalAction(dbWriteOperations, centralSrv.undo _)

}