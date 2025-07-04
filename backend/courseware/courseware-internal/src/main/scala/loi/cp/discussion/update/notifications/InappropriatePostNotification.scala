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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate, Schema}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.course.CourseSectionService
import loi.cp.discussion.dto.PostProperties
import loi.cp.discussion.user.DiscussionUserService
import loi.cp.discussion.{Discussion, DiscussionBoardService, PostId}
import loi.cp.email.Email
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation, SubscriptionPath}
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scaloi.syntax.instant.*

import java.time.Instant

@Schema("inappropriatePostNotification")
trait InappropriatePostNotification extends Notification:
  type Init = InappropriatePostNotificationInit

  @JsonProperty
  def postId: Long
  @JsonProperty
  def title: String
  @JsonProperty
  def edgePath: EdgePath

  @JsonProperty
  def threadId: Long

  @JsonProperty
  def senderFullName: String

  @JsonIgnore
  def discussion: Option[Discussion]
end InappropriatePostNotification

@Component
class InappropriatePostNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(
  courseSectionService: CourseSectionService,
  discussionBoardService: DiscussionBoardService,
  discussionUserService: DiscussionUserService
) extends InappropriatePostNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(init: InappropriatePostNotificationInit): Unit =
    self.setTime(init.time.asDate)
    self.setSender(Some(init.reporter.id))
    self.setContext(Some(init.discussionId.contextId.value))
    self.setData(
      InappropriatePostNotificationDto(
        init.discussionId,
        init.post.id,
        init.post.threadId,
        init.reviewersOfInappropriate.map(x => long2Long(x.value)),
        long2Long(init.reporter.value),
        init.post.postPath.map(long2Long)
      )
    )
  end init

  override lazy val postId: Long       = data.postId
  override lazy val title: String      = discussion.map(d => d.title).getOrElse("")
  override lazy val edgePath: EdgePath = data.discussionId.edgePath

  override lazy val threadId: Long = data.threadId

  override lazy val senderFullName: String =
    discussionUserService.getUser(UserId(getSender.get), data.discussionId.contextId).fullName

  override lazy val discussion: Option[Discussion] =
    for
      section <- courseSectionService.getCourseSection(data.discussionId.contextId.value)
      content <- section.contents.get(data.discussionId.edgePath)
      board   <- discussionBoardService.getDiscussion(section, content)
    yield board

  override lazy val subscriptionPath: Option[SubscriptionPath] = Some(
    PostNotification.subscriptionPath(edgePath, data.postPath.map(Long2long))
  )

  override lazy val aggregationKey: Option[String] = Some(s"$schemaName:$threadId")

  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    super.bindings(using domain, user, cs) ++
      discussion.map("discussion" -> _)

  override def audience: Iterable[Long] = data.reviewersOfInappropriate.map(Long2long)

  /** Get a notification email initializer. */
  override def emailInfo: Option[Notification.EmailInfo[InappropriatePostNotificationEmail]] =
    Some(Notification.EmailInfo(classOf[InappropriatePostNotificationEmail], Email.Init(Some(getId), None)))

  private lazy val data = self.getData(classOf[InappropriatePostNotificationDto])
end InappropriatePostNotificationImpl

object InappropriatePostNotification:

  def subscriptionPath(contentId: ContentIdentifier, postPath: Seq[PostId]): SubscriptionPath =
    SubscriptionPath((Seq(contentId.edgePath.toString) ++ postPath.map(_.toString)).toList)

final case class InappropriatePostNotificationInit(
  reporter: UserId,
  reviewersOfInappropriate: Seq[UserDTO],
  discussionId: ContentIdentifier,
  post: PostProperties,
  time: Instant
)

final case class InappropriatePostNotificationDto(
  discussionId: ContentIdentifier,
  postId: JLong,
  threadId: JLong,
  reviewersOfInappropriate: Seq[JLong],
  reporter: JLong,
  postPath: Seq[JLong]
)
