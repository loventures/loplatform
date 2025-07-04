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

package loi.cp.lti.storage

import argonaut.*
import argonaut.Argonaut.*
import loi.cp.lti.LtiItemSyncStatus
import loi.cp.lti.LtiItemSyncStatus.LtiItemSyncStatusHistory
import loi.cp.lti.ags.AgsScore
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStoreable

import scalaz.syntax.semigroup.*
import scalaz.syntax.nel.*
import scalaz.std.map.*
import scaloi.json.ArgoExtras.*

/** This value exists as course storage for each user. It maps edgepaths to the sync history of their grades
  */
case class UserGradeSyncHistory(
  outcomes1Callbacks: Map[EdgePath, LtiCallback],
  agsScores: Map[EdgePath, LtiItemSyncStatusHistory[AgsScore]],
  courseLinkData: Option[CourseLinkData] = None,
):

  def addOutcomes1Callback(ep: EdgePath)(cb: LtiCallback): UserGradeSyncHistory =
    this.copy(outcomes1Callbacks ++ Map(ep -> cb))

  def withOutcomes1Callback(ep: EdgePath)(f: LtiCallback => LtiCallback): UserGradeSyncHistory =
    outcomes1Callbacks.get(ep).map(f).fold(this) { cb => this.copy(outcomes1Callbacks ++ Map(ep -> cb)) }

  def pushOutcomes1Status(ep: EdgePath)(status: LtiItemSyncStatus[Double]) =
    withOutcomes1Callback(ep)(cb => cb.pushStatus(status))

  def pushAgsScoreStatus(ep: EdgePath)(status: LtiItemSyncStatus[AgsScore]): UserGradeSyncHistory =
    this.copy(agsScores = agsScores |+| Map(ep -> status.wrapNel))
end UserGradeSyncHistory

object UserGradeSyncHistory:

  def empty: UserGradeSyncHistory = UserGradeSyncHistory(Map.empty, Map.empty, None)

  implicit val codec: CodecJson[UserGradeSyncHistory] =
    CodecJson.derived(using
      E = EncodeJson.derive[UserGradeSyncHistory],
      D = DecodeJson.derive[UserGradeSyncHistory]
    )

  implicit val storageable: CourseStoreable[UserGradeSyncHistory] =
    CourseStoreable("agsGradeSyncHistory")(empty)
end UserGradeSyncHistory
