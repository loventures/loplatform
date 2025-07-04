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

package loi.cp.quiz.question

import loi.cp.quiz.attempt.DistractorOrder

/** A question that may allow randomization of its distractors.
  */
trait RandomizableQuestion extends AutoScorable:

  /** whether this particular question declares that is /should/ randomize distractors */
  val allowDistractorRandomization: Boolean

  /** Generates a distractor order for this question. This method does not check [[allowDistractorRandomization]].
    *
    * @return
    *   a distractor order for this question
    */
  def generateDistractorOrder(): DistractorOrder
end RandomizableQuestion
