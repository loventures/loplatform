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

package loi.cp.lti

import java.time.Instant
import loi.cp.lti.LtiItemSyncStatus.LtiItemSyncStatusHistory
import loi.cp.lti.ags.AgsLineItem
import loi.cp.reference.EdgePath
import scalaz.std.map.*
import scalaz.syntax.nel.*
import scalaz.syntax.semigroup.*
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

/** A Course-level configuration of EdgePaths with their associated `LineItem`s. A `LineItem` is an external
  * representation of a gradebook column in LO. The `results` url from the LineItem will be used send grades for
  * individual students back to the consumer.
  *
  * @param systemId
  *   the system which these configurations reference
  * @param lineItems
  * @param lastManualSync
  *   the last time a user manually synced the entire gradebook
  */
case class CourseColumnIntegrations(
  systemId: Long,
  lineItemsUrl: String,
  lineItems: Map[EdgePath, LtiItemSyncStatusHistory[AgsLineItem]],
  lastManualSync: Option[Instant] = None
):

  def isSynced(edgePath: EdgePath) =
    lineItems.get(edgePath).fold(false)(_.isSynced)

  def configContains(e: EdgePath): Boolean = lineItems.contains(e)

  def pushAgsStatus(edgePath: EdgePath, status: LtiItemSyncStatus[AgsLineItem]): CourseColumnIntegrations =
    this.copy(lineItems = this.lineItems |+| Map(edgePath -> status.wrapNel))
end CourseColumnIntegrations

object CourseColumnIntegrations:

  import argonaut.*

  def empty(systemId: Long, customLineItemsUrl: String) =
    CourseColumnIntegrations(systemId, customLineItemsUrl, Map.empty)

  val key = "courseColumnIntegrations"

  implicit val codec: CodecJson[CourseColumnIntegrations] =
    CodecJson.casecodec4(CourseColumnIntegrations.apply, ArgoExtras.unapply)(
      "systemId",
      "lineItemsUrl",
      "lineItems",
      "lastManualSync"
    )
end CourseColumnIntegrations
