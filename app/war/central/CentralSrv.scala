package war.central

/**
 * El cliente tiene que ser capaz de reproducir el estado de la cosa a partir de un estado previo y un conjunto de cambios (pregunta: ¿es costoso que los cambios estén indexados?).
 * El servidor debe poder dar los cambios que ocurrieron desde cierto punto.
 * El servidor debe poder dar el estado completo de la cosa en ciertos puntos singulares.
 * Los puntos singulares podrían ser aquellos donde no es necesario conocer los cambios anteriores para conocer el estado requerido.
 * Prohibir el deshacer de un punto singular permitiría poder olvidar los cambios anteriores.
 * El estado completo puede estar compuesto por el estado en un punto singular y una serie ordenada de cambios. No todos los cambios sufridos por la cosa, sino solo los que no han sido deshechos.
 * Se me ocurren tres tipos de cambio en cuanto a la capacidad que se tiene de poder deshacerlo (poder ver como quedaría el estado de la cosa si el cambio no hubiera sido realizado): los que no toleran ser deshechos, los que para deshacerlos hay que recrear el estado desde el punto anterior a ser cometido, y los que pueden deshacer su efecto sin necesidad de deshacer y rehacer los cambios posteriores.
 */

import settings.membership.Organization
import settings.membership.Icon
import common.typeAliases._
import settings.account.Account
import settings.account.Account
import utils.TransacMode
import utils.Transition
import utils.TransacTransitionExec
import utils.TransactionalTransition._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import common.ParameterlessCmd
import scala.util.Try

/**
 * @author Gustavo
 */

object WarEvent {
  type Id = Long
  type Instant = org.joda.time.DateTime
  implicit def jsonWrites: Writes[WarEvent] = Writes[WarEvent](_.toJson)
}
trait WarEvent {
  val id: WarEvent.Id
  val instant: WarEvent.Instant
  val actorIconName: String
  def toJson: JsValue
  def undoOp(centralPerformer:CentralPerformer): (WarEvent, Icon) => TtTm[UndoEvent]
}
case class WarEventInfo(id: WarEvent.Id, instant: WarEvent.Instant, actorIconName: String)
object WarEventInfo {
  implicit val jsonWrites = Json.writes[WarEventInfo]
}
abstract class WarEventBase(wei: WarEventInfo) extends WarEvent {
  val id = wei.id
  val instant = wei.instant
  val actorIconName = wei.actorIconName
}

trait CentralSrv {

  def getWarInitState(accountId: Account.Id, cmd: ParameterlessCmd): TiTac[Seq[WarEvent]]
  def getWarEventsAfter(accountId: Account.Id, cmd: GetWarEventsAfterCmd): TiTac[Seq[WarEvent]]

  def startPreparation(accountId: Account.Id, cmd: StartPreparationCmd): TtTm[Seq[WarEvent]]
  def addParticipant(accountId: Account.Id, cmd: AddParticipantCmd): TtTm[Seq[WarEvent]]
  def startBattle(accountId: Account.Id, cmd: StartBattleCmd): TtTm[Seq[WarEvent]]
  def addGuess(accountId: Account.Id, cmd: AddGuessCmd): TtTm[Seq[WarEvent]]
  def getSchedule(accountId: Account.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto]
  def addAttack(accountId: Account.Id, cmd: AddAttackCmd): TtTm[Seq[WarEvent]]
  def addDefense(accountId: Account.Id, cmd: AddDefenseCmd): TtTm[Seq[WarEvent]]
  def endWar(accountId: Account.Id, cmd: EndWarCmd): TtTm[Seq[WarEvent]]

  def undo(accountId: Account.Id, cmd: UndoCmd): TtTm[Seq[WarEvent]]

}


