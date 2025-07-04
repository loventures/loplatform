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

package loi.cp.discussion

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.query.{ApiOrder, ApiPage, BaseApiPage}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.cpxp.util.HtmlUtils
import loi.cp.attachment.AttachmentId
import loi.cp.content.CourseContent
import loi.cp.course.CourseSection
import loi.cp.discussion.DiscussionBoardService.*
import loi.cp.discussion.api.dto.UserPostCountDTO
import loi.cp.discussion.dto.{DiscussionSummary, FilteredSeq, Jumpbar, Post}
import loi.cp.reference.ContentIdentifier

import java.time.Instant
import scala.util.Try

@Service
trait DiscussionBoardService:

  /** @param lwc
    *   The context we are in
    * @param contents
    *   The contents of that context that we are interested in searching for discussions.
    * @return
    *   The discussions within the contents of the lwc.
    */
  def getDiscussions(section: CourseSection, contents: Seq[CourseContent]): Seq[Discussion]

  final def getDiscussion(section: CourseSection, content: CourseContent): Option[Discussion] =
    getDiscussions(section, Seq(content)).headOption

  /** @param contentId
    *   the reference to content and context of the discussion board
    * @return
    *   A discussion board from content in a course.
    */
  def getDiscussionSummary(contentId: ContentIdentifier, user: UserId): Option[DiscussionSummary] =
    getDiscussionSummaries(Seq(contentId), user).headOption.map(_._2)

  def getDiscussionSummaryForReviewer(contentId: ContentIdentifier, user: UserId): Option[DiscussionSummary] =
    getDiscussionSummariesForReviewer(Seq(contentId), user).headOption.map(_._2)

  def getDiscussionSummaries(contentId: Seq[ContentIdentifier], user: UserId): Map[ContentIdentifier, DiscussionSummary]

  def getDiscussionSummariesForReviewer(
    contentId: Seq[ContentIdentifier],
    user: UserId
  ): Map[ContentIdentifier, DiscussionSummary]

  /** @param contentId
    *   the reference to content and context of the discussion board
    * @param user
    *   who is doing the visiting
    * @param when
    *   what time should we say they came by.
    * @return
    *   the time we are considering the user to have visited the board. This is only pertinent towards figuring out the
    *   new posts.
    */
  def visitDiscussionBoard(contentId: ContentIdentifier, user: UserId, when: Instant): Instant

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param postId
    *   which post are we interested
    * @param user
    *   the user the Post will be tailored to
    * @return
    *   the post associated with a discussion and potentially some statistic data for each post tailored to the user
    *   asking.
    */
  def getPost(discussionId: ContentIdentifier, postId: PostId, user: UserId): Option[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param fetchSettings
    *   What if any stats should be collected
    * @param postIds
    *   Ids of posts to filter down to.
    * @param toDepth
    *   How far down the rabbit hole should we look
    * @param searchFor
    *   What text should we search for when filtering posts
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   the posts associated with a discussion and potentially some statistic data for each post tailored to the user
    *   asking.
    */
  def getPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]] = None,
    toDepth: Option[Long] = None,
    searchFor: Option[String] = None,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param authorId
    *   the author of the posts we are interested in
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   the visible posts for a specific author.
    */
  def getPostsForAuthor(
    discussionId: ContentIdentifier,
    userId: UserId,
    authorId: UserId,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param since
    *   the time from which we consider posts new
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   the posts in the discussion board that were updated after the since date.
    */
  def getNewPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    since: Instant,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param targetUserId
    *   Who's unread posts do we care about.
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   posts that are considered unread by the user
    */
  def getUnreadPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param targetUserId
    *   Who's bookmarked posts do we care about.
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   posts that are bookmarked by the user
    */
  def getBookmarkedPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to.
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   threads that have not been responded to yet by any moderator
    */
  def getUnrespondedThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param ancestorPostId
    *   What is the common ancestor id?
    * @param fetchSettings
    *   What statistics should we include with this fetch
    * @param postIds
    *   Should we filter to specific targeted posts
    * @param toDepth
    *   What depth do we need to fetch to
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    *   descendant posts
    */
  def getDescendantPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    ancestorPostId: PostId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]] = None,
    toDepth: Option[Long] = None,
    searchFor: Option[String] = None,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param threadIds
    *   Which threads do we want to fetch all the replies to?
    * @param includeHidden
    *   Should removed or inappropriate replies be included
    * @param apiOrders
    *   ordering support on a per thread basis
    * @return
    *   a set of posts mapped to their parent thread.
    */
  def getDescendantPostMappedToThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    threadIds: Seq[PostId],
    includeHidden: Boolean,
    apiOrders: Seq[ApiOrder] = Seq.empty
  ): Map[PostId, FilteredSeq[Post]]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param userId
    *   the user the Post may be tailored to
    * @param fetchSettings
    *   what kind of statistics to include
    * @param postIds
    *   should this be limited to a smaller set of known threads
    * @param apiOrders
    *   ordering support
    * @param apiPage
    *   paging support
    * @return
    */
  def getThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]] = None,
    searchFor: Option[String] = None,
    apiOrders: Seq[ApiOrder] = Seq.empty,
    apiPage: ApiPage = BaseApiPage.DEFAULT_PAGE
  ): FilteredSeq[Post]

  def createPost(discussion: Discussion, createWith: CreatePostRequest): Try[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param updateWith
    *   how should the post be updated
    * @return
    *   the updated post
    */
  def updatePostContent(discussionId: ContentIdentifier, updateWith: UpdatePostRequest): Try[Post]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param postId
    *   the post to mark as inappropriate
    * @param userId
    *   who is marking this as inappropriate
    * @param inappropriate
    *   mark it inappropriate or remove that marking
    * @return
    *   inappropriate if successful
    */
  def updatePostInappropriate(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    inappropriate: Boolean
  ): Try[Boolean]

  /** This is a side-effectly thing that creates a notification for a moderator to operate off of.
    * @param discussionId
    *   which discussion board does the post reside in.
    * @param postId
    *   the post to report as inappropriate
    * @param userId
    *   who is reporting it
    * @param reason
    *   what reason are they giving
    */
  def reportPostInappropriate(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    reason: String
  ): Try[Unit]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param postId
    *   the post to report as inappropriate
    * @param userId
    *   who is pinning it
    * @param pinned
    *   None to unpin, otherwise pinned per some given date.
    * @return
    *   when was this post pinned
    */
  def updatePostPinned(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    pinned: Option[Instant]
  ): Try[Option[Instant]]

  /** @param discussionId
    *   which discussion board does the post reside in.
    * @param postId
    *   the post to report as inappropriate
    * @param userId
    *   who is pinning it
    * @param removed
    *   to remove or not to remove
    * @param onlyOriginalUser
    *   validate that the original user is removing this.
    * @return
    */
  def updatePostRemoved(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    removed: Boolean,
    onlyOriginalUser: Boolean = true
  ): Try[Boolean]

  // User interaction overlays
  def updatePostBookmarked(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    bookmarked: Boolean
  ): Try[Boolean]

  def updatePostViewed(discussionId: ContentIdentifier, postId: PostId, userId: UserId, viewed: Boolean): Try[Boolean]

  def updatePostFavorited(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    favorited: Boolean
  ): Try[Boolean]

  def getJumpbarSummary(discussionId: ContentIdentifier, userId: UserId, settings: JumpbarSettings): Jumpbar

  def getPostCountForUsers(discussionId: ContentIdentifier): Seq[UserPostCountDTO]
end DiscussionBoardService

object DiscussionBoardService:

  def filterDiscussionContent(contents: Seq[CourseContent]): Seq[CourseContent] =
    contents.filter(content => DiscussionContent(content.asset).isDefined)

  case class CreatePostRequest(
    user: UserDTO,
    moderator: Boolean,
    parentPostId: Option[PostId],
    createTime: Instant,
    title: Option[String],
    content: String,
    uploads: Option[List[UploadInfo]]
  )

  case class UpdatePostRequest(
    postId: PostId,
    userId: UserId,
    moderator: Boolean,
    title: Option[String],
    content: String,
    attachmentsToKeep: Option[List[AttachmentId]],
    newUploads: Option[List[UploadInfo]]
  )

  /** @param includeHidden
    *   should we keep removed or inappropriate posts in statistic counts
    * @param includeDescendantCount
    *   count how many descendants are there for this post
    * @param includeNewDescendantSinceCount
    *   If not none, include a count of posts updates since this date
    * @param includeUnreadDescendantCount
    *   count how many unread descendants posts there are, expensive.
    */
  case class StatisticFetchSetting(
    includeHidden: Boolean = false,
    includeDescendantCount: Boolean = false,
    includeNewDescendantSinceCount: Option[Instant] = None,
    includeUnreadDescendantCount: Boolean = false
  )

  case class JumpbarSettings(
    userPosts: Option[UserId],
    bookmarkedPosts: Boolean,
    newPosts: Option[Instant],
    unrespondedThreads: Boolean,
    unreadPosts: Boolean
  )

  def searchableContent(
    title: Option[String],
    content: String,
    fullName: String
  ): String =
    s"$fullName ${HtmlUtils.toPlaintext(title.getOrElse(""))} ${HtmlUtils.toPlaintext(content)}"
end DiscussionBoardService
