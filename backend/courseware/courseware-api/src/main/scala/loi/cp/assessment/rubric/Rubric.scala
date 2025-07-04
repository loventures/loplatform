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

package loi.cp.assessment.rubric

import java.util.UUID

import loi.asset.rubric.model.{RubricCriterion, RubricCriterionLevel}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency

final case class AssessmentRubric(nodeName: UUID, title: String, sections: Seq[AssessmentRubricCriterion]):

  /** Returns all competencies associated with this rubric.
    *
    * @return
    *   competencies associated with this rubric
    */
  def competencies: Seq[Competency] = sections.flatMap(_.competencies).distinct

final case class AssessmentRubricCriterion(
  name: UUID,
  title: String,
  description: String,
  levels: Seq[RubricCriterionLevel],
  competencies: Seq[Competency]
)

object AssessmentRubricCriterion:
  def apply(section: Asset[RubricCriterion], competencies: Seq[Competency]): AssessmentRubricCriterion =
    AssessmentRubricCriterion(
      section.info.name,
      section.data.title,
      section.data.description,
      section.data.levels,
      competencies
    )
