/**
 *
 */
package log

import utils.Transition
import auth.models.User
import utils.TransacMode
import utils.JdbcTransacMode
import anorm._
import anorm.SqlParser.get
import settings.membership.Organization

/**
 * @author Gustavo
 *
 */
class LogDaoImpl extends LogDao {

	override def insert(): Transition[TransacMode, (Event.Id, Event.Instant)] = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"insert into orga_event (id, instant) values (default, LOCALTIMESTAMP)"
		sql.executeInsert(LogDaoImpl.eventParser.single)
	}
}

object LogDaoImpl {
	val eventParser: RowParser[(Event.Id, Event.Instant)] =
		get[Event.Id](1) ~ get[Event.Instant](2) map {
			case id ~ instant => (id, instant)
		}

}