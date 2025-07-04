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
import loi.cp.course.{CourseEnrollmentService, CourseSectionService}
import loi.cp.discussion.dto.Post
import loi.cp.discussion.user.{DiscussionUserProfile, DiscussionUserService}
import loi.cp.discussion.{Discussion, DiscussionBoardService, PostId}
import loi.cp.email.Email
import loi.cp.notification.{Notification, NotificationFacade, NotificationImplementation, SubscriptionPath}
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scaloi.syntax.instant.*

import scala.collection.Iterable

@Schema("postNotification")
trait PostNotification extends Notification:
  type Init = PostNotificationInit

  @JsonProperty
  def postId: Long
  @JsonProperty
  def title: String
  @JsonProperty
  def edgePath: EdgePath
  @JsonProperty
  def authorId: Long
  @JsonProperty
  def threadId: Long

  @JsonIgnore
  def discussion: Option[Discussion]

  @JsonIgnore
  def author: DiscussionUserProfile
end PostNotification

@Component
class PostNotificationImpl(val componentInstance: ComponentInstance, val self: NotificationFacade)(
  courseSectionService: CourseSectionService,
  discussionBoardService: DiscussionBoardService,
  discussionUserService: DiscussionUserService,
  courseEnrollmentService: CourseEnrollmentService,
  cs: ComponentService
) extends PostNotification
    with NotificationImplementation
    with ComponentImplementation:

  @PostCreate
  def init(dto: PostNotificationInit): Unit =
    self.setTime(dto.post.created.asDate)
    self.setSender(Some(dto.poster.id))
    self.setContext(Some(dto.discussionId.contextId.value))
    self.setData(
      PostNotificationDto(dto.discussionId, dto.post.id, dto.post.threadId, dto.post.postPath.map(long2Long))
    )

  override lazy val postId: Long       = data.postId
  override lazy val title: String      = discussion.map(d => d.title).getOrElse("")
  override lazy val edgePath: EdgePath = data.discussionId.edgePath
  override lazy val authorId: Long     = getSender.get
  override lazy val threadId: Long     = data.threadId

  override lazy val discussion: Option[Discussion] =
    for
      section <- courseSectionService.getCourseSection(data.discussionId.contextId.value)
      content <- section.contents.get(data.discussionId.edgePath)
      board   <- discussionBoardService.getDiscussion(section, content)
    yield board

  override lazy val author: DiscussionUserProfile =
    discussionUserService.getUser(UserId(authorId), data.discussionId.contextId)

  override lazy val subscriptionPath =
    Some(PostNotification.subscriptionPath(edgePath, data.postPath.map(Long2long)))

  override lazy val aggregationKey = Some(s"$schemaName:${data.threadId}")

  override def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any] =
    super.bindings(using domain, user, cs) ++
      discussion.map("discussion" -> _) ++ Map("author" -> author)

  /** Course-level discussions automatically broadcast to all students while they remain hijacked as course-level
    * announcements.
    */
  // TODO: Course level discussion board announcements (maybe)
//  override def audience: Iterable[Long] =
//    getPost.getContainer.getNotificationModel match {
//      case DiscussionNotificationModel.NOTIFY_STUDENTS =>
//        getPost.getContainer.getCourse.asScala.fold(Iterable.empty[Long])(courseStudents)
//      case DiscussionNotificationModel.NO_NOTIFICATION =>
//        Iterable.empty
//    }

  override def audience: Iterable[Long] =
    courseInstructorIds(data.discussionId.contextId.id)

  /** Gather all the actively enrolled instructors in a course. */
  private def courseInstructorIds(courseId: Long): Iterable[Long] =
    courseEnrollmentService.getEnrolledInstructorDTOs(courseId).map(_.getId.longValue)

  /** Get a notification email initializer. */
  override def emailInfo =
    Some(Notification.EmailInfo(classOf[PostNotificationEmail], Email.Init(Some(getId), None)))

  private lazy val data = self.getData(classOf[PostNotificationDto])
end PostNotificationImpl

object PostNotification:

  def subscriptionPath(edgePath: EdgePath, postPath: Seq[PostId]): SubscriptionPath =
    SubscriptionPath((Seq(edgePath.toString) ++ postPath.map(_.toString)).toList)

final case class PostNotificationInit(poster: UserId, discussionId: ContentIdentifier, post: Post)
final case class PostNotificationDto(
  discussionId: ContentIdentifier,
  postId: JLong,
  threadId: JLong,
  postPath: Seq[JLong]
)
