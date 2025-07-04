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

package loi.cp.script

import com.learningobjects.cpxp.util.JDBCUtils.{DefaultFormatter, ResultSetWriter}
import com.typesafe.config.{Config, ConfigFactory}
import loi.cp.script.SqlStatement.CommandType
import loi.db.ConfigPersistenceUnitInfo

import java.io.Writer
import java.sql.{PreparedStatement, ResultSet}
import scala.util.Using

private[script] object RedshiftScriptRunner:

  val config: Config = ConfigFactory.load().getConfig("de.databases.redshift")
  val dataSource     = ConfigPersistenceUnitInfo.dataSource(config).getConnection

  def run(
    script: String,
    box: ScriptServlet.Box
  ): Unit =

    val console = ConsoleWriter(box.writer)

    var statements = SqlStatement.statements(script)
    statements.foreach { s =>
      val statement = s.args.head

      s.commandType match
        case CommandType.Help                 =>
        case CommandType.Download             =>
        case CommandType.Format               =>
        case CommandType.Describe             =>
        case CommandType.ListRelations        =>
        case CommandType.Explain              =>
        case CommandType.ResultsQuery         => queryAndWrite(console)(statement)
        case CommandType.UpdateQueryReturning =>
        case CommandType.UpdateQuery          =>
      end match
    }
  end run

  def doQuery[A](query: String, args: Args = Nil, disableCache: Boolean = false)(f: ResultSet => A): A =
    runStatement(query, args, disableCache) { stmt =>
      Using.resource(stmt.executeQuery())(f)
    }

  private def queryAndWrite(
    rw: ResultSetWriter
  )(query: String, args: Seq[String] = Nil): Boolean =
    log.info(query)
    var wrote = false
    doQuery(query, args) { rs =>
      wrote = resultSetSink(rw)(rs) > 0
    }
    wrote

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

  private type Args = Seq[String]

  private def runStatement[A](query: String, args: Args, disableCache: Boolean)(
    f: PreparedStatement => A
  ): A =
    Using.resource(dataSource.prepareStatement(query)) { stmt =>
      args.zipWithIndex.foreach { case (s, i) =>
        stmt.setString(i + 1, s) // injection safe
      }
      f(stmt)
    }

  private case class ConsoleWriter(writer: Writer) extends ResultSetWriter:

    override def writeHeader(cols: Seq[Any]): Unit =
      val str = getRow(cols)
      writer.write(str)
      writer.write((1 to str.length).map(_ => '─').mkString(""))
      writer.write("\n")

    override def writeRow(cols: Seq[Any]): Unit = writer.write(getRow(cols))

    def write(s: String): Unit = writer.write(s)

    private def getRow(cols: Seq[Any]) =
      s"｜ ${cols.map(DefaultFormatter.format).mkString("｜")} ｜\n"
  end ConsoleWriter

  private val log = org.log4s.getLogger
end RedshiftScriptRunner
