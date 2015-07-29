package utils

import java.sql.Connection

import scala.util.Try

import javax.inject.Inject
import javax.sql.DataSource
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
  def inConnectionTry[X](block: Connection => Try[X]): TransitionTry[JdbcTransacMode, X]
}

object JdbcTransacMode {
  def apply[X](block: JdbcTransacMode => X): Transition[TransacMode, X] = Transition[TransacMode, X] {
    (transacMode =>
      TransitionResult(transacMode, block(transacMode.asInstanceOf[JdbcTransacMode])))
  }

  def inConnection[X](block: Connection => X): Transition[TransacMode, X] = Transition[TransacMode, X] { transacMode =>
    val jdbcTransacMode = transacMode.asInstanceOf[JdbcTransacMode]
    jdbcTransacMode.inConnection(block).run(jdbcTransacMode)
  }

  def inConnectionTry[X](block: Connection => Try[X]): TransitionTry[TransacMode, X] = TransitionTry[TransacMode, X] { transacMode =>
    val jdbcTransacMode = transacMode.asInstanceOf[JdbcTransacMode]
    jdbcTransacMode.inConnectionTry(block).run(jdbcTransacMode)
  }

  def inTransaction[X](block: Connection => X): Transition[TransacMode, X] = TransacTransitionExec.inTransaction(inConnection(block))
  def inTransactionTry[X](block: Connection => Try[X]): TransitionTry[TransacMode, X] = Transition.toTransitionTry(TransacTransitionExec.inTransaction(inConnection(block)))
}

class OutJdbcTransacMode(dataSource: DataSource) extends JdbcTransacMode with OutTransacMode {
  override def isTruly = true;
  override def begin = new InJdbcTransacMode(dataSource)
  override def getConnection() = dataSource.getConnection().ensuring(_.getAutoCommit())

  override def inConnection[X](block: Connection => X): Transition[JdbcTransacMode, X] = Transition[JdbcTransacMode, X] { jdbcTransacMode =>
    val connection = jdbcTransacMode.asInstanceOf[OutJdbcTransacMode].getConnection()
    assert(!connection.getAutoCommit)
    try {
      TransitionResult(jdbcTransacMode, block(connection))
    } finally {
      connection.close()
    }
  }

  override def inConnectionTry[X](block: Connection => Try[X]): TransitionTry[JdbcTransacMode, X] = TransitionTry[JdbcTransacMode, X] { jdbcTransacMode =>
    val connection = jdbcTransacMode.asInstanceOf[OutJdbcTransacMode].getConnection()
    assert(!connection.getAutoCommit)
    try {
      TransitionResult(jdbcTransacMode, block(connection))
    } finally {
      connection.close()
    }
  }
}

class InJdbcTransacMode(dataSource: DataSource) extends JdbcTransacMode with InTransacMode {
  logger.info("transaction begin")
  private[this] val connection = dataSource.getConnection()
  connection.setAutoCommit(false)

  private[this] var stillTruly = true;
  override def isTruly = stillTruly;

  override def getConnection() = {
    assert(stillTruly)
    assert(!connection.getAutoCommit)
    connection
  }

  override def commit = {
    logger.info("commit")
    assert(stillTruly)
    assert(!connection.getAutoCommit)
    connection.commit()
    connection.setAutoCommit(true)
    connection.close()
    stillTruly = false
    new OutJdbcTransacMode(dataSource)
  }

  override def rollback = {
    if (stillTruly)
      logger.info("rollBack")
    else
      logger.error("uncertain rollback")
    stillTruly = false
    connection.rollback()
    connection.setAutoCommit(true)
    connection.close()
    new OutJdbcTransacMode(dataSource)
  }

  override def inConnection[X](block: Connection => X): Transition[JdbcTransacMode, X] = Transition[JdbcTransacMode, X] { jdbcTransacState =>
    TransitionResult(jdbcTransacState, block(jdbcTransacState.asInstanceOf[InJdbcTransacMode].getConnection()))
  }

  override def inConnectionTry[X](block: Connection => Try[X]): TransitionTry[JdbcTransacMode, X] = TransitionTry[JdbcTransacMode, X] { jdbcTransacState =>
    TransitionResult(jdbcTransacState, block(jdbcTransacState.asInstanceOf[InJdbcTransacMode].getConnection()))
  }
}

class JdbcTransacTransitionExec @Inject() (dataSource: DataSource) extends TransacTransitionExec {
  override val initialTransacMode = new OutJdbcTransacMode(dataSource)
}


