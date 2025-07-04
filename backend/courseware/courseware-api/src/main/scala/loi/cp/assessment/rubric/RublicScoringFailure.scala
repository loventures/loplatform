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

import loi.cp.Widen
import loi.cp.assessment.RubricScore

/** A failure when scoring a response with a rubric.
  */
sealed abstract class RubricScoringFailure(val message: String) extends Widen[RubricScoringFailure]

/** A user provided a rubric score when rubric is not available on the question.
  *
  * @param score
  *   the score provided
  */
case class MismatchedRubricScore(score: RubricScore)
    extends RubricScoringFailure(s"A rubric score $score was provided for a non-rubric response")

/** A user provided a rubric score that does not correspond to the question's rubric (e.g a score with three criteria
  * was provided when the rubric only contains two criteria).
  *
  * @param expectedSection
  *   the number of sections in the question
  * @param givenSections
  *   the number of sections given in the score
  */
case class InvalidRubricResponseScore(expectedSection: Int, givenSections: Int)
    extends RubricScoringFailure(
      s"Invalid Score: rubric had $expectedSection sections, score had $givenSections sections"
    )
