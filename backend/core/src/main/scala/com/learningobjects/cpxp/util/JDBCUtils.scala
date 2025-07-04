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

package com.learningobjects.cpxp.util

import com.github.tototoshi.csv.CSVWriter
import org.hibernate.{CacheMode, Session}

import java.sql.{PreparedStatement, ResultSet}
import java.util.Date
import scala.util.Using

object JDBCUtils:

  trait ResultSetWriter:
    def writeHeader(cols: Seq[Any]): Unit = writeRow(cols)
    def writeRow(cols: Seq[Any]): Unit
  object ResultSetWriter:
    def apply[A](f: Seq[Any] => Unit)(implicit
      fmt: ColFormatter[A]
    ): ResultSetWriter = cols => f(cols.map(fmt.format))

  trait ColFormatter[+A <: Any]:
    def format(a: Any): Any = DefaultFormatter.format(a)

  def resultSetSink(rw: ResultSetWriter)(rs: ResultSet): Int =
    var written = 0
    if rs.isBeforeFirst then
      val md    = rs.getMetaData
      val count = 1 to md.getColumnCount
      rw.writeHeader(count.map(md.getColumnLabel))
      written += 1
      while rs.next() do
        rw.writeRow(count.map(rs.getObject))
        written += 1
    written
  end resultSetSink

  def doQuery[A](query: String, args: Args = Nil, disableCache: Boolean = false)(f: ResultSet => A): A =
    runStatement(query, args, disableCache) { stmt =>
      Using.resource(stmt.executeQuery())(f)
    }

  def doUpdate(query: String, args: Seq[String] = Nil): Int =
    runStatement[Int](query, args, disableCache = false) { stmt =>
      stmt.executeUpdate()
    }

  private type Args = Seq[String]

  private def runStatement[A](query: String, args: Args, disableCache: Boolean)(
    f: PreparedStatement => A
  ): A =
    val em      = ManagedUtils.getEntityContext.getEntityManager
    val session = em.unwrap(classOf[Session])
    if disableCache then session.setCacheMode(CacheMode.IGNORE)
    session.doReturningWork { con =>
      Using.resource(con.prepareStatement(query)) { stmt =>
        // do later -- make Args Seq[Any] and use proper setters
        args.zipWithIndex.foreach { case (s, i) =>
          stmt.setString(i + 1, s) // injection safe
        }
        f(stmt)
      }
    }
  end runStatement

  implicit object CsvFormatter extends ColFormatter[CSVWriter]

  object DefaultFormatter extends ColFormatter[Any]:
    override def format(a: Any): Any = a match
      case d: Date => d.toInstant.toString
      case _       => a
end JDBCUtils
