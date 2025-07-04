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

package loi.authoring.project

import cats.syntax.either.*
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebRequest}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.cp.i18n.AuthoringBundle
import loi.cp.i18n.syntax.bundleMessage.*

import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
@Controller(root = true, value = "sync-report-web-controller")
@Secured(Array(classOf[AccessAuthoringAppRight]))
class SyncReportWebController(
  ci: ComponentInstance,
  domainDto: => DomainDTO,
  syncReportDao: SyncReportDao
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/sync-reports/{reportId}", method = Method.GET)
  def getReportCsv(@PathVariable("reportId") reportId: Long, request: WebRequest): FileResponse[?] =

    val entity = syncReportDao.loadEntity(reportId).getOrElse(AuthoringBundle.noSuchSynReport(reportId).throw404)
    val report = entity.body.jdecode[SyncReport].result.valueOr { case (err, loc) =>
      throw new RuntimeException(s"failed to decode sync report $reportId; $err at $loc")
    }

    val domainFormatter = SyncReportWebController.Formatter.withZone(ZoneId.of(domainDto.timeZone))

    val code     = Option(entity.project.code).getOrElse(entity.project.id.toString)
    val time     = entity.created.format(domainFormatter)
    val id       = entity.id.toString
    val filename = s"$code-sync-report-$time-$id.csv"

    val exportFile = ExportFile.create(filename, MediaType.CSV_UTF_8, request)
    exportFile.file.writeCsvWithBom[SyncReport.Row] { csv =>
      report.rows.foreach(csv.write)
    }

    FileResponse(exportFile.toFileInfo)
  end getReportCsv
end SyncReportWebController

object SyncReportWebController:
  private val Formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM'T'HH-mm-ss-z")
