/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.startup

import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.util.ManagedUtils
import jakarta.persistence.EntityManager
import org.hibernate.internal.SessionImpl

/** Helper database ops for startup tasks.
  */
trait DatabaseOps:
  val logger: org.log4s.Logger

  def leaveTransaction(): Unit = ManagedUtils.leaveTransaction()

  def executeUpdate(sql: String)(implicit qs: QueryService): Int =
    val query = qs.createQuery(sql)
    val count = query.executeUpdate
    logger info s"Update $count"
    count

  def executeNativeUpdate(sql: String)(implicit em: EntityManager): Int =
    var count = 0
    em.getDelegate.asInstanceOf[SessionImpl] doWork { connection =>
      val statement = connection.createStatement
      count = statement.executeUpdate(sql)
      logger info s"Update $count"
    }
    count

  def dropTable(finder: String)(implicit em: EntityManager): Unit =
    logger info s"Drop table $finder"
    executeNativeUpdate(s"DROP TABLE IF EXISTS $finder") // CASCADE?

  def clearTable(finder: String)(implicit em: EntityManager): Unit =
    logger info s"Clear table $finder"
    executeNativeUpdate(s"DELETE FROM $finder") // TRUNCATE?

  def dropColumn(finder: String)(column: String)(implicit em: EntityManager): Unit =
    logger info s"Drop column $finder.$column"
    executeNativeUpdate(s"ALTER TABLE $finder DROP COLUMN IF EXISTS $column CASCADE")

  def dropIndex(index: String)(implicit em: EntityManager): Unit =
    logger info s"Drop index $index"
    executeNativeUpdate(s"DROP INDEX CONCURRENTLY IF EXISTS $index")

  def dropConstraint(finder: String)(constraint: String)(implicit em: EntityManager): Unit =
    logger info s"Drop constraint $constraint"
    executeNativeUpdate(s"ALTER TABLE $finder DROP CONSTRAINT IF EXISTS $constraint")

  def tableExists(tableName: String)(implicit qs: QueryService): Boolean =
    sql"""SELECT c.relname FROM pg_class c
          WHERE c.relname = LOWER($tableName)""".getResultList.size > 0

  /** Whether the table has a column of this name */
  def columnExists(tableName: String, columnName: String)(implicit qs: QueryService): Boolean =
    sql"""SELECT c.relname FROM pg_class c, pg_attribute a, pg_type t
          WHERE c.relname = LOWER($tableName) AND a.attnum > 0
          AND a.attrelid = c.oid AND a.atttypid = t.oid
          AND a.attname = LOWER($columnName)""".getResultList.size > 0
end DatabaseOps
