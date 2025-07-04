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
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentInstance}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.FileUtils
import com.learningobjects.cpxp.util.JDBCUtils.*
import com.learningobjects.cpxp.util.logging.ThreadLogs
import loi.cp.job.{AbstractEmailJob, EmailJobFacade, GeneratedReport, JobUtils}
import org.apache.commons.lang3.exception.ExceptionUtils
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.regex.*

import javax.script.ScriptEngine

@Component(name = "Scripted Report")
class ScriptedReportImpl(
  val componentInstance: ComponentInstance,
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  domain: DomainDTO,
  ts: TimeSource,
  componentEnvironment: ComponentEnvironment,
) extends AbstractEmailJob[ScriptedReport]
    with ScriptedReport:
  import ScriptedReport.*
  import ScriptedReportImpl.*

  override def update(job: ScriptedReport): ScriptedReport =
    self.setAttribute("script", job.getScript)
    super.update(job)

  override def getScript: String = Option(self.getAttribute("script", classOf[String])).orZero

  override protected def generateReport(): GeneratedReport = executeScript(getScript, debug = false)

  override def debug(script: DebugScript): ErrorResponse \/ GeneratedReport =
    val (report, logs) = ThreadLogs.logged {
      \/.attempt(executeScript(script.script, debug = false))(identity)
    }
    report leftMap { th =>
      ErrorResponse.badRequest(DebugError(ExceptionUtils.getRootCauseStackTrace(th).mkString("\n"), logs))
    }

  private def executeScript(script: String, debug: Boolean): GeneratedReport =
    if SqlRe `lookingAt` script then executeSql(script)
    else engine(debug).eval(script).asInstanceOf[GeneratedReport]

  private def executeSql(script: String): GeneratedReport =
    val now  = ts.instant
    val name = Option(self).cata(_.getName, "Job")
    val sql  = script.replace("${domain}", domain.id.toString)
    val att  = JobUtils.csv(s"${name}_${FileUtils.cleanFilename(now.toString)}.csv") { csv =>
      doQuery(sql)(rs => resultSetSink(ResultSetWriter[CSVWriter](csv.writeRow))(rs))
    }
    GeneratedReport(s"$name: $now", "See attached.", html = false, att :: Nil)

  private def engine(debug: Boolean): ScriptEngine =
    DEScalaEngine.newEngine(new LoggingWriter(jLogger), componentEnvironment.getClassLoader, debug)

  override val logger = org.log4s.getLogger
end ScriptedReportImpl

object ScriptedReportImpl:

  private final val jLogger = java.util.logging.Logger.getLogger(classOf[ScriptedReportImpl].getName)

  private final val SqlRe = "(?i)\\s*(?:select|with)".r

  final case class DebugError(throwable: String, logs: String)
