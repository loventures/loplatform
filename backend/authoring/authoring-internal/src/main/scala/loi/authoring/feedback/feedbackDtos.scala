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

package loi.authoring.feedback

import argonaut.Json
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.{DeXssSupport, FormattingUtils}
import kantan.csv.{CellEncoder, HeaderEncoder}
import loi.cp.web.HandleService
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*
import scaloi.syntax.option.*

import java.lang
import java.util.{Date, UUID}

private[feedback] final case class NewFeedback(
  project: Long,
  branch: Long,
  assetName: UUID,
  contentName: Option[UUID],
  lessonName: Option[UUID],
  moduleName: Option[UUID],
  unitName: Option[UUID],
  identifier: Option[String],
  quote: Option[String],
  feedback: String,
  attachments: List[UploadInfo],
  assignee: Option[lang.Long],
)

private[feedback] final case class EditFeedback(
  feedback: String,
)

private[feedback] final case class NewReply(
  value: String,
  attachments: List[UploadInfo],
)

private[feedback] final case class EditReply(
  value: String,
)

private[feedback] final case class Transition(
  status: Option[String],
  closed: Boolean
)

private[feedback] final case class Assign(
  assignee: Option[lang.Long]
)

private[feedback] final case class Relocate(
  contentName: Option[UUID],
  lessonName: Option[UUID],
  moduleName: Option[UUID],
  unitName: Option[UUID],
)

private[feedback] final case class FeedbackDto(
  id: Long,
  project: Long,
  branch: Long,
  remotes: List[Long],
  assetName: UUID,
  contentName: Option[UUID],
  lessonName: Option[UUID],
  moduleName: Option[UUID],
  unitName: Option[UUID],
  identifier: Option[String],
  section: Option[FeedbackSectionDto],
  created: Date,
  creator: FeedbackProfileDto,
  role: FeedbackRole,
  modified: Date,
  edited: Option[Date],
  status: Option[String],
  assignee: Option[FeedbackProfileDto],
  quote: Option[String],
  feedback: String,
  attachments: List[Long],
  closed: Boolean,
  replies: Long,
)

private[feedback] object FeedbackDto:
  def apply(feedback: AssetFeedbackFinder)(implicit hs: HandleService): FeedbackDto = new FeedbackDto(
    id = feedback.id,
    project = feedback.project,
    branch = feedback.branch,
    remotes = feedback.remotes.toList.unboxInside(),
    assetName = feedback.assetName,
    contentName = Option(feedback.contentName),
    lessonName = Option(feedback.lessonName),
    moduleName = Option(feedback.moduleName),
    unitName = Option(feedback.unitName),
    identifier = Option(feedback.identifier),
    section = Option(feedback.section).map(FeedbackSectionDto.apply),
    created = feedback.created,
    creator = FeedbackProfileDto(feedback.creator),
    role = FeedbackRole.withName(feedback.role),
    modified = feedback.modified,
    edited = Option(feedback.edited),
    assignee = Option(feedback.assignee).map(FeedbackProfileDto.apply),
    status = Option(feedback.status),
    quote = Option(feedback.quote),
    feedback = DeXssSupport.deXss(feedback.feedback, s"Feedback:${feedback.id}"),
    attachments = feedback.attachments.toList.unboxInside(),
    closed = feedback.closed,
    replies = feedback.replies,
  )
end FeedbackDto

private[feedback] final case class FeedbackSummary(
  assetName: UUID,
  contentName: Option[UUID],
  lessonName: Option[UUID],
  moduleName: Option[UUID],
  unitName: Option[UUID],
  count: Int,
)

private[feedback] final case class FeedbackRow(
  course: Option[String],
  unit: Option[String],
  module: Option[String],
  lesson: Option[String],
  content: Option[String],
  created: Date,
  creator: String,
  modified: Date,
  status: String,
  assignee: Option[String],
  quote: Option[String],
  feedback: String,
  comments: String,
)

private[feedback] object FeedbackRow:
  implicit def feedbackRowHeaderEncoder(implicit dateCellEncoder: CellEncoder[Date]): HeaderEncoder[FeedbackRow] =
    HeaderEncoder.caseEncoder(
      "Course",
      "Unit",
      "Module",
      "Lesson",
      "Content",
      "Created",
      "Creator",
      "Updated",
      "Status",
      "Assignee",
      "Quote",
      "Feedback",
      "Comments",
    )(ArgoExtras.unapply)
end FeedbackRow

private[feedback] final case class FeedbackSectionDto(
  id: Long,
  code: String,
  title: String,
  url: String,
  `type`: String,
)

private[feedback] object FeedbackSectionDto:
  def apply(section: GroupFinder): FeedbackSectionDto = new FeedbackSectionDto(
    id = section.id,
    code = section.groupId,
    title = section.name,
    url = section.url,
    `type` = section.xtype, // GroupConstants.GroupType
  )

final case class FeedbackProfileDto(
  id: Long,
  handle: String,
  givenName: String,
  fullName: String,
  thumbnailId: Option[Long],
)

object FeedbackProfileDto:
  def apply(user: UserFinder)(implicit hs: HandleService): FeedbackProfileDto = new FeedbackProfileDto(
    id = user.id,
    handle = hs.mask(user),
    givenName = user.givenName,
    fullName = FormattingUtils.userStr(user.userName, user.givenName, user.middleName, user.familyName),
    thumbnailId = Option(user.image).unboxMap(_.generation),
  )

  final val Unknown = FeedbackProfileDto(-1, "", "Unknown", "Unknown Person", None)
end FeedbackProfileDto

private[feedback] final case class FeedbackActivityDto(
  id: Long,
  created: Date,
  creator: FeedbackProfileDto,
  edited: Option[Date],
  event: FeedbackEvent,
  value: Option[AnyRef], // for realz.. String | FeedbackProfileDto
  attachments: List[Long],
)

private[feedback] object FeedbackActivityDto:
  def apply(activity: FeedbackActivityFinder)(implicit is: ItemService, hs: HandleService): FeedbackActivityDto =
    val event = FeedbackEvent.withName(activity.event)
    new FeedbackActivityDto(
      id = activity.id,
      created = activity.created,
      creator = FeedbackProfileDto(activity.creator),
      edited = Option(activity.edited),
      event = event,
      value = Option(activity.value).flatMap(activityValue(_, event, activity)),
      attachments = activity.attachments.toList.unboxInside(),
    )
  end apply

  private def activityValue(value: Json, event: FeedbackEvent, activity: FeedbackActivityFinder)(implicit
    is: ItemService,
    hs: HandleService
  ): Option[AnyRef] =
    event match
      case FeedbackEvent.Reply =>
        value.string.map(DeXssSupport.deXss(_, s"FeedbackActivity:${activity.id}"))

      case FeedbackEvent.Status =>
        value.string

      case FeedbackEvent.Assign =>
        value.isNull.noption(
          value.number
            .flatMap(_.truncateToLong.finder_?[UserFinder])
            .cata(FeedbackProfileDto.apply, FeedbackProfileDto.Unknown)
        )
end FeedbackActivityDto
