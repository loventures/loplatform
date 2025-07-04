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

package loi.cp.maintenanceWindow

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.maintenanceWindow.MaintenanceWindowFinder
import com.learningobjects.cpxp.service.query.QueryService
import loi.cp.announcement.{AnnouncementDTO, AnnouncementService}
import org.apache.commons.text.StringEscapeUtils
import scaloi.syntax.DateOps.*

import scala.concurrent.duration.*
import scalaz.\/
import scalaz.syntax.std.option.*

/** Implementation of the Maintenance Window Crud Operations.
  */
@Component
class MaintenanceWindowRootApiImpl(val componentInstance: ComponentInstance)(implicit
  domain: () => DomainDTO,
  as: AnnouncementService,
  fs: FacadeService,
  qs: QueryService
) extends MaintenanceWindowRootApi
    with ComponentImplementation:
  import MaintenanceWindowRootApiImpl.*

  override def get(apiQuery: ApiQuery): ApiQueryResults[MaintenanceWindow] =
    ApiQueries.query[MaintenanceWindow](domain.queryChildren[MaintenanceWindowFinder], apiQuery)

  override def get(id: Long): Option[MaintenanceWindow] =
    get(ApiQuery.byId(id)).asOption

  override def create(maintenanceWindow: MaintenanceWindowDTO): MaintenanceWindow =
    val annDTO    = createAnnouncementDTO(maintenanceWindow)
    val ann       = as.create(domain, annDTO)
    val newWindow = MaintenanceWindowDTO(
      maintenanceWindow.startTime,
      maintenanceWindow.duration,
      maintenanceWindow.disabled,
      ann.getId
    )
    domain.addComponent[MaintenanceWindow](classOf[MaintenanceWindowImpl], newWindow)
  end create

  override def update(id: Long, maintenanceWindow: MaintenanceWindowDTO): ErrorResponse \/ MaintenanceWindow =
    for oldMaintenanceWindow <- get(id) \/> ErrorResponse.notFound
    yield
      val annDTO    = createAnnouncementDTO(maintenanceWindow)
      val newWindow = MaintenanceWindowDTO(
        maintenanceWindow.startTime,
        maintenanceWindow.duration,
        maintenanceWindow.disabled,
        oldMaintenanceWindow.getAnnouncementId
      )
      as.get(domain, oldMaintenanceWindow.getAnnouncementId) foreach { as.update(_, annDTO) }
      oldMaintenanceWindow.update(newWindow)
      oldMaintenanceWindow

  override def delete(id: Long): ErrorResponse \/ WebResponse =
    for maintenanceWindow <- get(id) \/> ErrorResponse.notFound
    yield
      as.get(domain, maintenanceWindow.getAnnouncementId) foreach { _.delete() }
      maintenanceWindow.delete()
      NoContentResponse

  // I really want to deliver a message key and parameters to the front end for it to translate it
  // in the user's timezone and language... But maybe in another decade.
  // TODO: locale and timezone and i18n for overlord because.. that's where maintenance windows are
  private def createAnnouncementDTO(maintenanceWindow: MaintenanceWindowDTO): AnnouncementDTO =
    implicit val cd = componentInstance.getComponent
    val startTime   = maintenanceWindow.startTime
    val duration    = maintenanceWindow.duration
    val hours       = duration / 60
    val minutes     = duration % 60
    val durationMsg = if hours == 0 then MinutesMsg else if minutes == 0 then HoursMsg else HoursAndMinutesMsg
    val durationStr = durationMsg.i18n("hours" -> hours, "minutes" -> minutes)
    val message     = MaintenanceMsg.i18n("duration" -> durationStr, "startTime" -> startTime)
    // By default, run an announcement from a week before until five minutes after the maintenance window starts
    AnnouncementDTO(
      startTime - 7.days,
      startTime + (duration min 5).minutes,
      s"<p>${StringEscapeUtils.escapeHtml4(message)}</p>",
      "info",
      !maintenanceWindow.disabled
    )
  end createAnnouncementDTO
end MaintenanceWindowRootApiImpl

object MaintenanceWindowRootApiImpl:
  final val HoursMsg           = I18nMessage.key("MAINTENANCE_WINDOW_ALERT_HOURS")
  final val MinutesMsg         = I18nMessage.key("MAINTENANCE_WINDOW_ALERT_MINUTES")
  final val HoursAndMinutesMsg = I18nMessage.key("MAINTENANCE_WINDOW_ALERT_HOURS_MINUTES")
  final val MaintenanceMsg     = I18nMessage.key("MAINTENANCE_WINDOW_ALERT_MESSAGE")
