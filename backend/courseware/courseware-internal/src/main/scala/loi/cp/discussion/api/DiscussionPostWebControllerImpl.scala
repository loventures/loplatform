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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSource}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JList, JLong}
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.{UserDTO, UserWebService}
import com.learningobjects.cpxp.util.FileInfo
import loi.cp.attachment.AttachmentId
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.discussion.DiscussionBoardService.StatisticFetchSetting
import loi.cp.discussion.api.dto.PostDTO
import loi.cp.discussion.attachment.DiscussionAttachmentService
import loi.cp.discussion.dto.{FilteredSeq, Post}
import loi.cp.discussion.user.DiscussionUserRightsService
import loi.cp.discussion.{DiscussionBoardService, PostId}
import loi.cp.reference.ContentIdentifier
import loi.cp.storage.CourseStorageService
import loi.cp.web.HandleService
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*

import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

@Component
class DiscussionPostWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  discussionBoardService: DiscussionBoardService,
  discussionUserRightsService: DiscussionUserRightsService,
  handleService: HandleService,
  userWebService: UserWebService,
  currentUser: => UserDTO,
  discussionAttachmentService: DiscussionAttachmentService,
  time: TimeSource
)(implicit cs: ComponentSource, courseStorageService: CourseStorageService)
    extends DiscussionPostWebController
    with ComponentImplementation:
  import DiscussionHelpers.*

  override def getPostsForDiscussion(
    discussionId: ContentIdentifier,
    toDepth: Option[JLong],
    rootPostId: Option[JLong],
    includeCounts: Option[JBoolean],
    previousVisit: Option[Instant],
    postIds: JList[JLong],
    searchFor: Option[String],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    val currentIsReviewer: Boolean = canModerate(discussionId)

    val specificPostFilter: Option[Seq[Long]] =
      Option(postIds).filterNot(_.isEmpty).map(_.asScala.toSeq.map(Long2long))

    val includeStats: Boolean = includeCounts.exists(_.booleanValue)

    val depth: Option[Long] = toDepth.map(Long2long)

    val statSettings: StatisticFetchSetting =
      statisticsSettings(currentIsReviewer, searchFor.isDefined, includeStats, previousVisit)

    val posts =
      if rootPostId.isDefined then
        // Descendant specific fetch
        discussionBoardService
          .getDescendantPosts(
            discussionId,
            currentUser,
            rootPostId.get,
            statSettings,
            specificPostFilter,
            depth,
            searchFor,
            apiQuery.getOrders.asScala.toSeq,
            apiQuery.getPage
          )
      else if depth.contains(0) then
        // Thread specific fetch
        discussionBoardService
          .getThreads(
            discussionId,
            currentUser,
            statSettings,
            specificPostFilter,
            searchFor,
            apiQuery.getOrders.asScala.toSeq,
            apiQuery.getPage
          )
      else
        // Generic fetch posts
        discussionBoardService
          .getPosts(
            discussionId,
            currentUser,
            statSettings,
            specificPostFilter,
            depth,
            searchFor,
            apiQuery.getOrders.asScala.toSeq,
            apiQuery.getPage
          )

    for
      section <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed   = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
    yield toPostDTOs(
      discussionId,
      currentUser.id,
      closed,
      currentIsReviewer,
      posts
    )
    end for
  end getPostsForDiscussion

  override def getUserPosts(
    discussionId: ContentIdentifier,
    userHandle: String,
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    for
      section      <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      targetUserId <- handleService
                        .unmask(userHandle)
                        .toTry(new IllegalArgumentException(s"handle doesn't exist $userHandle"))
      closed        = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      targetUser    = userWebService.getUserDTO(targetUserId)
      isModerator   = canModerate(discussionId)
      posts         =
        discussionBoardService
          .getPostsForAuthor(discussionId, currentUser, targetUser, apiQuery.getOrders.asScala.toSeq, apiQuery.getPage)
    yield toPostDTOs(
      discussionId,
      currentUser.id,
      closed,
      isModerator,
      posts
    )

  override def getUnreadPosts(
    discussionId: ContentIdentifier,
    targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    val targetUser: UserDTO = targetUserId.fold(currentUser)(id => userWebService.getUserDTO(id))
    for
      isModerator <- allowEditAsSelfOrModerator(discussionId, targetUser)
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
    yield
      val closed = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      val posts  = discussionBoardService.getUnreadPosts(
        discussionId,
        currentUser,
        targetUser,
        apiQuery.getOrders.asScala.toSeq,
        apiQuery.getPage
      )
      toPostDTOs(
        discussionId,
        currentUser.id,
        closed,
        isModerator,
        posts
      )
    end for
  end getUnreadPosts

  override def getNewPosts(
    discussionId: ContentIdentifier,
    previousVisit: Instant,
    targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    val targetUser: UserDTO = targetUserId.fold(currentUser)(id => userWebService.getUserDTO(id))

    for
      isModerator <- allowEditAsSelfOrModerator(discussionId, targetUser)
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed       = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      posts        = discussionBoardService.getNewPosts(
                       discussionId,
                       targetUser,
                       previousVisit,
                       apiQuery.getOrders.asScala.toSeq,
                       apiQuery.getPage
                     )
    yield toPostDTOs(
      discussionId,
      currentUser.id,
      closed,
      isModerator,
      posts
    )
    end for
  end getNewPosts

  override def getBookmarkedPosts(
    discussionId: ContentIdentifier,
    targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    val targetUser: UserDTO = targetUserId.fold(currentUser)(id => userWebService.getUserDTO(id))

    for
      isModerator <- allowEditAsSelfOrModerator(discussionId, targetUser)
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed       = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      posts        = discussionBoardService.getBookmarkedPosts(
                       discussionId,
                       currentUser,
                       targetUser,
                       apiQuery.getOrders.asScala.toSeq,
                       apiQuery.getPage
                     )
    yield toPostDTOs(
      discussionId,
      currentUser.id,
      closed,
      isModerator,
      posts
    )
    end for
  end getBookmarkedPosts

  override def getUnrespondedThreads(
    discussionId: ContentIdentifier,
    targetUserId: Option[JLong],
    apiQuery: ApiQuery
  ): Try[FilteredSeq[PostDTO]] =
    val targetUser: UserDTO = targetUserId.fold(currentUser)(id => userWebService.getUserDTO(id))

    for
      isModerator <- allowEditAsSelfOrModerator(discussionId, targetUser, onlyModerator = true)
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed       = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      posts        = discussionBoardService.getUnrespondedThreads(
                       discussionId,
                       targetUser,
                       apiQuery.getOrders.asScala.toSeq,
                       apiQuery.getPage
                     )
    yield toPostDTOs(
      discussionId,
      currentUser.id,
      closed,
      isModerator,
      posts
    )
    end for
  end getUnrespondedThreads

  def getRepliesForThread(
    discussionId: ContentIdentifier,
    threadIds: JList[JLong],
    apiQuery: ApiQuery
  ): Try[Map[PostId, FilteredSeq[PostDTO]]] =
    val currentIsReviewer: Boolean = canModerate(discussionId)

    for
      section <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed   = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      replies  = discussionBoardService
                   .getDescendantPostMappedToThreads(
                     discussionId,
                     currentUser,
                     threadIds.asScala.toSeq.map(Long2long),
                     currentIsReviewer,
                     apiQuery.getOrders.asScala.toSeq
                   )
    yield for (threadId, posts) <- replies
    yield threadId -> toPostDTOs(discussionId, currentUser.id, closed, currentIsReviewer, posts)
    end for
  end getRepliesForThread

  private def statisticsSettings(
    currentIsReviewer: Boolean,
    hasSearch: Boolean,
    includeStats: Boolean,
    previousVisit: Option[Instant]
  ): StatisticFetchSetting =
    val includeNewPostCountFromTime: Option[Instant] =
      (!currentIsReviewer && includeStats).flatOption(previousVisit.orElse(Some(Instant.EPOCH)))
    val includeUnreadPostCount: Boolean              = currentIsReviewer && includeStats
    val includeHidden: Boolean                       = currentIsReviewer && !hasSearch
    StatisticFetchSetting(includeHidden, includeStats, includeNewPostCountFromTime, includeUnreadPostCount)
  end statisticsSettings

  override def createPost(discussionId: ContentIdentifier, post: PostRequest.CreatePost): Try[PostDTO] =

    val section    = courseWebUtils.sectionOrThrow404(discussionId.contextId.value)
    val discussion = courseWebUtils.discussionOrThrow404(section, discussionId.edgePath)

    for
      isModerator <- allowCreate(discussionId, discussion.closed)
      request      = DiscussionBoardService.CreatePostRequest(
                       currentUser,
                       isModerator,
                       post.parentPostId.map(_.toLong),
                       time.instant,
                       post.title,
                       post.content,
                       post.uploads
                     )
      newPost     <- discussionBoardService.createPost(discussion, request)
    yield toPostDto(
      discussionId,
      newPost,
      currentUser.id,
      discussion.closed,
      isModerator,
      discussionAttachmentService.loadAttachmentInfos(discussionId, newPost.attachmentIds)
    )
    end for
  end createPost

  override def updatePost(
    postId: PostId,
    discussionId: ContentIdentifier,
    post: PostRequest.UpdatePost
  ): Try[PostDTO] =
    for
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed       = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      isModerator <- allowCreate(discussionId, closed)
      request      = DiscussionBoardService.UpdatePostRequest(
                       postId,
                       currentUser,
                       isModerator,
                       post.title,
                       post.content,
                       post.attachments,
                       post.uploads
                     )
      updatedPost <- discussionBoardService.updatePostContent(discussionId, request)
    yield toPostDto(
      discussionId,
      updatedPost,
      currentUser.id,
      closed,
      isModerator,
      discussionAttachmentService.loadAttachmentInfos(discussionId, updatedPost.attachmentIds)
    )

  override def togglePin(
    postId: PostId,
    discussionId: ContentIdentifier,
    request: PostRequest.ToggleState
  ): Unit =
    val postPinTime: Option[Instant] = request.newState.flatOption(Some(time.instant))
    discussionBoardService
      .updatePostPinned(discussionId, postId, currentUser, postPinTime)
      .get

  override def toggleInappropriate(
    postId: PostId,
    discussionId: ContentIdentifier,
    request: PostRequest.ToggleState
  ): Unit =
    discussionBoardService
      .updatePostInappropriate(discussionId, postId, currentUser, request.newState)
      .get

  def reportInappropriate(
    postId: PostId,
    discussion: ContentIdentifier,
    request: PostRequest.InappropriateReason
  ): Unit =
    discussionBoardService
      .reportPostInappropriate(discussion, postId, currentUser, request.reason)
      .get

  override def toggleRemoved(
    postId: PostId,
    discussionId: ContentIdentifier,
    request: PostRequest.ToggleState
  ): Unit =
    discussionBoardService
      .updatePostRemoved(
        discussionId,
        postId,
        currentUser,
        request.newState,
        !canModerate(discussionId)
      )
      .get

  override def toggleBookmark(
    postId: PostId,
    discussionId: ContentIdentifier,
    request: PostRequest.ToggleState
  ): Unit =
    discussionBoardService
      .updatePostBookmarked(discussionId, postId, currentUser, request.newState)
      .get

  override def toggleViewed(
    postId: PostId,
    discussionId: ContentIdentifier,
    request: PostRequest.ToggleState
  ): Unit =
    discussionBoardService
      .updatePostViewed(discussionId, postId, currentUser, request.newState)
      .get

  override def attachment(
    postId: PostId,
    discussion: ContentIdentifier,
    attachmentId: AttachmentId,
    download: Option[JBoolean],
    direct: Option[JBoolean],
    size: String
  ): ErrorResponse \/ FileResponse[? <: FileInfo] =
    for
      post     <- getVisiblePost(postId, discussion) \/> ErrorResponse.notFound
      response <- discussionAttachmentService.buildFileResponse(
                    discussion,
                    post.id,
                    attachmentId,
                    download.isTrue,
                    direct.isTrue,
                    size
                  )
    yield response

  // This makes no effort to check whether the discussion itself is visible; that seems not to concern discussions 2.0?
  def getVisiblePost(postId: PostId, discussion: ContentIdentifier): Option[Post] =
    discussionBoardService.getPost(discussion, postId, currentUser) filter { post =>
      (!post.removed && !post.inappropriate) || (currentUser.id == post.authorId.value || canModerate(discussion))
    }

  def allowCreate(discussionId: ContentIdentifier, discussionClosed: Boolean): Try[Boolean] =
    val isModerator = canModerate(discussionId)
    if isModerator || !discussionClosed then Success(isModerator)
    else Failure(new AccessForbiddenException(s"Not allowed to update post in $discussionId"))

  def allowEditAsSelfOrModerator(
    discussionId: ContentIdentifier,
    targetUser: UserDTO,
    onlyModerator: Boolean = false
  ): Try[Boolean] =
    val isModerator = canModerate(discussionId)
    if isModerator || (!onlyModerator && targetUser == currentUser) then Success(isModerator)
    else Failure(new AccessForbiddenException(s"Not allowed to perform operation in $discussionId as ${targetUser.id}"))

  def canModerate(discussion: ContentIdentifier): Boolean =
    discussionUserRightsService.userHasModeratorRight(currentUser, ContextId(discussion.contextId.value))

  private def toPostDTOs(
    discussionId: ContentIdentifier,
    currentUserId: Long,
    closed: Boolean,
    moderator: Boolean,
    posts: FilteredSeq[Post]
  ): FilteredSeq[PostDTO] =
    val attachmentIds   = posts.partialResults.flatMap(_.attachmentIds)
    val attachmentsById = discussionAttachmentService
      .loadAttachmentInfos(discussionId, attachmentIds)
      .map(a => (a.id, a))
      .toMap
    FilteredSeq(
      posts.total,
      posts.offset,
      posts.partialResults
        .map(p =>
          toPostDto(discussionId, p, currentUserId, closed, moderator, p.attachmentIds.map(id => attachmentsById(id)))
        )
    )
  end toPostDTOs
end DiscussionPostWebControllerImpl

object DiscussionPostWebControllerImpl:
  private val log = org.log4s.getLogger
