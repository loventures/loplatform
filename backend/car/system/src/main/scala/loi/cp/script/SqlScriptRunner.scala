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

import com.github.tototoshi.csv.CSVWriter
import com.learningobjects.cpxp.util.JDBCUtils.*
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.script.SqlStatement.CommandType

import java.io.{File, FileOutputStream, OutputStreamWriter, Writer}
import java.lang.reflect.UndeclaredThrowableException
import java.nio.charset.StandardCharsets.UTF_8
import java.sql.SQLException
import java.time.Duration
import scala.util.Try

private[script] object SqlScriptRunner:

  def run(
    script: String,
    extendedTimeOut: Boolean,
    box: ScriptServlet.Box
  ): Unit =
    import SqlStatement.CommandType.*

    val context = ManagedUtils.getEntityContext
    context.setStatementTimeout(Duration.ofMinutes(if extendedTimeOut then 10 else 1))

    val console          = ConsoleWriter(box.writer)
    var fileOut: FWriter = null

    val statements = SqlStatement.statements(script)
    statements.foreach { s =>
      val statement = s.args.head

      val preamble =
        s"# ${statement.take(77)}${if statement.length() > 77 then "..." else ""}\n"

      s.commandType match
        case c if statements.size > 1 && c != Download => console.write(preamble)
        case _                                         => ()
      s.commandType match
        case Help =>
          console.write("Commands:\n\n")
          SqlStatement.CommandType.values
            .filter(_.help.isDefined)
            .foreach(c => console.write(HelpFmt.format(c.entryName, c.help.get)))

        case ListRelations =>
          val relArgs = s.args.slice(2, s.args.size)
          val query   = SqlStatement.listRelationsQuery(
            s.args(1),
            relArgs
          )
          queryAndWrite(FmtWriter(console.writer, ListRelFmt))(query, relArgs)

        case Explain =>
          queryAndWrite(FmtWriter(console.writer, underlineHeader = false))(statement)

        case ResultsQuery => queryAndWrite(console)(statement)

        case UpdateQueryReturning => trySql { queryAndWrite(console)(statement) }

        case UpdateQuery => trySql { console.write(s"${doUpdate(statement)}\n") }

        case Download => // TODO make all commands downloadable?
          if fileOut == null then
            fileOut = FWriter()
            box.tmpFiles.add(fileOut.file)
          fileOut.write(preamble)
          queryAndWrite(fileOut.writer)(s.args(1))
          fileOut.write(fileOut.csv.format.lineTerminator)

        case Format =>
          queryAndWrite(FmtWriter(console.writer, s.args(1)))(s.args(2))

        case Describe =>
          val table                                      = Seq(s.args(1).replace("\"", ""))
          val label                                      = s"Table '${table.head}'"
          console.write((1 to ((91 - label.length) / 2)).map(_ => ' ').mkString(""))
          console.write(label)
          console.write("\n")
          def fmtWrite(query: String, args: Seq[String]) =
            queryAndWrite(FmtWriter(console.writer, writeHeader = false))(query, args)

          queryAndWrite(FmtWriter(console.writer, DescFmt))(
            SqlStatement.TableInfoQuery,
            table
          )

          console.write("\nForeign-key constraints:\n")
          fmtWrite(SqlStatement.ForeignKeyQuery, table)

          console.write("\nReferenced by:\n")
          fmtWrite(SqlStatement.ReferencedByQuery, table)

          console.write("\nIndexes:\n")
          fmtWrite(SqlStatement.IndexQuery, table)
        case null     => ()
      end match
      if s.commandType != Download then console.write("\n")
    }
    if fileOut != null then
      fileOut.close()
      box.file = fileOut.file
  end run

  private def queryAndWrite(
    rw: ResultSetWriter
  )(query: String, args: Seq[String] = Nil): Boolean =
    var wrote = false
    trySql {
      doQuery(query, args) { rs =>
        wrote = resultSetSink(rw)(rs) > 0
      }
    }
    wrote
  end queryAndWrite

  private val ListRelFmt = "%-8s｜%-50s｜%-20s｜%-8s\n"
  private val DescFmt    = "%-34s｜%-35s｜%-20s\n"
  private val HelpFmt    = s"%-${CommandType.EntryColLen}s%s\n"

  private case class FWriter(
    file: File = File.createTempFile("sql", ".csv")
  ):
    private val fos = new OutputStreamWriter(new FileOutputStream(file), UTF_8)

    val csv: CSVWriter          = CSVWriter.open(fos)
    val writer: ResultSetWriter = ResultSetWriter[CSVWriter](csv.writeRow)

    def write(s: String): Unit = fos.write(s)
    def close(): Unit          = fos.close()
  end FWriter

  private case class FmtWriter(
    writer: Writer,
    format: String = "%s\n",
    writeHeader: Boolean = true,
    underlineHeader: Boolean = true
  ) extends ResultSetWriter:

    override def writeHeader(cols: Seq[Any]): Unit = if writeHeader then
      val str = format.format(cols*)
      writer.write(str)
      if underlineHeader then
        writer.write((1 to str.length).map(_ => '─').mkString(""))
        writer.write("\n")

    override def writeRow(cols: Seq[Any]): Unit =
      writer.write(format.format(cols*))
  end FmtWriter

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

  private def trySql(f: => Unit): Unit =
    Try(f)
      .recover({ case u: UndeclaredThrowableException =>
        u.getUndeclaredThrowable.getCause match
          case ex: SQLException =>
            log.warn(s"${ex.getClass.getSimpleName} - ${ex.getMessage}")
            ManagedUtils.rollback()
          case _                => throw u
      })
      .get
end SqlScriptRunner
