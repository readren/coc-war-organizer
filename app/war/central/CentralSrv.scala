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

/**
 * @author Gustavo
 */

trait Command {
	val actor: Account.Tag
}

object WarEvent {
	type Id = Long
	type Instant = java.time.Instant
}
trait WarEvent {
	val id: WarEvent.Id
	val instant: WarEvent.Instant
	val actorIconName: String  
}
case class WarEventInfo(id: WarEvent.Id, instant: WarEvent.Instant, actorIconName: String)
abstract class WarEventBase(wei: WarEventInfo) extends WarEvent {
	val id = wei.id
	val instant = wei.instant
	val actorIconName = wei.actorIconName
}



///////////////////////////

trait CentralSrv {

	def getWarState()
	def startPreparation(accountId: Account.Id, cmd: StartPreparationCmd): TiTac[StartPreparationEvent]

}


