package common

import settings.account.Account

/**
 * @author Gustavo
 */
trait Command {
	val actor: Account.Tag
}