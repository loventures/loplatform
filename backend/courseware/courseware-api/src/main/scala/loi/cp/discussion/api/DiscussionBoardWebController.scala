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

import java.time.Instant

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.context.ContextId
import loi.cp.course.right.{InteractCourseRight, ReadCourseRight, TeachCourseRight}
import loi.cp.discussion.api.DiscussionBoardWebController.*
import loi.cp.discussion.api.dto.{DiscussionDTO, DiscussionDetailsDTO, JumpbarDTO, UserPostCountDTO}
import loi.cp.reference.{ContentIdentifier, EdgePath}

import scala.util.Try
@Controller(root = true, value = "discussion2Board")
@RequestMapping(path = "discussion/boards")
trait DiscussionBoardWebController extends ApiRootComponent:

  /** Get all discussion boards in a specific context
    *
    * @param contextId
    *   context we are interested in
    * @param summarize
    *   Should there be summary statistics embeded
    * @param apiQuery
    *   paging/filtering support
    * @return
    *   the matching discussions
    */
  @RequestMapping(method = Method.GET)
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getDiscussions(
    @SecuredAdvice @MatrixParam(value = "context") contextId: ContextId,
    @MatrixParam(value = "summarize") summarize: Option[JBoolean],
    @QueryParam(value = "userId", required = false) targetUser: Option[JLong],
    apiQuery: ApiQuery
  ): Try[ApiQueryResults[DiscussionDTO]]

  /** Get information about a specific discussion board with details such as instructions and rubrics
    *
    * @param discussion
    *   what discussion board is being visited
    * @return
    *   the discussion with the requested embedded information
    */
  @RequestMapping(method = Method.GET, path = "{discussion}")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getDiscussion(
    @SecuredAdvice @PathVariable("discussion") discussion: ContentIdentifier,
    @QueryParam(value = "userId", required = false) targetUser: Option[JLong]
  ): Try[DiscussionDetailsDTO]

  /** @param discussion
    *   what discussion board is being visited
    * @param includeUserPosts
    *   include user posts with given handle if not None. Will fall back to current user if handle is invalid.
    * @param includeBookmarkedPosts
    *   Include posts that have been bookmarked by the target user
    * @param includeNewPostFrom
    *   Include new posts for target user if not none from date given
    * @param includeUnreadPosts
    *   Include posts that have not been read by the target user if not None
    * @param includeUnrespondedToThreads
    *   Include threads that have not been responded to by a moderator=
    * @param targetUser
    *   target user to scope posts returned to, Current user will be used if this is none.
    * @return
    *   The jumpbar for a specific discussion board.
    */
  @RequestMapping(method = Method.GET, path = "{discussion}/jumpbarSummary")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getJumpbarSummary(
    @SecuredAdvice @PathVariable("discussion") discussion: ContentIdentifier,
    @MatrixParam(value = "userPosts", required = false) includeUserPosts: Option[String],
    @MatrixParam(value = "bookmarkedPosts", required = false) includeBookmarkedPosts: Option[JBoolean],
    @MatrixParam(value = "newPosts", required = false) includeNewPostFrom: Option[Instant],
    @MatrixParam(value = "unreadPosts", required = false) includeUnreadPosts: Option[JBoolean],
    @MatrixParam(value = "unrespondedThreads", required = false) includeUnrespondedToThreads: Option[JBoolean],
    @QueryParam(value = "userId", required = false) targetUser: Option[JLong]
  ): Try[JumpbarDTO]

  /** Returns a map of post counts for all users active in this discussion board.
    * @param discussion
    *   the discussion board we want this breakdown for.
    * @return
    *   a map of user id to post count
    */
  @RequestMapping(method = Method.GET, path = "{discussion}/userPostCount")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getUserPostCounts(@SecuredAdvice @PathVariable("discussion") discussion: ContentIdentifier): Seq[UserPostCountDTO]

  /** Notify the server that the current user has explicitly visited a discussion board.
    *
    * @param discussion
    *   what discussion board is being visited
    */
  @RequestMapping(method = Method.POST, path = "{discussion}/visit")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def visit(@SecuredAdvice @PathVariable("discussion") discussion: ContentIdentifier): Instant

  /** Close or open discussions.
    *
    * @param contextId
    *   which course
    * @param request
    *   what and how do we want to change things.
    */
  @RequestMapping(method = Method.POST, path = "close")
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def closeDiscussions(
    @SecuredAdvice @MatrixParam(value = "context") contextId: ContextId,
    @RequestBody request: DiscussionAccessChange
  ): Unit
end DiscussionBoardWebController

object DiscussionBoardWebController:
  case class DiscussionAccessChange(discussions: Map[EdgePath, JBoolean])
