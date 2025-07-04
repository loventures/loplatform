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
import com.learningobjects.cpxp.component.discussion.*
import com.learningobjects.cpxp.component.query.{ApiOrder, ApiPage, BaseApiOrder, BaseApiPage, OrderDirection}
import com.learningobjects.cpxp.component.web.HttpResponseException
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, BusinessRuleViolationException}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.analytics.CoursewareAnalyticsServiceImpl
import loi.cp.attachment.{AttachmentId, InvalidUploads}
import loi.cp.config.ConfigurationService
import loi.cp.content.CourseContent
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection}
import loi.cp.discussion.DiscussionBoardService.{JumpbarSettings, StatisticFetchSetting, UpdatePostRequest}
import loi.cp.discussion.api.dto.UserPostCountDTO
import loi.cp.discussion.attachment.DiscussionAttachmentService
import loi.cp.discussion.dto.*
import loi.cp.discussion.persistence.*
import loi.cp.discussion.update.notifications.{
  InappropriatePostNotification,
  InappropriatePostNotificationInit,
  PostNotification,
  PostNotificationInit
}
import loi.cp.discussion.user.{DiscussionUserProfile, DiscussionUserRightsService, DiscussionUserService}
import loi.cp.notification.{Interest, NotificationService, SubscriptionService}
import loi.cp.path.Path
import loi.cp.reference.*
import loi.cp.security.SecuritySettings
import loi.cp.storage.CourseStorageService
import loi.cp.user.UserService
import org.apache.http.HttpStatus
import scalaz.ValidationNel
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.misc.TryInstances.*
import scaloi.syntax.boolean.*
import scaloi.syntax.map.*
import scaloi.syntax.option.*
import scaloi.syntax.validation.*

import java.time.Instant
import java.util.Date
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

@Service
class DiscussionBoardServiceImpl(
  postDao: PostDao,
  discussionInteractionDao: DiscussionInteractionDao,
  postInteractionDao: PostInteractionDao,
  summaryDao: DiscussionSummaryDao,
  unreadPostDao: UnreadPostDao,
  unrespondedThreadDao: UnrespondedThreadDao,
  bookmarkedPostsDao: BookmarkedPostsDao,
  discussionUserService: DiscussionUserService,
  discussionUserRightsService: DiscussionUserRightsService,
  discussionAttachmentService: DiscussionAttachmentService,
  courseConfigurationService: CourseConfigurationService,
  courseStorageService: CourseStorageService,
  coursewareAnalyticsService: CoursewareAnalyticsServiceImpl,
  notificationService: NotificationService,
  subscriptionService: SubscriptionService,
  userService: UserService,
  time: TimeSource
)(implicit configurationService: ConfigurationService)
    extends DiscussionBoardService:
  import DiscussionBoardService.searchableContent

  override def getDiscussions(section: CourseSection, contents: Seq[CourseContent]): Seq[Discussion] =
    val discussions = DiscussionBoardService.filterDiscussionContent(contents)
    val settings    = discussionSettings(section)
    discussions map { discussionContent =>
      Discussion(
        discussionContent,
        section,
        settings.getOrElse(discussionContent.edgePath, DiscussionSetting.default).closed
      )
    }
  end getDiscussions

  override def getDiscussionSummaries(
    contentIds: Seq[ContentIdentifier],
    userId: UserId
  ): Map[ContentIdentifier, DiscussionSummary] =
    val foundSummaries = summaryDao.summarizeForLearner(contentIds, userId)

    val missing = contentIds.diff(foundSummaries.keys.toSeq).map(ci => ci -> GeneralDiscussionSummary.empty).toMap
    foundSummaries ++ missing

  override def getDiscussionSummariesForReviewer(
    contentIds: Seq[ContentIdentifier],
    userId: UserId
  ): Map[ContentIdentifier, DiscussionSummary] =
    val foundSummaries = summaryDao.summarizeForReviewer(contentIds, userId)
    val missing        = contentIds.diff(foundSummaries.keys.toSeq).map(ci => ci -> ReviewerDiscussionSummary.empty).toMap
    foundSummaries ++ missing

  override def visitDiscussionBoard(contentId: ContentIdentifier, userId: UserId, when: Instant): Instant =
    val interaction: DiscussionInteraction = discussionInteractionDao.visitDiscussionBoard(contentId, userId, when)
    interaction.visited

  override def getThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]],
    searchFor: Option[String],
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = postDao.readonlyPosts(
      discussionId,
      userId,
      Some(0),
      None,
      fetchSettings.includeHidden,
      fetchSettings.includeDescendantCount,
      fetchSettings.includeNewDescendantSinceCount,
      postIds,
      searchFor,
      apiOrders,
      apiPage
    )
    postValuesToPosts(discussionId, userId, posts, fetchSettings.includeUnreadDescendantCount)
  end getThreads

  override def getPost(discussionId: ContentIdentifier, postId: PostId, userId: UserId): Option[Post] =
    for
      post                 <- postDao.readonlyPost(postId)
      if post.contentIdentifier == discussionId
      discussionInteraction = discussionInteractionDao.getOrCreate(discussionId, userId)
    yield PostImpl(
      post,
      discussionUserService
        .getUser(post.user, discussionId.contextId),
      canEdit(discussionInteraction, userId, post.user),
      None,
      None
    )

  override def getPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]],
    toDepth: Option[Long],
    searchFor: Option[String],
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts: FilteredSeq[PostValue] = postDao.readonlyPosts(
      discussionId,
      userId,
      toDepth,
      None,
      fetchSettings.includeHidden,
      fetchSettings.includeDescendantCount,
      fetchSettings.includeNewDescendantSinceCount,
      postIds,
      searchFor,
      apiOrders,
      apiPage
    )
    postValuesToPosts(discussionId, userId, posts, fetchSettings.includeUnreadDescendantCount)
  end getPosts

  override def getPostsForAuthor(
    discussionId: ContentIdentifier,
    userId: UserId,
    authorId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = postDao.readonlyPostsForUser(discussionId, authorId, apiOrders, apiPage)
    postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def getNewPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    since: Instant,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = postDao.readonlyNewPosts(discussionId, userId, since, apiOrders, apiPage)
    postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def getUnreadPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = unreadPostDao.unreadPosts(discussionId, targetUserId, apiOrders, apiPage)
    postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def getBookmarkedPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = bookmarkedPostsDao.bookmarkedPosts(discussionId, targetUserId, apiOrders, apiPage)
    postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def getUnrespondedThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts = unrespondedThreadDao.unrespondedThreads(discussionId, userId, apiOrders, apiPage)
    postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def getDescendantPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    ancestorPostId: PostId,
    fetchSettings: StatisticFetchSetting,
    postIds: Option[Seq[PostId]],
    toDepth: Option[Long],
    searchFor: Option[String],
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[Post] =
    val posts: FilteredSeq[PostValue] = postDao.readonlyPosts(
      discussionId,
      userId,
      toDepth,
      Some(ancestorPostId),
      fetchSettings.includeHidden,
      fetchSettings.includeDescendantCount,
      fetchSettings.includeNewDescendantSinceCount,
      postIds,
      searchFor,
      apiOrders,
      apiPage
    )
    postValuesToPosts(discussionId, userId, posts, fetchSettings.includeUnreadDescendantCount)
  end getDescendantPosts

  def getDescendantPostMappedToThreads(
    discussionId: ContentIdentifier,
    userId: UserId,
    threadIds: Seq[PostId],
    includeHidden: Boolean,
    apiOrders: Seq[ApiOrder] = Seq.empty
  ): Map[PostId, FilteredSeq[Post]] =
    for (threadId, posts) <- postDao.readonlyPostsInThread(
                               discussionId,
                               threadIds,
                               includeHidden,
                               apiOrders
                             )
    yield threadId -> postValuesToPosts(discussionId, userId, posts, includeUnreadDescendantCount = false)

  override def createPost(
    discussion: Discussion,
    createWith: DiscussionBoardService.CreatePostRequest
  ): Try[Post] =
    val discussionId                = discussion.identifier
    val discussionInteraction       = discussionInteractionDao.getOrCreate(discussionId, createWith.user)
    val user: DiscussionUserProfile = discussionUserService.getUser(createWith.user, discussionId.contextId)
    for
      parentPost <- createWith.parentPostId.traverse(tryLoadPost)
      uploads    <- validateUploads(createWith.uploads.orZ).toTry(InvalidUploads.apply)
    yield
      val initialEntity: PostEntity =
        postDao.create(
          discussionId,
          createWith.user,
          createWith.moderator,
          parentPost,
          createWith.title,
          createWith.content,
          searchableContent(createWith.title, createWith.content, user.fullName),
          Nil,
          createWith.createTime
        )

      val newAttachmentIds: Seq[AttachmentId] =
        uploads.map(discussionAttachmentService.addAttachment(discussionId, initialEntity.id, _))
      val postEntity                          = PostEntity.updateAttachments(initialEntity, newAttachmentIds, None)
      postDao.write(postEntity)

      val ancestorIds: Seq[Long] = new Path(postEntity.postPath).getElements.asScala.toSeq.map(_.toLong)
      postDao.updateActivityTime(ancestorIds, createWith.createTime)
      val createdPost            =
        PostImpl(
          postEntity,
          user,
          canEdit(discussionInteraction, createWith.user, createWith.user),
          None,
          Some(0),
          Some(0)
        )

      coursewareAnalyticsService.emitDiscussionPostPutEvent(createdPost.postValue, discussion, None, None)

      notificationService.nοtify[PostNotification](
        discussion.section.id,
        PostNotificationInit(createWith.user, discussionId, createdPost)
      )

      // Users get subscribed to their own threads.
      if createdPost.depth == 0 then
        val path = PostNotification.subscriptionPath(discussionId.edgePath, createdPost.postPath)
        subscriptionService.subscribe(createWith.user, discussion.section, path, Interest.Alert)

      // if the created post is created by an instructor then the parent post gets an instructor reply time
      parentPost
        .when(createWith.moderator)
        .map(PostValueEntity(_, None, None))
        .foreach(pp =>
          val instructorReplyUserId = Some(createdPost.authorId.id)
          val instructorReplyTime   = Some(createdPost.created)

          coursewareAnalyticsService.emitDiscussionPostPutEvent(
            pp,
            discussion,
            instructorReplyUserId,
            instructorReplyTime
          )
        )

      createdPost
    end for
  end createPost

  override def updatePostContent(discussionId: ContentIdentifier, updateWith: UpdatePostRequest): Try[Post] =
    for
      post                          <- tryLoadPost(updateWith.postId)
      discussionInteraction          = discussionInteractionDao.getOrCreate(discussionId, updateWith.userId)
      profile: DiscussionUserProfile = discussionUserService.getUser(
                                         UserId(Long2long(post.userId)),
                                         discussionId.contextId
                                       )
      _                             <- editsAreAllowed(
                                         discussionId,
                                         PostValueEntity(post, None, None),
                                         discussionInteraction,
                                         updateWith.userId,
                                         updateWith.moderator
                                       )
      uploads                       <- validateUploads(updateWith.newUploads.orZ).toTry(InvalidUploads.apply)
      _                             <- validateAttachmentsToKeep(post, updateWith.attachmentsToKeep)
    yield
      val newAttachmentIds =
        uploads.map(upload => discussionAttachmentService.addAttachment(discussionId, post.id, upload))
      PostEntity.updateAttachments(post, newAttachmentIds, updateWith.attachmentsToKeep)

      if updateWith.title.isDefined then post.title = updateWith.title.get
      post.content = updateWith.content
      post.searchableContent = searchableContent(updateWith.title, updateWith.content, profile.fullName)
      post.updated = Date.from(time.instant)

      postDao.write(post)

      val user: DiscussionUserProfile = discussionUserService.getUser(updateWith.userId, discussionId.contextId)

      val outPost = PostImpl(post, user, canEdit = true, None)

      outPost

  private def validateUploads(uploads: List[UploadInfo]): ValidationNel[String, List[UploadInfo]] =
    val security = SecuritySettings.config.getDomain
    uploads.traverse(SecuritySettings.validateUpload(security))

  // LOSD-485 this check is just to handle a PUT request that an evildoer made themselves outside of our UI
  private def validateAttachmentsToKeep(post: PostEntity, attachmentIds: Option[List[AttachmentId]]): Try[Unit] =
    val existingIds = PostEntity.mapAttachmentIds(post.attachmentIds).toSet
    val notYourIds  = attachmentIds.orZ.filterNot(existingIds.contains)
    notYourIds.isEmpty either (()) orFailure {
      DiscussionBoardServiceImpl.log.info(
        s"bad request, attachments ${notYourIds.mkString(",")} do not belong to post ${post.id}"
      )
      new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "")
    }

  private def tryLoadPost(postId: PostId): Try[PostEntity] =
    postDao.load(postId).toTry(new IllegalStateException(s"No post with id $postId"))

  override def reportPostInappropriate(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    reason: String
  ): Try[Unit] =
    for
      post <- postDao.load(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      _    <- interactionAllowed(discussionId, PostValueEntity(post, None, None))
    yield
      if post.contentIdentifier != discussionId then
        throw new IllegalStateException(s"$postId found ${post.contentIdentifier} expected $discussionId")
      import scaloi.misc.Monoids.rightBiasMapMonoid

      val discussionReviewers =
        courseConfigurationService.getGroupConfig(CoursePreferences, discussionId.contextId).discussionReviewers
      val adminReviewers      =
        discussionReviewers.nonEmpty ?? userService.getUsersByUsername(discussionReviewers.split(","))
      val reviewers           = discussionUserRightsService.courseReviewers(discussionId.contextId)

      notificationService.nοtify[InappropriatePostNotification](
        discussionId.contextId,
        InappropriatePostNotificationInit(
          reporter = userId,
          reviewersOfInappropriate = reviewers ++ adminReviewers.values,
          discussionId = discussionId,
          post = PostPropertiesImpl(post),
          time = time.instant
        )
      )

  override def updatePostInappropriate(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    inappropriate: Boolean
  ): Try[Boolean] =
    for
      post <- postDao.load(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      _    <- interactionAllowed(discussionId, PostValueEntity(post, None, None))
    yield
      val oldValue = post.inappropriate
      post.inappropriate = inappropriate
      post.updated = Date.from(time.instant)
      postDao.write(post)

      inappropriate

  override def updatePostPinned(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    pinned: Option[Instant]
  ): Try[Option[Instant]] =
    for
      post: PostEntity <- postDao.load(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      _                <- interactionAllowed(discussionId, PostValueEntity(post, None, None))
    yield
      val oldValue: Option[Instant] = Option(post.pinnedOn).map(_.toInstant)
      post.pinnedOn = pinned.fold[Date](null)(p => Date.from(p))
      post.updated = Date.from(time.instant)
      postDao.write(post)

      pinned

  override def updatePostRemoved(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    removed: Boolean,
    onlyOriginalUser: Boolean = true
  ): Try[Boolean] =
    for
      post                 <- postDao.load(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      discussionInteraction = discussionInteractionDao.getOrCreate(discussionId, userId)
      _                    <- editsAreAllowed(
                                discussionId,
                                PostValueEntity(post, None, None),
                                discussionInteraction,
                                userId,
                                !onlyOriginalUser || (onlyOriginalUser && UserId(Long2long(post.userId)) == userId)
                              )
    yield
      val oldValue = post.removed
      post.removed = removed
      post.updated = Date.from(time.instant)
      postDao.write(post)

      removed

  override def updatePostBookmarked(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    bookmarked: Boolean
  ): Try[Boolean] =
    for
      post       <- postDao.readonlyPost(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      interaction = postInteractionDao.getOrCreate(userId, postId)
      _          <- interactionAllowed(discussionId, post)
    yield
      val oldValue = Option(interaction.bookmarked).map(_.booleanValue)
      interaction.bookmarked = bookmarked
      postInteractionDao.write(interaction)

      val path = PostNotification.subscriptionPath(discussionId.edgePath, post.postPath)
      if bookmarked then subscriptionService.subscribe(userId, discussionId.contextId, path, Interest.Alert)
      else subscriptionService.unsubscribe(userId, discussionId.contextId, path)

      bookmarked

  override def updatePostViewed(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    viewed: Boolean
  ): Try[Boolean] =
    for
      post       <- postDao.readonlyPost(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      interaction = postInteractionDao.getOrCreate(userId, postId)
      _          <- interactionAllowed(discussionId, post)
    yield
      interaction.viewed = viewed
      postInteractionDao.write(interaction)
      // Intentionally not throwing this event
      viewed

  override def updatePostFavorited(
    discussionId: ContentIdentifier,
    postId: PostId,
    userId: UserId,
    favorited: Boolean
  ): Try[Boolean] =
    for
      post       <- postDao.readonlyPost(postId).toTry(new IllegalStateException(s"No post with id $postId"))
      interaction = postInteractionDao.getOrCreate(userId, postId)
      _          <- interactionAllowed(discussionId, post)
    yield
      val oldValue = Option(interaction.favorited).map(_.booleanValue)
      interaction.favorited = favorited
      postInteractionDao.write(interaction)

      favorited

  override def getJumpbarSummary(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    settings: JumpbarSettings
  ): Jumpbar =
    val defaultPage                                          = new BaseApiPage(0, 5)
    // Using different limit for new post because if we don't fetch a lot the frontend gets janky and there isn't a good way to fix it.
    val newPostPage                                          = new BaseApiPage(0, 50)
    // These default orders should align with DiscussionJumpbarLoaders.js
    val defaultPostOrder                                     =
      Seq(new BaseApiOrder("descendantActivity", OrderDirection.DESC), new BaseApiOrder("depth", OrderDirection.ASC))
    val defaultThreadOrder                                   =
      Seq(new BaseApiOrder("descendantActivity", OrderDirection.DESC))
    val maybePostsForJumpbar: Map[String, FilteredSeq[Post]] = postValuesToPosts(
      discussionId,
      targetUserId,
      Seq(
        settings.userPosts.map(userId =>
          "user" -> postDao.readonlyPostsForUser(discussionId, userId, defaultPostOrder, defaultPage)
        ),
        settings.bookmarkedPosts.option(
          "bookmarked"  -> bookmarkedPostsDao
            .bookmarkedPosts(discussionId, targetUserId, defaultPostOrder, defaultPage)
        ),
        settings.newPosts.map(
          "new" -> postDao.readonlyNewPosts(discussionId, targetUserId, _, defaultPostOrder, newPostPage)
        ),
        settings.unreadPosts.option(
          "unread"      -> unreadPostDao.unreadPosts(discussionId, targetUserId, defaultPostOrder, defaultPage)
        ),
        settings.unrespondedThreads.option(
          "unresponded" ->
            unrespondedThreadDao.unrespondedThreads(discussionId, targetUserId, defaultThreadOrder, defaultPage)
        )
      ).flatten.toMap,
      includeUnreadDescendantCount = false
    )
    Jumpbar(
      maybePostsForJumpbar.get("user"),
      maybePostsForJumpbar.get("bookmarked"),
      maybePostsForJumpbar.get("new"),
      maybePostsForJumpbar.get("unread"),
      maybePostsForJumpbar.get("unresponded")
    )
  end getJumpbarSummary

  def getPostCountForUsers(discussionId: ContentIdentifier): Seq[UserPostCountDTO] =
    val counts        = postDao.userPostCounts(discussionId)
    val userIds       = counts.map(count => UserId(count.itemId))
    val userToProfile = discussionUserService.getUsers(userIds, discussionId.contextId)
    counts.map(count => UserPostCountDTO(userToProfile(UserId(count.itemId)), long2Long(count.count)))

  private def postValuesToPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    posts: FilteredSeq[PostValue],
    includeUnreadDescendantCount: Boolean
  ): FilteredSeq[Post] =
    postValuesToPosts(discussionId, userId, Map("" -> posts), includeUnreadDescendantCount)("")

  private def postValuesToPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    allPosts: Map[String, FilteredSeq[PostValue]],
    includeUnreadDescendantCount: Boolean
  ): Map[String, FilteredSeq[Post]] =
    val discussionInteraction =
      discussionInteractionDao.read(discussionId, userId).getOrElse(DiscussionInteraction.empty)

    val userIdToProfile = discussionUserService
      .getUsers(allPosts.values.flatMap(_.partialResults.map(_.user)).toList.distinct, discussionId.contextId)

    val postIds: Seq[PostId] = allPosts.values.flatMap(_.partialResults.map(_.id)).toSeq
    val postIdToInteractions = postInteractionDao.userInteractions(userId, postIds)

    val threadIdToUnread: Map[PostId, Long] =
      if includeUnreadDescendantCount then unreadPostDao.unreadReplyCounts(discussionId, userId, postIds)
      else Map.empty

    import scalaz.syntax.functor.*
    allPosts.mapValuesEagerly(
      _.map(post =>
        PostImpl(
          post,
          userIdToProfile(post.user),
          canEdit(discussionInteraction, userId, post.user),
          postIdToInteractions.get(post.id),
          threadIdToUnread.get(post.id)
        )
      )
    )
  end postValuesToPosts

  private def canEdit(discussionInteraction: DiscussionInteraction, userId: UserId, authorId: UserId): Boolean =
    discussionInteraction.canEditOwnPosts && userId == authorId

  /** Will throw or always return true.
    */
  private def editsAreAllowed(
    discussionId: ContentIdentifier,
    post: PostValue,
    discussionInteraction: DiscussionInteraction,
    targetUser: UserId,
    superEditAbility: => Boolean
  ): Try[Unit] =
    if discussionId != post.contentIdentifier then
      Failure(
        new BusinessRuleViolationException(
          s"$targetUser attempting to update post with mismatched discussion. Original ${post.contentIdentifier}, post against $discussionId"
        )
      )
    else if (discussionInteraction.canEditOwnPosts && post.user == targetUser) || superEditAbility then Success { () }
    else
      Failure(
        new AccessForbiddenException(
          s"$targetUser may not update the content for this post.  Only the original learner may update content."
        )
      )

  private def interactionAllowed(discussionId: ContentIdentifier, post: PostValue): Try[Unit] =
    if discussionId != post.contentIdentifier then
      Failure(
        new BusinessRuleViolationException(
          s"Attempting to update post interaction with mismatched discussion. Original ${post.contentIdentifier}, post against $discussionId"
        )
      )
    else Success { () }

  private def discussionSettings(section: CourseSection): Map[EdgePath, DiscussionSetting] =
    (for settings <- Try(courseStorageService.get[StoragedDiscussionSettings](section)).toOption
    yield settings.settings.discussionBoardSettings).getOrElse(Map.empty)
end DiscussionBoardServiceImpl

object DiscussionBoardServiceImpl:
  private final val log = org.log4s.getLogger
