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

package loi.cp.course.notification

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.cpxp.scala.json.JEnumCodec
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.course.right.TeachCourseRight
import loi.cp.notification.NotificationUrgency
import loi.cp.reference.EdgePath

import scalaz.\/

@Controller(root = true)
trait InstructorNotificationWebController extends ApiRootComponent:

  @RequestMapping(path = "lwc/{context}/instructorNotification", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def notify(
    @RequestBody request: ArgoBody[InstructorNotificationRequest],
    @SecuredAdvice @PathVariable("context") context: Long
  ): ErrorResponse \/ Unit

case class InstructorNotificationRequest(
  edgePath: EdgePath,
  message: String,
  notifiedUserIds: List[Long],
  urgency: NotificationUrgency
)

object InstructorNotificationRequest:
  implicit val codec: CodecJson[InstructorNotificationRequest] =
    implicit val notificationUrgencyCodec: CodecJson[NotificationUrgency] = JEnumCodec.jenumCodec[NotificationUrgency]
    CodecJson.derive[InstructorNotificationRequest]
