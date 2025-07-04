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

package loi.cp.quiz.attempt

import loi.cp.assessment.BasicScore
import loi.cp.quiz.question.Question
import org.apache.commons.math3.util.Precision

object ResponseScores:
  def allOf(question: Question): BasicScore =
    BasicScore(question.pointValue, question.pointValue)

  def of(pointsAwarded: Double, question: Question): BasicScore =
    if pointsAwarded >= 0.0 then BasicScore(pointsAwarded, question.pointValue)
    else BasicScore(0.0, question.pointValue)

  def allOrNothingOf(pointsAwarded: Double, question: Question): BasicScore =
    if pointsAwarded + Precision.EPSILON >= question.pointValue then allOf(question)
    else zero(question)

  def zero(question: Question): BasicScore =
    ResponseScores.of(0.0, question)
end ResponseScores
