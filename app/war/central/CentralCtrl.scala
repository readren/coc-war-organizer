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
import utils.TransactionalTransition._
import utils.executionContexts.dbWriteOperations
import utils.executionContexts.default
import settings.membership.IconDto
import log.OrgaEvent
import common.TypicalActions
import common.Command
import centralJsonConverters._
import utils.executionContexts._
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.reflect._
import common.ParameterlessCmd
import common.CommandThatUpdates



case class GetWarEventsAfterCmd(actor: Account.Tag, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates

case class StartPreparationCmd(actor: Account.Tag, enemyClanName: String, enemyClanTag: String, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class StartPreparationEvent(wei: WarEventInfo, orgaEventId: OrgaEvent.Id, enemyClanName: String, enemyClanTag: String) extends WarEventBase(wei) {
	override def toJson = startPrepatationEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoStartPreparation _ 
}

case class AddParticipantCmd(actor: Account.Tag, iconTag: Icon.Tag, basePosition: Position, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class AddParticipantEvent(wei: WarEventInfo, iconName: String, basePosition: Position) extends WarEventBase(wei) {
	override def toJson = addParticipantEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoAddParticipant _ 
}

case class StartBattleCmd(actor: Account.Tag, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class StartBattleEvent(wei: WarEventInfo) extends WarEventBase(wei) {
	override def toJson = startBattleEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoStartBattle _ 
}

case class GuessInfo(evaluatedParticipantId: ParticipantId, targetPosition: Position, oneStarForecast: Forecast, twoStarsForecast: Forecast, threeStarsForecast: Forecast)
case class AddGuessCmd(actor: Account.Tag, guessInfo: GuessInfo, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class AddGuessEvent(wei: WarEventInfo, guessInfo: GuessInfo) extends WarEventBase(wei) {
	override def toJson = addGuessEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoAddGuess _ 
}

case class QueueItem(suwe: Suwe, participantId: ParticipantId)
case class GetScheduleCmd(actor: Account.Tag) extends Command
case class ScheduleDto(schedule: IndexedSeq[Seq[QueueItem]])

case class FightInfo(participantPosition: Position, opponentPosition: Position, suwe: Suwe, stars: Stars, destruction: Percentage)
case class AddAttackCmd(actor: Account.Tag, attackInfo: FightInfo, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class AddAttackEvent(wei: WarEventInfo, attackInfo: FightInfo) extends WarEventBase(wei) {
	override def toJson = addAttackEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoAddAttack _ 
}

case class AddDefenseCmd(actor: Account.Tag, defenseInfo: FightInfo, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class AddDefenseEvent(wei: WarEventInfo, defenseInfo: FightInfo) extends WarEventBase(wei) {
	override def toJson = addDefenseEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoAddDefense _ 
}

case class EndWarCmd(actor: Account.Tag, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class EndWarEvent(wei: WarEventInfo) extends WarEventBase(wei) {
	override def toJson = endWarEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoEndWar _ 
}

case class UndoCmd(actor: Account.Tag, targetEventId: WarEvent.Id, targetEventInstant:WarEvent.Instant, lastReceivedEventInstant:WarEvent.Instant) extends CommandThatUpdates
case class UndoEvent(wei: WarEventInfo, undoneEventId: WarEvent.Id) extends WarEventBase(wei) {
	override def toJson = undoEventWrites.writes(this)
  override def undoOp(cp:CentralPerformer) = cp.undoUndo _ 
}

object centralJsonConverters {
	def typeTagged[E](writes: Writes[E])(implicit ct:ClassTag[E]): Writes[E] = writes.transform { _.as[JsObject] + ("type" -> JsString(ct.runtimeClass.getSimpleName.replace("Event","")))}
  
  implicit val getWarEventsAfterCmdReads:Reads[GetWarEventsAfterCmd] = Json.reads[GetWarEventsAfterCmd]
	
	implicit val startPreparationCmdReads:Reads[StartPreparationCmd] = Json.reads[StartPreparationCmd]
	implicit val startPrepatationEventWrites:Writes[StartPreparationEvent] = typeTagged(Json.writes[StartPreparationEvent])

	implicit val addParticipantCmdReads:Reads[AddParticipantCmd] = Json.reads[AddParticipantCmd]
	implicit val addParticipantEventWrites:Writes[AddParticipantEvent] = typeTagged(Json.writes[AddParticipantEvent])

	implicit val startBattleCmdReads:Reads[StartBattleCmd] = Json.reads[StartBattleCmd]
	implicit val startBattleEventWrites:Writes[StartBattleEvent] = typeTagged(Json.writes[StartBattleEvent])

	implicit val addGuessInfoFormat:Format[GuessInfo] = Json.format[GuessInfo]
	implicit val addGuessCmdReads:Reads[AddGuessCmd] = Json.reads[AddGuessCmd]
	implicit val addGuessEventWrites:Writes[AddGuessEvent] = typeTagged(Json.writes[AddGuessEvent])

	implicit val queueItemWrites:Writes[QueueItem] = Json.writes[QueueItem]
	implicit val getScheduleCmdReads:Reads[GetScheduleCmd] = Json.reads[GetScheduleCmd]
	implicit val scheduleDtoWrites:Writes[ScheduleDto] = Json.writes[ScheduleDto]

	implicit val fightInfoFormat:Format[FightInfo] = Json.format[FightInfo]
	implicit val addDefenseCmdReads:Reads[AddDefenseCmd] = Json.reads[AddDefenseCmd]
	implicit val addDefenseEventWrites:Writes[AddDefenseEvent] = typeTagged(Json.writes[AddDefenseEvent])

	implicit val addAttackCmdReads:Reads[AddAttackCmd] = Json.reads[AddAttackCmd]
	implicit val addAttackEventWrites:Writes[AddAttackEvent] = typeTagged(Json.writes[AddAttackEvent])

	implicit val endWarCmdReads:Reads[EndWarCmd] = Json.reads[EndWarCmd]
	implicit val endWarEventWrites:Writes[EndWarEvent] = typeTagged(Json.writes[EndWarEvent])

	implicit val undoCmdReads:Reads[UndoCmd] = Json.reads[UndoCmd]
	implicit val undoEventWrites:Writes[UndoEvent] = typeTagged(Json.writes[UndoEvent])
}

/**The load and the information will be distributed according to a hash of the organisation id, such that the state of an organisation be not distributed and strongly consistent.
 * So, all request corresponding to an organisation X will be attended by the single node that is the primary keeper of X. Secondary keepers of X will exist for resiliency sake only.   
 * @author Gustavo
 */
class CentralCtrl @Inject() (centralSrv: CentralSrv, val tte: TransacTransitionExec, implicit val env: Environment[User, JWTAuthenticator]) extends Silhouette[User, JWTAuthenticator] with TypicalActions[JWTAuthenticator] {

	def getWarInitState:Action[JsValue] = typicalAction(simpleDbLookups, centralSrv.getWarInitState _)
  def getWarEventsAfter:Action[JsValue] = typicalAction(simpleDbLookups, centralSrv.getWarEventsAfter _)
	def startPreparation:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.startPreparation _)
	def addParticipant:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.addParticipant _)
	def startBattle:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.startBattle _)
	def getSchedule:Action[JsValue] = typicalAction(simpleDbLookups, centralSrv.getSchedule _)
	def addGuess:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.startBattle _)
	def addAttack:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.addAttack _)
	def addDefense:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.addDefense _)
	def endWar:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.endWar _)
	def undo:Action[JsValue] = tryAction(dbWriteOperations, centralSrv.undo _)

}