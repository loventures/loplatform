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

package loi.cp.lwgrade

import argonaut.Argonaut.*
import argonaut.*
import loi.cp.lti.LtiItemSyncStatus.*
import loi.cp.lti.ags.AgsScore
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.reference.EdgePath
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

case class SingleGradeHistory(
  outcomes1: Option[LtiItemSyncStatusHistory[Double]],
  agsScore: Option[LtiItemSyncStatusHistory[AgsScore]],
)

object SingleGradeHistory:

  def fromUserGradeHistory(u: UserGradeSyncHistory, edgepath: EdgePath): SingleGradeHistory =
    SingleGradeHistory(
      u.outcomes1Callbacks.get(edgepath).flatMap(_.statusHistory),
      u.agsScores.get(edgepath),
    )

  def empty: SingleGradeHistory = SingleGradeHistory(None, None)

  implicit val singleCodec: CodecJson[SingleGradeHistory] =
    CodecJson.casecodec2(SingleGradeHistory.apply, ArgoExtras.unapply)(
      "outcomes1",
      "agsScore",
    )
end SingleGradeHistory
