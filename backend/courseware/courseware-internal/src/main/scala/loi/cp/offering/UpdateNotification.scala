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

package loi.cp.offering

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation}
import scaloi.syntax.CollectionBoxOps.*

/** Notification to an instructor that curriculum updates have been made to their course. */
@Schema("updateNotification")
trait UpdateNotification extends Notification:
  type Init = UpdateNotificationData

  @JsonProperty def course: LightweightCourse
  @JsonProperty def message: String

@Component
class UpdateNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(implicit
  enrollmentWebService: EnrollmentWebService,
  facadeService: FacadeService,
  cs: ComponentService
) extends UpdateNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: UpdateNotificationData): Unit =
    self.setTime(new Date)
    self.setContext(Some(init.course))
    self.setData(init)

  override def audience: Iterable[Long] =
    enrollmentWebService
      .getGroupActiveUserIdsByRoleName(data.course, EnrollmentWebService.ROLE_INSTRUCTOR_NAME)
      .unboxInsideTo[Seq]()

  override def aggregationKey: Option[String] = Some(s"Update-${data.course}")

  override def course: LightweightCourse = data.course.component[LightweightCourse]

  override def message: String = data.message

  private lazy val data = self.getData[UpdateNotificationData]
end UpdateNotificationImpl

final case class UpdateNotificationData(course: Long, message: String)
