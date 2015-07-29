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
import javax.inject.Inject
import utils.TransacTransitionExec

/**
 * @author Gustavo
 */
class CentralDaoImpl @Inject() (jtte: JdbcTransacTransitionExec) extends CentralDao {

  def getWarEventsAfter(organizationId: Organization.Id, phase: WarPhaseInfo, oThreshold: Option[WarEvent.Instant]): TiTac[Seq[WarEvent]] = {
    def sql(sql: String): SimpleSql[Row] = {
      oThreshold match {
        case Some(threshold) => SQL(sql + " AND e.instant > {threshold}").asSimple().on("threshold" -> threshold)
        case None            => SQL(sql + " AND w.remove_event_id is null").asSimple()
      }
    }

    JdbcTransacMode.inTransaction { implicit connection =>

      var result: List[WarEvent] = Nil

      phase.oStartPreparationEventId.foreach { startPreparationEventId =>
        val startPreparationSql = sql("""
select w.add_event_id, e.instant, e.actor_icon_name, w.start_clash_orga_event_id, w.enemy_clan_name, w.enemy_clan_tag
from war_clash w
inner join war_event e on (e.id = w.add_event_id)
where w.add_event_id = {startPreparationEventId}""").on("startPreparationEventId" -> startPreparationEventId)
        val startPreparationParser: RowParser[StartPreparationEvent] =
          CentralDaoImpl.warEventInfoParser ~ get[OrgaEvent.Id](4) ~ get[String](5) ~ get[String](6) map {
            case wei ~ orgaEventId ~ enemyClanName ~ enemyClanTag => StartPreparationEvent(wei, orgaEventId, enemyClanName, enemyClanTag)
          }
        result = startPreparationSql.as(startPreparationParser.single) :: result

        val addParticipantSql = sql("""
select w.add_event_id, e.instant, e.actor_icon_name, i.name, w.base_position
from war_participant w
inner join orga_icon i on (i.organization_id = {organizationId} AND i.tag = w.icon_tag)
inner join war_event e on (e.id = w.add_event_id)
where w.clash_id = {startPreparationEventId}""").on("startPreparationEventId" -> startPreparationEventId, "organizationId" -> organizationId)
        val addParticipantParser: RowParser[AddParticipantEvent] =
          CentralDaoImpl.warEventInfoParser ~ get[String](4) ~ get[Position](5) map {
            case wei ~ iconName ~ position => AddParticipantEvent(wei, iconName, position)
          }
        result ++= addParticipantSql.as(addParticipantParser.*)

        oThreshold.foreach { threshold =>
          val undoSql = SQL"""
select remove_event_id, e.instant, e.actor_icon_name, add_event_id
from (
    select add_event_id, remove_event_id
    from war_clash
    where remove_event_id is not null AND add_event_id = $startPreparationEventId
  union
    select add_event_id, remove_event_id
    from war_participant
    where remove_event_id is not null AND clash_id = $startPreparationEventId
  union
    select add_event_id, remove_event_id
    from war_reservation
    where remove_event_id is not null AND clash_id = $startPreparationEventId
  union
    select add_event_id, remove_event_id
    from war_guess
    where remove_event_id is not null AND clash_id = $startPreparationEventId
  union
    select add_event_id, remove_event_id
    from war_plan
    where remove_event_id is not null AND clash_id = $startPreparationEventId
  union
    select add_event_id, remove_event_id
    from war_queue
    where remove_event_id is not null AND clash_id = $startPreparationEventId
)
inner join war_event e on (e.id = remove_event_id)
where e.instant > $threshold"""
          val undoParser: RowParser[UndoEvent] =
            CentralDaoImpl.warEventInfoParser ~ get[WarEvent.Id](4) map {
              case wei ~ undoneEventId => UndoEvent(wei, undoneEventId)
            }
          result ++= undoSql.as(undoParser.*)
        }
      }

      phase.oStartBattleEventId.foreach { startBattleEventId =>
        val startBattleSql = sql("""
select p.add_event_id, e.instant, e.actor_icon_name
from war_battle b
inner join war_event e on (e.id = b.add_event_id)
where b.add_event_id = {startBattleEventId}""").on("startBattleEventId" -> startBattleEventId)
        val startBattleParser: RowParser[StartBattleEvent] =
          CentralDaoImpl.warEventInfoParser map {
            wei => StartBattleEvent(wei)
          }
        result = startBattleSql.as(startBattleParser.single) :: result

        oThreshold.foreach { threshold =>
          val undoSql = SQL"""
select remove_event_id, e.instant, e.actor_icon_name, add_event_id
from (
    select add_event_id, remove_event_id
    from war_battle
    where remove_event_id is not null AND add_event_id = $startBattleEventId
  union
    select add_event_id, remove_event_id
    from war_fight
    where remove_event_id is not null AND clash_id = $startBattleEventId
)
inner join war_event e on (e.id = remove_event_id)
where e.instant > $threshold"""
          val undoParser: RowParser[UndoEvent] =
            CentralDaoImpl.warEventInfoParser ~ get[WarEvent.Id](4) map {
              case wei ~ undoneEventId => UndoEvent(wei, undoneEventId)
            }
          result ++= undoSql.as(undoParser.*)
        }

      }

      phase.oEndWarEventId.foreach { endWarEventId =>
        val endWarSql = sql("""
select add_event_id, e.instant, e.actor_icon_name
from war_end
inner join war_event e on (e.id = add_event_id)
where add_event_id = {endWarEventId}""").on("endWarEventId" -> endWarEventId)
        val endWarParser: RowParser[EndWarEvent] =
          CentralDaoImpl.warEventInfoParser map { wei => EndWarEvent(wei) }
        result = endWarSql.as(endWarParser.single) :: result

        oThreshold.foreach { threshold =>
          val undoSql = SQL"""
select remove_event_id, e.instant, e.actor_icon_name, add_event_id
from (
    select add_event_id, remove_event_id
    from war_end
    where remove_event_id is not null AND add_event_id = $endWarEventId
)
inner join war_event e on (e.id = remove_event_id)
where e.instant > $threshold"""
          val undoParser: RowParser[UndoEvent] =
            CentralDaoImpl.warEventInfoParser ~ get[WarEvent.Id](4) map {
              case wei ~ undoneEventId => UndoEvent(wei, undoneEventId)
            }
          result ++= undoSql.as(undoParser.*)
        }
      }

      // TODO faltan las otras tablas que tienen eventos: war_reservation, war_guess, war_fight, etc
      result
    }
  }

  def getWarPhaseInfo(organizationId: Organization.Id): TiTac[WarPhaseInfo] = {
    val sql = SQL"""
select c.add_event_id, c.current_battle_id, b.current_end_id
from war_clash c
left outer join war_battle b on (b.add_event_id = c.current_battle_id)
where c.organization_id = $organizationId AND c.next_clash_id is null AND c.remove_event_id is null"""

    val parser: RowParser[WarPhaseInfo] = {
      get[WarEvent.Id](1) ~ get[WarEvent.Id](2).? ~ get[WarEvent.Id](3).? map {
        case clashId ~ None ~ None      => InPreparation(clashId)
        case clashId ~ battleId ~ None  => InBattle(clashId, battleId.get)
        case clashId ~ battleId ~ endId => InPostWar(clashId, battleId.get, endId.get)
        case _                          => throw new AssertionError
      }
    }

    JdbcTransacMode.inConnection { implicit connection =>
      sql.as(parser.singleOpt) match {
        case None      => InPeace
        case Some(wpi) => wpi
      }
    }
  }

  def getSchedule(accountId: Account.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto] = {
    ???
  }

  def insertNewWarEvent(actorIconName: String): TiTac[WarEventInfo] = {
    val sql = SQL"insert into war_event (id, instant, actor_icon_name) values (default, LOCALTIMESTAMP, $actorIconName)"
    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeInsert(CentralDaoImpl.warEventInfoParser.single)
    }
  }

  def insertStartPreparationEvent(warEventId: WarEvent.Id, orgaEventId: OrgaEvent.Id, organizationId: Organization.Id, enemyClanName: String, enemyClanTag: String): TiTac[Unit] = {
    val sql = SQL"""
insert into war_clash
  (add_event_id, start_clash_orga_event_id, organization_id, enemy_clan_name, enemy_clan_tag) values
  ($warEventId, $orgaEventId,              $organizationId, $enemyClanName,  $enemyClanTag)"""

    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }

  def putNextClashMark(previousClashId: WarEvent.Id, newClashId: WarEvent.Id): TiTac[Unit] = {
    val sql = SQL"update war_clash set next_clash_id = $newClashId where add_event_id = $previousClashId"
    JdbcTransacMode.inConnection { implicit connection =>
      sql.executeUpdate().ensuring(_ == 1)
    }
  }

  def insertUndoStartPreparationEvent() = ???

  def insertAddParticipantEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, iconTag: Icon.Tag, basePosition: Position): TtTm[Unit] = {
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

  def insertStartBattleEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id): TtTm[Unit] = {
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

  def insertFightEvent(warEventId: WarEvent.Id, battleId: WarEvent.Id, fightInfo: FightInfo, kind: FightKind): TtTm[Unit] = {
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

  def getSchedule(clashId: WarEvent.Id, cmd: GetScheduleCmd): TiTac[ScheduleDto] = {
    ???
  }

  def insertAddGuessEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: AddGuessCmd): TtTm[Unit] = {
    ???
  }

  def insertEndWarEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: EndWarCmd): TtTm[Unit] = {
    ???
  }

  def insertUndoEvent(warEventId: WarEvent.Id, clashId: WarEvent.Id, cmd: UndoCmd): TtTm[Unit] = {
    ???
  }

/*
    def getClashIdOf(organizationId: Organization.Id): TiTac[Option[WarEvent.Id]] = {
    val sql = SQL"""
select add_event_id
from war_clash
where organization_id = $organizationId  AND next_clash_id is null  AND remove_event_id is null"""

    JdbcTransacMode.inConnection { implicit connection =>
      sql.as(get[WarEvent.Id](1).singleOpt)
    }
  }

  def getBattleIdOf(clashId: WarEvent.Id): TiTac[Option[WarEvent.Id]] = {
    val sql = SQL"""
select add_event_id
from war_battle
where clash_id = $clashId AND remove_event_id is null"""

    JdbcTransacMode.inConnection { implicit connection =>
      sql.as(get[WarEvent.Id](1).singleOpt)
    }
  }

  def getEndIdOf(battleId: WarEvent.Id): TiTac[Option[WarEvent.Id]] = {
    val sql = SQL"""
select add_event_id
from war_end
where battle_id = $battleId AND remove_event_id is null"""

    JdbcTransacMode.inConnection { implicit connection =>
      sql.as(get[WarEvent.Id](1).singleOpt)
    }
  }
*/

}

object CentralDaoImpl {

  val warEventInfoParser: RowParser[WarEventInfo] =
    get[WarEvent.Id](1) ~ get[WarEvent.Instant](2) ~ get[String](3) map {
      case id ~ instant ~ iconName => WarEventInfo(id, instant, iconName)
    }

}