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

package loi.cp.lti

import java.net.URL

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import jakarta.servlet.http.HttpServletRequest
import loi.cp.content.CourseContent
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.lwgrade.GradeColumn
import loi.cp.reference.EdgePath
import loi.cp.user.UserComponent
import scalaz.\/

@Service
trait LtiOutcomesService:
  def processLaunch(
    course: CourseSection,
    user: UserComponent,
    content: Option[CourseContent],
    systemId: Long,
    instructorLike: Boolean
  )(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Unit

  def manuallySyncColumns(lwc: CourseSection): Unit

  def manuallySyncColumn(lwc: CourseSection, column: GradeColumn): Unit

  def deleteColumn(lwc: CourseSection, edgePath: EdgePath): Unit

  def setOutcomes1Config(
    context: ContextId,
    user: UserId,
    edgePath: EdgePath,
    serviceUrl: URL,
    resultSourceDid: String,
    system: BasicLtiSystemComponent
  ): Unit

  def setAgsConfig(section: CourseSection, lineItemsUrl: URL, system: BasicLtiSystemComponent): Unit
end LtiOutcomesService
