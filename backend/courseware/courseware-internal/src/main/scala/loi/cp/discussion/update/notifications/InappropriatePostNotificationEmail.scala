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
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.context.ContextId
import loi.cp.discussion.dto.Post
import loi.cp.discussion.user.DiscussionUserRightsService
import loi.cp.discussion.{Discussion, DiscussionBoardService}
import loi.cp.email.{Email, EmailFacade}
import loi.cp.notification.{AbstractNotificationEmail, NotificationService}
import loi.cp.reference.ContentIdentifier
import loi.cp.reply.ReplyService
import scaloi.misc.TimeSource

import scala.xml.Elem

/** @author
  *   mkalish
  */
@Schema("inappropriatePostNotificationEmail")
trait InappropriatePostNotificationEmail extends Email {}

@Component
class InappropriatePostNotificationEmailImpl(
  val self: EmailFacade,
  val domain: DomainDTO,
  val componentInstance: ComponentInstance,
  val replyService: ReplyService,
  val fs: FacadeService,
  val ns: NotificationService,
  val userDTO: UserDTO,
  val timeSource: TimeSource,
  val discussionBoardService: DiscussionBoardService,
  val discussionUserRightsService: DiscussionUserRightsService,
)(implicit cs: ComponentService, mapper: ObjectMapper)
    extends AbstractNotificationEmail[InappropriatePostNotification]
    with InappropriatePostNotificationEmail:

  import AbstractNotificationEmail.UnparsedStringOps
  import InappropriatePostNotificationEmailImpl.*

  private lazy val author = notification.senderFullName

  override def getBindings: Map[String, Any] =
    super.getBindings ++
      discussion.map("discussion" -> _) ++
      post.map("post" -> _) ++
      Map("author" -> author)

  private def postBody: String =
    post
      .map(_.content)
      .fold(throw new IllegalStateException(s"Recipient is not permitted to view post ${notification.postId}")) {
        content =>
          content
      }

  private def meta: I18nMessage = NewInappropriatePost

  override def parts(bindings: Map[String, Any])(implicit cd: ComponentDescriptor): Seq[Elem] =
    Seq(
      <p>{intro.i18n(bindings).u}</p>,
      <p>{meta.i18n(bindings).u}</p>,
      <blockquote>{postBody.u}</blockquote>,
    )

  private lazy val post: Option[Post] = notification.getContext.flatMap(id =>
    discussionBoardService.getPost(
      ContentIdentifier(
        ContextId(id),
        notification.edgePath
      ),
      notification.postId,
      userDTO
    )
  )

  private lazy val discussion: Option[Discussion] = notification.discussion
end InappropriatePostNotificationEmailImpl

object InappropriatePostNotificationEmailImpl:
  val NewInappropriatePost = I18nMessage(
    "NOTIFICATIONS_EMAIL_BODY_ANONYMOUS_inappropriatePostNotification_html",
    """A student has marked a post as inappropriate in "{discussion.title}":"""
  )
