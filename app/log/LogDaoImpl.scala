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

	override def insert(): Transition[TransacMode, (OrgaEvent.Id, OrgaEvent.Instant)] = JdbcTransacMode.inConnection { implicit connection =>
		val sql = SQL"insert into orga_event (id, instant) values (default, LOCALTIMESTAMP)"
		sql.executeInsert(LogDaoImpl.eventParser.single)
	}
}

object LogDaoImpl {
	val eventParser: RowParser[(OrgaEvent.Id, OrgaEvent.Instant)] =
		get[OrgaEvent.Id](1) ~ get[OrgaEvent.Instant](2) map {
			case id ~ instant => (id, instant)
		}

}