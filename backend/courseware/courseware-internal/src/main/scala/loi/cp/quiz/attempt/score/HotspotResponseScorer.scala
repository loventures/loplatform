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

package loi.cp.quiz.attempt.score

import loi.cp.assessment.BasicScore
import loi.cp.quiz.attempt.ResponseScores
import loi.cp.quiz.attempt.selection.HotspotSelection
import loi.cp.quiz.question.hotspot.Hotspot

object HotspotResponseScorer:
  def score(selection: HotspotSelection, question: Hotspot): BasicScore =
    if selection.point
        .flatMap(question.selectedChoice)
        .exists(_.correct)
    then ResponseScores.allOf(question)
    else ResponseScores.zero(question)
