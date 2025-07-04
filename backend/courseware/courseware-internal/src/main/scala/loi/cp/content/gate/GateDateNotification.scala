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

package loi.cp.content.gate

import java.time.Instant
import java.util.Date
import java.lang as jl

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import loi.cp.content.CourseContent
import loi.cp.course.CourseEnrollmentService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation}
import loi.cp.reference.EdgePath
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*

@Schema("gateDateNotification")
trait GateDateNotification extends Notification:
  type Init = GateDateNotificationInit

  @JsonProperty
  def getContentId: EdgePath

  @JsonProperty
  def getTitle: String

@Component
class GateDateNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(
  contentGateOverrideService: ContentGateOverrideService,
  courseEnrollmentService: CourseEnrollmentService
)(implicit cs: ComponentService)
    extends GateDateNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: GateDateNotificationInit): Unit =
    self.setContext(Some(init.course))
    self.setTime(Date.from(init.instant))
    self.setData(GateDateNotificationData(init.user, init.content.edgePath, init.content.title))
    self.setTopic(Option(init.content.edgePath).map(_.toString))

  override def getContentId: EdgePath = data.edgePath

  override def getTitle: String = data.title

  override def audience: Iterable[Long] =
    data.user.cata(u => Iterable(u.longValue), getContext.cata(students, Iterable.empty))

  private def students(course: Long): Iterable[Long] =
    val contentId     = getContentId
    val lwc           = course.component[LightweightCourse]
    val gateOverrides = contentGateOverrideService.loadOverrides(lwc).get
    // suppress notifications for gates that have been forced open by the instructor
    gateOverrides.overall.contains(contentId) !?
      courseEnrollmentService
        .getEnrolledStudentDTOs(course)
        .filterNot(u => gateOverrides.perUser.get(u.id).exists(_.contains(contentId)))
        .map(_.id)
  end students

  override def aggregationKey: Option[String] = Option(s"$schemaName:$getContentId")

  private lazy val data = self.getData[GateDateNotificationData]
end GateDateNotificationImpl

final case class GateDateNotificationInit(course: Long, user: Option[jl.Long], instant: Instant, content: CourseContent)

final case class GateDateNotificationData(user: Option[jl.Long], edgePath: EdgePath, title: String)
