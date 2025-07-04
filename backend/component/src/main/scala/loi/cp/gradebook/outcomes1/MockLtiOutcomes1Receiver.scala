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

package loi.cp.gradebook.outcomes1

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.de.authorization.Secured
import jakarta.servlet.http.HttpServletRequest

import scala.collection.mutable
import scala.xml.XML

@Controller(
  value = "mockOutcomes1",
  root = true,
  category = Controller.Category.CONTEXTS
)
@Component(enabled = false)
class MockLtiOutcomes1Receiver(
  val componentInstance: ComponentInstance,
  domain: DomainDTO
) extends ApiRootComponent
    with ComponentImplementation:
  import MockLtiOutcomes1Receiver.*

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "mockOutcomes1/{courseId}/{userId}", method = Method.POST, csrf = false)
  def storeCourseGrade(
    @PathVariable("courseId") courseId: CourseId,
    @PathVariable("userId") userId: UserId,
    req: HttpServletRequest
  ): Unit =
    val xml = XML.load(req.getReader)
    gradebooks.put(GradebookKey(courseId, userId, None), (xml \\ "resultScore" \\ "textString").text)

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "mockOutcomes1/{courseId}/{userId}/{activityId}", method = Method.POST, csrf = false)
  def storeGrade(
    @PathVariable("courseId") courseId: CourseId,
    @PathVariable("userId") userId: UserId,
    @PathVariable("activityId") activityId: ActivityId,
    req: HttpServletRequest
  ): Unit =
    val xml = XML.load(req.getReader)
    gradebooks.put(GradebookKey(courseId, userId, Some(activityId)), (xml \\ "resultScore" \\ "textString").text)
  end storeGrade

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "mockOutcomes1/{courseId}/{userId}", method = Method.GET)
  def getCourseGrade(
    @PathVariable("courseId") courseId: CourseId,
    @PathVariable("userId") userId: UserId,
    req: HttpServletRequest
  ): Option[String] =
    gradebooks.get(GradebookKey(courseId, userId, None))

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "mockOutcomes1/{courseId}/{userId}/{activityId}", method = Method.GET)
  def getGrade(
    @PathVariable("courseId") courseId: CourseId,
    @PathVariable("userId") userId: UserId,
    @PathVariable("activityId") activityId: ActivityId,
    req: HttpServletRequest
  ): Option[String] =
    gradebooks.get(GradebookKey(courseId, userId, Some(activityId)))
end MockLtiOutcomes1Receiver

object MockLtiOutcomes1Receiver:
  private type CourseId   = String
  private type UserId     = String
  private type ActivityId = String
  private case class GradebookKey(courseId: CourseId, userId: UserId, activityId: Option[ActivityId])
  private type Gradebook  = mutable.Map[GradebookKey, String]
  private val gradebooks: Gradebook = mutable.Map[GradebookKey, String]()
