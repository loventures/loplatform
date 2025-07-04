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

package loi.cp.discussion.update.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.scala.util.Misc.ErrorMessage
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.HtmlUtils
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.discussion.dto.Post
import loi.cp.discussion.user.DiscussionUserRightsService
import loi.cp.discussion.{Discussion, DiscussionBoardService}
import loi.cp.email.{Email, EmailFacade}
import loi.cp.notification.{AbstractNotificationEmail, NotificationService}
import loi.cp.reference.ContentIdentifier
import loi.cp.reply.ReplyService
import scalaz.\/
import scaloi.misc.TimeSource

import javax.mail.internet.MimeMessage
import scala.xml.Elem

@Schema("postNotificationEmail")
trait PostNotificationEmail extends Email {}

@Component
class PostNotificationEmailImpl(
  val self: EmailFacade,
  val domain: DomainDTO,
  val componentInstance: ComponentInstance,
  courseWebUtils: CourseWebUtils,
  val replyService: ReplyService,
  val fs: FacadeService,
  val ns: NotificationService,
  val userDTO: UserDTO,
  val timeSource: TimeSource,
  val discussionBoardService: DiscussionBoardService,
  val discussionUserRightsService: DiscussionUserRightsService,
)(implicit cs: ComponentService, mapper: ObjectMapper)
    extends AbstractNotificationEmail[PostNotification]
    with PostNotificationEmail:
  import AbstractNotificationEmail.UnparsedStringOps
  import PostNotificationEmailImpl.*

  // Technically discussion boards are isomorphic with threaded messaging and so I could do
  // the complicated in-reply-to header manipulation that exists in MessageEmail but...
  // insufficient reason

  /** Process a reply and turn it into a discussion board post. */
  override def processReply(email: MimeMessage): ErrorMessage \/ Option[Long] =
    \/.attempt {
      // TODO: the attachments too?
      notification.getContext.map(id =>

        val section    = courseWebUtils.sectionOrThrow404(id)
        val discussion = courseWebUtils.discussionOrThrow404(section, notification.edgePath)

        discussionBoardService
          .createPost(
            discussion,
            DiscussionBoardService.CreatePostRequest(
              userDTO,
              discussionUserRightsService.userHasModeratorRight(userDTO, discussion.section),
              Some(notification.postId),
              timeSource.instant,
              None,
              replyContent(email),
              None
            )
          )
          .get
          .id
      )
    } { e =>
      logger.warn(e)("Error processing discussion reply")
      InvalidDiscussionReply
    }

  override protected def getBindings: Map[String, Any] =
    super.getBindings ++
      discussion.map("discussion" -> _) ++
      post.map("post" -> _) ++
      parentTitleTruncated.map("threadTitle" -> _) ++
      Map("author" -> author)

  override protected def parts(bindings: Map[String, Any])(implicit cd: ComponentDescriptor): Seq[Elem] =
    Seq(
      <p>{intro.i18n(bindings).u}</p>,
      <p>{meta.i18n(bindings).u}</p>,
      <blockquote>{postBody}</blockquote>,
      <p>{outro.i18n(bindings).u}</p>
    )

  private def postBody(implicit cd: ComponentDescriptor): Elem =
    if parentPost.isDefined then newReplyBody
    else newThreadBody

  private def newThreadBody: Elem =
    <div style="border: solid 1px; padding: 8px;">
      <strong>{author.fullName}</strong><br/>
      {postContent.u}
    </div>

  private def newReplyBody: Elem =
    <div style="margin-left: 24px; border: solid 1px; padding: 8px;">
      <strong>{author.fullName}</strong><br/>
      {postContent.u}
    </div>

  /** Get the post body or throw if you can't see it. Doing so at this point is pretty bogus; a notification should
    * never have been scattered to this user. In theory I could subscribe to a discussion board but not have
    * view-other-posts rights and I will get bogus notifications. Should fix that.
    */
  private def postContent: String =
    post
      .map(_.content)
      .fold(throw new IllegalStateException(s"Recipient is not permitted to view post ${notification.postId}")) {
        content =>
          content
      }

  private def meta: I18nMessage =
    if parentPost.isDefined then NewReplyByUser else NewThreadByUser

  override protected def outro: I18nMessage =
    if canReply then OutroCanReply else super.outro

  private def canReply: Boolean = true // TODO: Rights around discussion boards

  private lazy val author = notification.author

  private lazy val post: Option[Post] = loadPost(notification.postId)

  private lazy val parentPost: Option[Post] =
    post.flatMap(p => p.parentId.flatMap(id => loadPost(id)))

  private lazy val parentTitleTruncated: Option[String] =
    parentPost
      .map(p => p.title.filterNot(_.isEmpty).getOrElse(p.content)) // Use content if title is None or Empty
      .map(title => HtmlUtils.truncate(title, MAX_TITLE_LENGTH, true))
      .map(t => t.getText + (if t.isTruncated then "..." else ""))

  private def loadPost(postId: Long): Option[Post] =
    discussionBoardService.getPost(
      ContentIdentifier(
        ContextId(notification.getContext.get), // posts will always be inside of a course context
        notification.edgePath
      ),
      postId,
      userDTO
    )

  private lazy val discussion: Option[Discussion] = notification.discussion
end PostNotificationEmailImpl

object PostNotificationEmailImpl:
  private final val logger = org.log4s.getLogger

  val MAX_TITLE_LENGTH = 60

  val NewPostIntro = I18nMessage(
    "NOTIFICATIONS_EMAIL_INTRO_IN_DISCUSSION_html",
    """Hi {user.givenName}, you have received a new notification in {course.name} - "{discussion.title}":"""
  )

  val NewThreadByUser =
    I18nMessage("NOTIFICATIONS_EMAIL_META_NEW_THREAD_html", """{author.fullName} made a new thread:""")

  val NewReplyByUser = I18nMessage(
    "NOTIFICATIONS_EMAIL_META_NEW_REPLY_html",
    """{author.fullName} made a reply to a thread "{threadTitle}":"""
  )

  val OutroCanReply = I18nMessage(
    "NOTIFICATIONS_EMAIL_OUTRO_CAN_REPLY_postNotification_html",
    """You can reply to the discussion post by replying to this message."""
  )

  val InvalidDiscussionReply =
    I18nMessage("NOTIFICATIONS_EMAIL_INVALID_DISCUSSION_REPLY", "Your discussion board reply was not successful.")
end PostNotificationEmailImpl
