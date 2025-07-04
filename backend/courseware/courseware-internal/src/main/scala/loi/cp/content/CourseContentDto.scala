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

package loi.cp.content

import java.time.Instant
import java.util.UUID
import argonaut.Json
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT
import com.fasterxml.jackson.annotation.{JsonCreator, JsonInclude, JsonProperty, JsonValue}
import loi.asset.assessment.model.*
import loi.asset.course.model.Course
import loi.asset.discussion.model.Discussion1
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.html.model.{Html, Scorm}
import loi.asset.lesson.model.Lesson
import loi.asset.lti.Lti
import loi.asset.module.model.Module
import loi.asset.resource.model.Resource1
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.competency.Competency
import loi.cp.content.gate.{ContentGateDto, GateStatus}
import loi.cp.lwgrade.Grade
import loi.cp.path.Path
import loi.cp.progress.report.Progress
import loi.cp.reference.{ContentIdentifier, EdgePath}

import scala.annotation.meta.getter

/** A DTO containing information on course contents.
  *
  * This contains navigation information, and potentially also progress and activity data.
  *
  * @param id
  *   the identifier of this content in the course. Note that this may not be a database PK; in "lightweight" courses it
  *   will be an [[loi.cp.reference.EdgePath]] or similar.
  * @param name
  *   the "name" of the content, by which is meant "title". (No relation whatsoever to asset node names.)
  * @param description
  *   a description of the content if it exists
  * @param contentType
  *   the component identifier of the content item which would back this DTO. (In lightweight courses, there is no
  *   content item; this is the identifier of the component which in an old course would be used.) TODO: move away from
  *   this.
  * @param index
  *   the logical index of this content in the navigation tree. This is derived from the incoming edge's position.
  * @param path
  *   the path to this content. That is, a [[Path]] starting at the root of the course and including all of this
  *   content's parents' IDs, as well as this content's ID itself.
  * @param logicalGroup
  *   the edge group of the incoming edge. Usually "elements", but other values are possible.
  * @param depth
  *   the depth of this content in the content tree. Purportedly, `path.size`.
  * @param typeId
  *   the type of the asset backing this content.
  * @param subType
  *   if this is a legacy resource, its `ResourceType`. `None` for all other content types.
  * @param iconClass
  *   the CSS class for the icon to be displayed in delivery. Yes, it's a bit strange for it to be on the backend but we
  *   do allow authors to customize this.
  * @param dueDate
  *   the due date of this content, if authored.
  * @param dueDateExempt
  *   whether the user is exempt from the due date.
  * @param duration
  *   the amount of time, in minutes, which the author of this content estimates a student should be spending on it.
  * @param hasGradebookEntry
  *   whether this content has a matching column in the course's gradebook.
  * @param gatingInformation
  *   information about the gating status of this content.
  * @param progress
  *   information about the user's progress against this content.
  * @param grade
  *   the user's current grade against this content item.
  * @param competencies
  *   all competencies related to the content, either directly or through the content's activity
  * @param metadata
  *   arbitrary metadata that are associated with this content via instructor customisation
  * @param hasSurvey
  *   whether this content has an associated survey.
  */
final case class CourseContentDto(
  id: String,
  parent_id: Option[String],
  name: String,         // TODO: rename to "title" like we call it elsewhere
  description: Option[String],
  contentType: String,  // TODO: we should not still be using this... kill
  index: Long,
  path: Path,
  logicalGroup: String, // TODO: always "elements"... kill
  depth: Int,
  assetId: Long,
  typeId: AssetTypeId,
  subType: Option[String],
  iconClass: Option[IconClass],
  dueDate: Option[Instant],
  dueDateExempt: Option[Boolean],
  maxMinutes: Option[Long],
  duration: Option[Long],
  hasGradebookEntry: Boolean,
  @JsonProperty(CourseContentDto.NodeNameProperty)
  nodeName: UUID,
  gatingInformation: GatingInformation,
  @JsonInclude(NON_ABSENT) progress: Option[Progress],
  @JsonInclude(NON_ABSENT) grade: Option[Grade],
  contentId: ContentIdentifier,
  competencies: Seq[Competency],
  metadata: Option[Json],
  hasSurvey: Boolean,
  isForCredit: Option[Boolean],
  accessControlled: Option[Boolean],
  gradebookCategory: Option[String],
  hyperlinks: Map[UUID, EdgePath],
  bannerImage: Option[UUID],
)

object CourseContentDto:
  /* using JS objects with a `nodeName` property gets dicey (angular/angular.js#11629)
   * so we're sending this down with an abnormally serpentine identifier. */
  final val NodeNameProperty = "node_name"

final case class GatingInformation(
  gate: Option[ContentGateDto],
  gateStatus: GateStatus,
)

object GatingInformation:
  val None = GatingInformation(scala.None, GateStatus.Open)

// jackson...
final case class IconClass @JsonCreator() (@(JsonValue @getter) value: String)

object IconClass extends (String => IconClass):

  def apply(a: Asset[?]): Option[IconClass] = PartialFunction.condOpt(a.data) {
    case c: Course                 => this(c.iconCls)
    case m: Module                 => this(m.iconCls)
    case l: Lesson                 => this(l.iconCls)
    case r: Resource1              => this(r.iconCls)
    case d: Discussion1            => this(d.iconCls)
    case h: Html                   => this(h.iconCls)
    case s: Scorm                  => this(s.iconCls)
    case f: FileBundle             => this(f.iconCls)
    case a: Assessment             => this(a.iconCls)
    case c: Checkpoint             => this(c.iconCls)
    case d: Diagnostic             => this(d.iconCls)
    case p: PoolAssessment         => this(p.iconCls)
    case a: Assignment1            => this(a.iconCls)
    case i: ObservationAssessment1 => this(i.iconCls)
    case l: Lti                    => this(l.iconCls)
  }
end IconClass
