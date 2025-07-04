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

import loi.cp.assessment.{ResponseScore, RubricScore}
import scalaz.\/
import scalaz.syntax.either.*

object RubricScoreValidationUtils:

  /** Checks that the given {{score}} is valid for the given {{rubric}}
    *
    * @param rubric
    *   the rubric to validate against
    * @param score
    *   the score to validate
    * @param submitResponse
    *   Whether this response should be validated as a draft or submission.
    * @return
    *   the validated score
    */
  private def validateRubricScore(
    rubric: AssessmentRubric,
    score: RubricScore,
    submitResponse: Boolean
  ): InvalidRubricResponseScore \/ Unit =
    if !submitResponse || score.criterionScores.keySet.size == rubric.sections.size then ().right
    else InvalidRubricResponseScore(rubric.sections.size, score.criterionScores.keySet.size).left

  def validateScore(
    possibleRubric: Option[AssessmentRubric],
    score: Option[ResponseScore],
    submitResponse: Boolean
  ): RubricScoringFailure \/ Unit =
    (possibleRubric, score) match
      case (Some(rubric), Some(rs: RubricScore)) => validateRubricScore(rubric, rs, submitResponse)
      case (None, Some(rs: RubricScore))         => MismatchedRubricScore(rs).left
      case (_, _)                                => ().right
end RubricScoreValidationUtils
