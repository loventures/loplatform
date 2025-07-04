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

package loi.cp.discussion.api

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.context.ContextId
import loi.cp.course.right.TeachCourseRight

import java.util.Date
import scala.util.Try

@Controller(root = true, value = "discussionPurge")
@RequestMapping(path = "discussion/purge")
trait DiscussionPurgeWebController extends ApiRootComponent:

  @RequestMapping(method = Method.POST, path = "{section}/dry")
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def dryRunPurgePosts(
    @SecuredAdvice @PathVariable("section") section: ContextId,
    @RequestBody body: PurgeRequestBody
  ): Try[Map[String, PurgeApiResponse]]

  @RequestMapping(method = Method.POST, path = "{section}")
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def purgePosts(
    @SecuredAdvice @PathVariable("section") section: ContextId,
    @RequestBody body: PurgeRequestBody
  ): Try[Map[String, PurgeApiResponse]]
end DiscussionPurgeWebController

case class PurgeApiResponse(numberOfPosts: Int, title: String, delGuid: Option[String] = None)

case class PurgeRequestBody(date: Date, timezoneOffset: String)
