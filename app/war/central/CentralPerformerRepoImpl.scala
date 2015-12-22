package war.central

import common.typeAliases._
import utils.JdbcTransacMode
import anorm._
import anorm.SqlParser.get
import settings.account.Account
import common.typeAliases._
import settings.membership.Organization
import log.OrgaEvent
import scala.util.Try
import org.postgresql.util.PSQLException
import common.AlreadyExistException
import scala.util.Failure
import settings.membership.Icon
import utils.JdbcTransacTransitionExec
import utils.TransactionalTransition._
import javax.inject.Inject
import utils.TransacTransitionExec

/**
 * @author Gustavo
 */
class CentralPerformerRepoImpl extends CentralPerformerRepo {

  override def insertNewWarEvent(actorIconName: String): TiTac[WarEventInfo] = {
    val sql = SQL"insert into war_event (id, instant, actor_icon_name) values (default, LOCALTIMESTAMP, $actorIconName)"
    JdbcTransacMode.inConnection { (implicit connection =>
      sql.executeInsert(CentralKnowerRepoImpl.warEventInfoParser.single))
    }
  }

  override def insertStartPreparationEvent(warEventId: WarEvent.Id, orgaEventId: OrgaEvent.Id, organizationId: Organization.Id, enemyClanName: String, enemyClanTag: String): TiTac[Unit] = {
    val sql = SQL"""
insert into war_clash
  (add_event_id, start_clash_orga_event_id, organization_id, enemy_clan_name, enemy_clan_tag) values
  ($warEventId, $orgaEventId,              $organizationId, $enemyClanName,  $enemyClanTag)"""

    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }
  
  def insertUndoStartPreparationEvent(undoEventId: WarEvent.Id, undoneEventId: WarEvent.Id): TtTm[Unit] = {
    ???
  }


  override def putNextClashMark(previousClashId: WarEvent.Id, newClashId: Option[WarEvent.Id]): TiTac[Unit] = {
    val sql = SQL"update war_clash set next_clash_id = $newClashId where add_event_id = $previousClashId"
    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }

  override def insertAddParticipantEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, iconTag: Icon.Tag, basePosition: Position): TtTm[Unit] = {
    val sql = SQL"""
insert into war_participant
  (add_event_id, clash_id, icon_tag, base_position) values
  ($warEventId, $clashId, $iconTag, $basePosition)"""

    JdbcTransacMode.inConnectionTry { implicit connection =>
      Try[Unit](sql.executeUpdate()).recover {
        case aee: PSQLException if aee.getSQLState() == "23505" => // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
          Failure(new AlreadyExistException("The position is already occupied by another member.", aee))
      }
    }
  }
  
  def insertUndoAddParticipantEvent(undoEventId: WarEvent.Id, undoneEventId: WarEvent.Id): TtTm[Unit] = {
    ???
  }

  override def insertStartBattleEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TtTm[Unit] = {
    val sql = SQL""""
insert into war_battle
  (add_event_id, clash_id) values
  ($warEventId, $clashId)"""

    JdbcTransacMode.inConnectionTry { implicit connection =>
      Try[Unit](sql.executeUpdate()).recover {
        case aee: PSQLException if aee.getSQLState() == "23505" => // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
          Failure(new AlreadyExistException("The battle was already started.", aee))
      }
    }
  }

  override def putCurrentBattleMark(clashId: WarEvent.Id, currentBattleId: WarEvent.Id): TiTac[Unit] = {
    val sql = SQL"update war_clash set current_battle_id = currentBattleId where add_event_id = $clashId"
    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }

  override def insertFightEvent(warEventId: WarEvent.Id, battleId: WarEvent.Id, fightInfo: FightInfo, kind: FightKind): TtTm[Unit] = {
    val sql = SQL"""
insert into war_fight
  (add_event_id, battle_id,   participant_position,            opponent_position,            kind,    suwe,              stars,             destruction) values
  ($warEventId, $battleId, ${fightInfo.participantPosition}, ${fightInfo.opponentPosition}, $kind, ${fightInfo.suwe}, ${fightInfo.stars}, ${fightInfo.destruction})"""

    JdbcTransacMode.inConnectionTry { implicit connection =>
      Try[Unit](sql.executeUpdate()).recover {
        case aee: PSQLException if aee.getSQLState() == "23505" => // unique_violation - duplicate key value violates unique constraint. See "http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html"
          Failure(new AlreadyExistException("A fight with the same attacker and defender already exist in this war.", aee))
      }
    }
  }

  override def getSchedule(clashId: WarEvent.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto] = {
    ???
  }

  override def insertAddGuessEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: AddGuessCmd): TtTm[Unit] = {
    ???
  }

  override def insertEndWarEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TiTac[Unit] = {
    val sql = SQL"insert int war_end (add_event_id, battle_id) values ($warEventId, $clashId)"
    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }

}

object CentralPerformerRepoImpl {
}