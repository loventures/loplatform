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
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, FileResponse, Method}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JList, JLong}
import com.learningobjects.cpxp.util.FileInfo
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.attachment.AttachmentId
import loi.cp.course.right.{FullContentRight, InteractCourseRight, ReadCourseRight, TeachCourseRight}
import loi.cp.discussion.PostId
import loi.cp.discussion.api.dto.PostDTO
import loi.cp.discussion.dto.FilteredSeq
import loi.cp.reference.ContentIdentifier
import scalaz.\/

import scala.util.Try

@Controller(root = true, value = "discussion2Post")
@RequestMapping(path = "discussion/posts")
trait DiscussionPostWebController extends ApiRootComponent:

  /** Retrieve all posts associated with a discussion board, pursuant to various filters. Normal use cases would be:
    *
    *   - Set toDepth to 0 in order to get all root posts.
    *   - Set rootPostId to some known post id to get all descendants of that post.
    *
    * Usually you would not fetch all posts generally nor would you use toDepth and rootPostId together (though using
    * those together you can do something nifty like only get immediate children of a post)
    *
    * @param discussion
    *   Which discussion are we interested in.
    * @param toDepth
    *   How deeply nested should posts returned be. Depth of 0 implies root threads only. No depth set implies as deep
    *   as possible. Depth is an absolute value from the root of the discussion
    * @param rootPostId
    *   What common ancestor should we filter our posts to. Exclusive of root post.
    * @param includeCounts
    *   Should we include child post counts? If this is not included, we will guess that you want them if depth is 0
    *   otherwise, you don't
    * @param previousVisitTime
    *   when considering whether a post is new, use this timestamp.
    * @param postIds
    *   If defined, only consider posts with these ids
    * @param apiQuery
    *   Query support, Only pagination and sort order is supported
    * @return
    *   All the posts that meet the filtering criteria.
    */
  @RequestMapping(method = Method.GET)
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getPostsForDiscussion(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @MatrixParam(value = "toDepth", required = false) toDepth: Option[JLong],
    @MatrixParam(value = "rootPostId", required = false) rootPostId: Option[JLong],
    @MatrixParam(value = "includeCounts", required = false) includeCounts: Option[JBoolean],
    @MatrixParam(value = "previousVisit", required = false) previousVisitTime: Option[Instant],
    @MatrixParam(value = "postIds", required = false) postIds: JList[JLong],
    @MatrixParam(value = "searchFor", required = false) searchFor: Option[String],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Jumpbar support to get specific user posts
    * @param discussion
    *   Which discussion are we interested in
    * @param userHandle
    *   The masked id of whose posts do we care about
    * @param apiQuery
    *   Query support, only pagination and sorting is supported
    * @return
    */
  @RequestMapping(method = Method.GET, path = "forUserHandle/{userHandle}")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getUserPosts(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @PathVariable(value = "userHandle") userHandle: String,
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Fetch unread posts for a target user or current. Primarily for jumpbar support to cycle through unread posts.
    * @param discussion
    *   Which discussion are we interested in
    * @param targetUserId
    *   Current user if None, otherwise use this user if the current user is a moderator.
    * @param apiQuery
    *   Query support, only pagination and sorting is supported
    */
  @RequestMapping(method = Method.GET, path = "unread")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getUnreadPosts(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @QueryParam(value = "userId", required = false) targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Fetch new posts (based on lastVisitedDate) for a target user or current. Primarily for jumpbar support to cycle
    * through new posts.
    * @param discussion
    *   Which discussion are we interested in
    * @param previousVisit
    *   when considering whether a post is new, use this timestamp.
    * @param targetUserId
    *   Current user if None, otherwise use this user if the current user is a moderator.
    * @param apiQuery
    *   Query support, only pagination and sorting is supported
    */
  @RequestMapping(method = Method.GET, path = "new")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getNewPosts(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @MatrixParam(value = "previousVisit", required = true) previousVisit: Instant,
    @QueryParam(value = "userId", required = false) targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Jumpbar support to fetch bookmarked posts.
    * @param discussion
    *   Which discussion are we interested in
    * @param apiQuery
    *   Query support, only pagination and sorting is supported
    */
  @RequestMapping(method = Method.GET, path = "bookmarked")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getBookmarkedPosts(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @QueryParam(value = "userId", required = false) targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Jumpbar support to fetch unresponded posts.
    * @param discussion
    *   Which discussion are we interested in
    * @param apiQuery
    *   Query support, only pagination and sorting is supported
    */
  @RequestMapping(method = Method.GET, path = "unresponded")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getUnrespondedThreads(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @QueryParam(value = "userId", required = false) targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]]

  /** Front-end specific implementation to fetch of child discussion posts for a set of threads. Specifically for
    * initial load.
    *
    * TODO: Update the frontend to take advantage of this. Currently a premature optimization. This is really only
    * appropriate if we don't have a lot of replies per thread...otherwise we should probably fall back on per-thread
    * requests. Thankfully, we should get a summary of exactly how many descendants each thread has and can
    * subsequentially do that fall back if need be.
    */
  @RequestMapping(method = Method.GET, path = "threads")
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def getRepliesForThread(
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @MatrixParam(value = "threadIds", required = true) postIds: JList[JLong],
    apiQuery: ApiQuery
  ): Try[Map[PostId, FilteredSeq[PostDTO]]]

  /** Create a post for the current user
    *
    * @param post
    *   the info for the post to create
    * @return
    *   the post that was created.
    */
  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[FullContentRight]))
  def createPost(
    @SecuredAdvice @MatrixParam(value = "discussion") discussion: ContentIdentifier,
    @RequestBody post: PostRequest.CreatePost
  ): Try[PostDTO]

  /** Update post content
    *
    * @param postId
    *   the post to update
    * @param request
    *   what to update the content to
    * @return
    *   the post that was updated.
    */
  @RequestMapping(method = Method.PUT, path = "{postId}")
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[FullContentRight]))
  def updatePost(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.UpdatePost
  ): Try[PostDTO]

  /** Toggle the pinned state of a post.
    *
    * @param postId
    *   what post to pin
    * @param request
    *   should the post be pinned or not.
    */
  @RequestMapping(path = "{postId}/pin", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def togglePin(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.ToggleState
  ): Unit

  /** Toggle the inappropriate state of a post.
    *
    * @param postId
    *   what post to mark as inappropriate
    * @param request
    *   should the post toggled inappropriate.
    */
  @RequestMapping(path = "{postId}/inappropriate", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def toggleInappropriate(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.ToggleState
  ): Unit

  /** Notify instructors of an inappropriate post.
    *
    * @param postId
    *   what post to mark as inappropriate
    * @param request
    *   reason for the inappropriate request.
    */
  @RequestMapping(path = "{postId}/reportInappropriate", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def reportInappropriate(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.InappropriateReason
  ): Unit

  /** Toggle the removed state of a post.
    *
    * @param postId
    *   what post to removed
    * @param request
    *   should the post be removed or not.
    */
  @RequestMapping(path = "{postId}/remove", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  def toggleRemoved(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.ToggleState
  ): Unit

  /** Toggle the bookmarked state of a post. Assumes the bookmark is for the current user.
    *
    * @param postId
    *   what post to bookmark
    * @param request
    *   should the post be bookmarked or not.
    */
  @RequestMapping(path = "{postId}/bookmark", method = Method.POST)
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def toggleBookmark(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.ToggleState
  ): Unit

  /** Toggle the viewed state of a post. Assumes the viewed state is for the current user.
    *
    * @param postId
    *   what post to mark viewed.
    * @param request
    *   should the post be marked viewed.
    */
  @RequestMapping(path = "{postId}/viewed", method = Method.POST)
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def toggleViewed(
    @PathVariable("postId") postId: PostId,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @RequestBody request: PostRequest.ToggleState
  ): Unit

  /** Retrieves an attachment found in a discussion post.
    *
    * @param postId
    *   the discussion post containing the attachment
    * @param discussion
    *   the discussion board containing the attachment
    * @param attachmentId
    *   the id of the attachment
    * @param download
    *   whether the client requests to download the attachment
    * @param direct
    *   whether or not a CDN should be used
    * @param size
    *   the specified size, if any
    * @return
    */
  @RequestMapping(path = "{postId}/attachments/{attachmentId}", method = Method.GET)
  @Secured(
    Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight], classOf[ReadCourseRight])
  )
  def attachment(
    @PathVariable("postId") postId: Long,
    @SecuredAdvice @MatrixParam("discussion") discussion: ContentIdentifier,
    @PathVariable("attachmentId") attachmentId: AttachmentId,
    @QueryParam(value = "download", required = false) download: Option[JBoolean],
    @QueryParam(value = "direct", required = false) direct: Option[JBoolean],
    @QueryParam(value = "size", required = false) size: String
  ): ErrorResponse \/ FileResponse[? <: FileInfo]
end DiscussionPostWebController
