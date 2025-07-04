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
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.{CourseAccessService, CourseWorkspaceService}
import loi.cp.discussion
import loi.cp.discussion.DiscussionBoardService.JumpbarSettings
import loi.cp.discussion.*
import loi.cp.discussion.api.DiscussionBoardWebController.DiscussionAccessChange
import loi.cp.discussion.api.dto.*
import loi.cp.discussion.attachment.DiscussionAttachmentService
import loi.cp.discussion.dto.{DiscussionSummary, FilteredSeq, Jumpbar, Post}
import loi.cp.discussion.user.DiscussionUserRightsService
import loi.cp.reference.{ContentIdentifier, EdgePath}
import loi.cp.storage.CourseStorageService
import loi.cp.user.ImpersonationService
import loi.cp.web.HandleService
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.DisjunctionOps.*
import scaloi.syntax.OptionOps.*

import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Component
class DiscussionBoardWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  discussionBoardService: DiscussionBoardService,
  discussionUserRightsService: DiscussionUserRightsService,
  impersonationService: ImpersonationService,
  discussionInstructionService: DiscussionInstructionService,
  handleService: HandleService,
  courseAccessService: CourseAccessService,
  currentUser: => UserDTO,
  discussionAttachmentService: DiscussionAttachmentService,
  timeSource: TimeSource,
  courseWorkspaceService: CourseWorkspaceService
)(implicit courseStorageService: CourseStorageService)
    extends DiscussionBoardWebController
    with ComponentImplementation:
  import DiscussionHelpers.*

  override def getDiscussions(
    contextId: ContextId,
    summarize: Option[JBoolean],
    targetUser: Option[JLong],
    apiQuery: ApiQuery
  ): Try[ApiQueryResults[DiscussionDTO]] =

    val userId: UserId = targetUser.map(targetId => UserId(Long2long(targetId))).getOrElse(UserId(currentUser.id))
    for
      rights            <- courseAccessService.actualRights(contextId, userId) <@~* new AccessForbiddenException("No access")
      section           <- courseWebUtils.loadCourseSection(contextId.value, rights.some).toTry(new ResourceNotFoundException(_))
      contents           = section.contents.nonRootElements
      allBoardsForCourse = discussionBoardService.getDiscussions(section, contents)
      discussions        = allBoardsForCourse.slice(
                             apiQuery.getPage.getOffset,
                             apiQuery.getPage.getOffset + apiQuery.getPage.getLimitOr(allBoardsForCourse.size)
                           )
    yield
      val results: Seq[DiscussionDTO] = (for
        _          <- impersonationService.checkImpersonation(contextId, userId).toTry
        isModerator = discussionUserRightsService.userHasModeratorRight(userId, contextId)
        summaries   = if summarize.exists(_.booleanValue()) then summaryData(userId, isModerator, discussions)
                      else Map.empty[ContentIdentifier, DiscussionSummary]
      yield for
        discussion <- discussions
        summary     = summaries.get(discussion.identifier)
      yield DiscussionDTO(
        discussion,
        summary,
        None
      )).get

      new ApiQueryResults(results.asJava, results.size.toLong, allBoardsForCourse.size.toLong)
    end for
  end getDiscussions

  override def getDiscussion(
    discussionId: ContentIdentifier,
    targetUser: Option[JLong]
  ): Try[DiscussionDetailsDTO] =
    val userId: UserId = targetUser.map(targetId => UserId(Long2long(targetId))).getOrElse(UserId(currentUser.id))
    for
      rights             <-
        courseAccessService.actualRights(discussionId.contextId, userId) <@~* new AccessForbiddenException("No access")
      _                  <- impersonationService.checkImpersonation(discussionId.contextId, userId).toTry
      (section, content) <- courseWebUtils
                              .loadCourseSectionContents(
                                discussionId.contextId.value,
                                discussionId.edgePath,
                                rights.some
                              )
                              .toTry(new ResourceNotFoundException(_))
      discussion         <- discussionBoardService
                              .getDiscussion(section, content)
                              .toTry(new ResourceNotFoundException(s"No discussion at $discussionId"))
    yield
      val instructions = discussionInstructionService.getInstructions(section.lwc, discussion.courseContent)
      val isModerator  = discussionUserRightsService.userHasModeratorRight(userId, discussionId.contextId)
      val summaries    = summaryData(userId, isModerator, Seq(discussion))
      DiscussionDetailsDTO(discussion, summaries.get(discussionId), instructions)
    end for
  end getDiscussion

  override def getJumpbarSummary(
    discussionId: ContentIdentifier,
    includeUserPosts: Option[String],
    includeBookmarkedPosts: Option[JBoolean],
    includeNewPostFrom: Option[Instant],
    includeUnreadPosts: Option[JBoolean],
    includeUnrespondedToThreads: Option[JBoolean],
    targetUserId: Option[JLong]
  ): Try[JumpbarDTO] =
    val targetUser: UserId = targetUserId.fold[UserId](currentUser)(id => UserId(Long2long(id)))
    for
      _           <- impersonationService
                       .checkImpersonation(discussionId.contextId, targetUser)
                       .toTry
      isModerator  = discussionUserRightsService.userHasModeratorRight(targetUser, discussionId.contextId)
      section     <- courseWebUtils.loadCourseSection(discussionId.contextId.value).toTry(new ResourceNotFoundException(_))
      closed       = isClosed(discussionSettings(section.lwc), discussionId.edgePath)
      targetUserId = includeUserPosts.flatMap(iup => handleService.unmask(iup).map(UserId(_)))
      settings     = JumpbarSettings(
                       userPosts = includeUserPosts.map(_ => targetUserId.getOrElse(currentUser)),
                       bookmarkedPosts = includeBookmarkedPosts.exists(_.booleanValue()),
                       newPosts = includeNewPostFrom,
                       unreadPosts = isModerator && includeUnreadPosts.exists(_.booleanValue()),
                       unrespondedThreads = isModerator && includeUnrespondedToThreads.exists(_.booleanValue())
                     )
      jumpbar      = discussionBoardService.getJumpbarSummary(discussionId, targetUser, settings)
    yield toJumpbarDTO(discussionId, currentUser.id, closed, isModerator, jumpbar)
    end for
  end getJumpbarSummary

  private def toJumpbarDTO(
    discussionId: ContentIdentifier,
    currentUserId: Long,
    closed: Boolean,
    moderator: Boolean,
    jumpbar: Jumpbar
  ): JumpbarDTO =
    val allAttachmentIds =
      Seq(
        jumpbar.userPosts,
        jumpbar.newPosts,
        jumpbar.unreadPosts,
        jumpbar.unrespondedThreads,
        jumpbar.bookmarkedPosts
      ).flatten
        .flatMap(_.partialResults.flatMap(_.attachmentIds))
        .distinct

    val attachmentsById = discussionAttachmentService
      .loadAttachmentInfos(discussionId, allAttachmentIds)
      .map(a => (a.id, a))
      .toMap

    def convert(posts: FilteredSeq[Post]): FilteredSeq[PostDTO] =
      FilteredSeq(
        posts.total,
        posts.offset,
        posts.partialResults
          .map(p =>
            toPostDto(
              discussionId,
              p,
              currentUserId,
              closed,
              moderator,
              p.attachmentIds.map(id => attachmentsById(id))
            )
          )
      )

    JumpbarDTO(
      jumpbar.userPosts.map(convert),
      jumpbar.bookmarkedPosts.map(convert),
      jumpbar.newPosts.map(convert),
      jumpbar.unreadPosts.map(convert),
      jumpbar.unrespondedThreads.map(convert)
    )
  end toJumpbarDTO

  def getUserPostCounts(discussionId: ContentIdentifier): Seq[UserPostCountDTO] =
    discussionBoardService.getPostCountForUsers(discussionId)

  override def visit(discussionId: ContentIdentifier): Instant =
    discussionBoardService.visitDiscussionBoard(discussionId, currentUser, timeSource.instant)

  private def summaryData(
    userId: UserId,
    isModerator: Boolean,
    discussions: Seq[Discussion]
  ): Map[ContentIdentifier, DiscussionSummary] =
    if isModerator then discussionBoardService.getDiscussionSummariesForReviewer(discussions.map(_.identifier), userId)
    else discussionBoardService.getDiscussionSummaries(discussions.map(_.identifier), userId)

  override def closeDiscussions(contextId: ContextId, request: DiscussionAccessChange): Unit =
    val newSettings: Map[EdgePath, DiscussionSetting] = request.discussions.map { case (edgePath, shouldClose) =>
      edgePath -> DiscussionSetting(shouldClose)
    }

    for section <- courseWebUtils.loadCourseSection(contextId.value).toTry(new ResourceNotFoundException(_))
    yield courseStorageService.modify[StoragedDiscussionSettings](section.lwc)(current =>
      discussion.StoragedDiscussionSettings(
        DiscussionSettings(current.settings.discussionBoardSettings ++ newSettings)
      )
    )
  end closeDiscussions
end DiscussionBoardWebControllerImpl
