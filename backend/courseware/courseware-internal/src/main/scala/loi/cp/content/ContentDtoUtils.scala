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

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.competency.Competency
import loi.cp.content.gate.GateSummary
import loi.cp.context.ContextId
import loi.cp.lwgrade.Grade
import loi.cp.progress.report.Progress
import loi.cp.reference.EdgePath

import java.util.UUID

/** A utils for composing REST objects from [[CourseContent]].
  */
@Service
trait ContentDtoUtils:

  /** Builds a REST object for the given [[CourseContent]] in the presence of its witnesses.
    *
    * @param content
    *   the content
    * @param course
    *   the course containing the content
    * @param index
    *   the index of the content in its parent
    * @param parents
    *   the edge paths of all the content's parents
    * @param dueDate
    *   the due date
    * @param dueDateExempt
    *   if exempt from duedate
    * @param progress
    *   course progress
    * @param grade
    *   the grade for this particular content
    * @param competencies
    *   all competencies related to the content, either directly or through the content's activity
    * @param hasSurvey
    *   whether a survey is associated with this content
    * @return
    *   a REST object
    */
  def toDto(
    content: CourseContent,
    course: ContextId,
    index: Int,
    parents: List[EdgePath],
    gateSummary: GateSummary,
    dueDate: Option[DueDate],
    dueDateExempt: Option[Boolean],
    progress: Option[Progress],
    grade: Option[Grade],
    competencies: Seq[Competency],
    hasSurvey: Boolean,
    gradebookCategories: Map[UUID, GradebookCategory],
    edgePaths: Map[UUID, EdgePath],
  ): CourseContentDto
end ContentDtoUtils
