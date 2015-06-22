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

	implicit val getWarStatusCmdReads = Json.reads[GetWarStatusCmd]
	implicit val getWarStatusDtoWrites = Json.writes[GetWarStatusDto]

	implicit val startPreparationCmdReads = Json.reads[StartPreparationCmd]
	implicit val startPrepatationEventWrites = Json.writes[StartPreparationEvent]

	implicit val addParticipantCmdReads = Json.reads[AddParticipantCmd]
	implicit val addParticipantEventWrites = Json.writes[AddParticipantEvent]

	implicit val startBattleCmdReads = Json.reads[StartBattleCmd]
	implicit val startBattleEventWrites = Json.writes[StartBattleEvent]

	implicit val addGuessInfoFormat = Json.format[GuessInfo]
	implicit val addGuessCmdReads = Json.reads[AddGuessCmd]
	implicit val addGuessEventWrites = Json.writes[AddGuessEvent]

	implicit val queueItemWrites = Json.writes[QueueItem]
	implicit val getScheduleCmdReads = Json.reads[GetScheduleCmd]
	implicit val scheduleDtoWrites = Json.writes[ScheduleDto]

	implicit val fightInfoFormat = Json.format[FightInfo]
	implicit val defenseInfoFormat = Json.format[DefenseInfo]
	implicit val addDefenseCmdReads = Json.reads[AddDefenseCmd]
	implicit val addDefenseEventWrites = Json.writes[AddDefenseEvent]

	implicit val attackInfoFormat = Json.format[AttackInfo]
	implicit val addAttackCmdReads = Json.reads[AddAttackCmd]
	implicit val addAttackEventWrites = Json.writes[AddAttackEvent]

	implicit val endWarCmdReads = Json.reads[EndWarCmd]
	implicit val endWarEventWrites = Json.writes[EndWarEvent]

	implicit val undoCmdReads = Json.reads[UndoCmd]
	implicit val undoEventWrites = Json.writes[UndoEvent]
}

/**
 * @author Gustavo
 */
class CentralCtrl @Inject() (centralSrv: CentralSrv, val tte: TransacTransitionExec, implicit val env: Environment[User, JWTAuthenticator]) extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {

	def getWarStatus = typicalAction(simpleDbLookups, centralSrv.getWarStatus _)
	def startPreparation = typicalAction(dbWriteOperations, centralSrv.startPreparation _)
	def addParticipant = typicalAction(dbWriteOperations, centralSrv.addParticipant _)
	def startBattle = typicalAction(dbWriteOperations, centralSrv.startBattle _)
	def getSchedule = typicalAction(simpleDbLookups, centralSrv.getSchedule _)
	def addGuess = typicalAction(dbWriteOperations, centralSrv.startBattle _)
	def addAttack = typicalAction(dbWriteOperations, centralSrv.addAttack _)
	def addDefense = typicalAction(dbWriteOperations, centralSrv.addDefense _)
	def endWar = typicalAction(dbWriteOperations, centralSrv.endWar _)
	def undo = typicalAction(dbWriteOperations, centralSrv.undo _)

}