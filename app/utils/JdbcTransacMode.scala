

package utils
import java.sql.Connection
import javax.sql.DataSource
import javax.inject.Inject
import play.api.Logger

/**
 * Implementation of [[TransacMode]] for JDBC
 * @author Gustavo
 */
trait JdbcTransacMode extends TransacMode {
	val logger = Logger(classOf[JdbcTransacMode])
	/** Given [[Connection]] is referentially opaque, any calling code will be also.  */
	def getConnection(): Connection
	def inConnection[X](block: Connection => X): Transition[JdbcTransacMode, X]
}

object JdbcTransacMode {
	def apply[X](block: JdbcTransacMode => X) = Transition[TransacMode, X] {
		(transacMode =>
			TransitionResult(transacMode, block(transacMode.asInstanceOf[JdbcTransacMode]))
		)
	}

	def inConnection[X](block: Connection => X) = Transition[TransacMode, X](transacMode => {
		val jdbcTransacMode = transacMode.asInstanceOf[JdbcTransacMode]
		jdbcTransacMode.inConnection(block).run(jdbcTransacMode)
	})
}

class OutJdbcTransacMode(dataSource: DataSource) extends JdbcTransacMode with OutTransacMode {
	override def isTruly = true;
	override def begin = new InJdbcTransacMode(dataSource)
	override def getConnection() = dataSource.getConnection().ensuring(_.getAutoCommit())

	override def inConnection[X](block: Connection => X) = Transition[JdbcTransacMode, X] { jdbcTransacState =>
		val connection = jdbcTransacState.asInstanceOf[JdbcTransacMode].getConnection()
		try {
			TransitionResult(jdbcTransacState, block(connection))
		} finally {
			connection.close()
		}
	}
}

class InJdbcTransacMode(dataSource: DataSource) extends JdbcTransacMode with InTransacMode {
	logger.info("getConnection")
	private[this] val connection = dataSource.getConnection()
	connection.setAutoCommit(false)

	private[this] var stillTruly = true;
	override def isTruly = stillTruly;

	override def getConnection() = {
		assert(stillTruly)
		connection
	}

	override def commit = {
		logger.info("commit")
		assert(stillTruly)
		connection.commit()
		connection.setAutoCommit(true)
		connection.close()
		stillTruly = false
		new OutJdbcTransacMode(dataSource)
	}

	override def rollback = {
		if( stillTruly)
			logger.info("rollBack")
		else
			logger.error("uncertain rollback")
		stillTruly = false
		connection.rollback()
		connection.setAutoCommit(true)
		connection.close()
		new OutJdbcTransacMode(dataSource)
	}

	override def inConnection[X](block: Connection => X) = Transition[JdbcTransacMode, X] { jdbcTransacState =>
		TransitionResult(jdbcTransacState, block(jdbcTransacState.asInstanceOf[JdbcTransacMode].getConnection()))
	}
}

class JdbcTransacTransitionExec @Inject() (dataSource: DataSource) extends TransacTransitionExec {
	override val initialTransacMode = new OutJdbcTransacMode(dataSource)
}


