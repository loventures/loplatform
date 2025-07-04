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

package loi.cp.mastery

import argonaut.CodecJson
import scaloi.json.ArgoExtras

import java.util.UUID

/** Whenever you get a grade on something that assesses a competency, we update the content grade, recompute the mastery
  * set.
  */
final case class UserMasteryState(
  competencyMastery: Set[UUID],
  competencyGradeTotal: Map[UUID, Double], // leaf UUID -> SUM(grade)
  competencyGradeCount: Map[UUID, Int],    // leaf UUID -> COUNT(grade)
  contentGrade: Map[UUID, Double],         // question/assignment -> grade
  recomputed: Boolean,                     // has this been recomputed with user history
)

object UserMasteryState:
  import scaloi.json.ArgoExtras.encodeJsonKeyForUuid

  final val Empty: UserMasteryState = UserMasteryState(Set.empty, Map.empty, Map.empty, Map.empty, recomputed = false)

  implicit val JsonUserMasteryCodecJson: CodecJson[UserMasteryState] = CodecJson.casecodec5(
    UserMasteryState.apply,
    ArgoExtras.unapply
  )("competencyMastery", "competencyGradeTotal", "competencyGradeCount", "contentGrade", "recomputed")
