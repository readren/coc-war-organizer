/**
 *
 */
package utils

import anorm.ToStatement
import java.util.UUID
import org.postgresql.util.PGInterval

/**
 * Copiado de {@link http://stackoverflow.com/questions/18838261/how-do-i-add-an-additional-implicit-extractor-in-play-2-1-4-and-actually-use-it}
 *
 */
object UuidToStatement {
	implicit def uuidToStatement = new ToStatement[UUID] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: UUID): Unit = s.setObject(index, aValue)
  }
	
		implicit def intervalToStatement = new ToStatement[PGInterval] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: PGInterval): Unit = s.setObject(index, aValue)
  }

}