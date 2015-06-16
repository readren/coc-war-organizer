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

	override def startPreparation(accountId: Account.Id, cmd: StartPreparationCmd): TiTac[StartPreparationEvent] =
		tte.inTransaction {
			
			???
		}

}