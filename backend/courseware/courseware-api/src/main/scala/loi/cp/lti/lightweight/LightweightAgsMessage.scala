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
package lightweight

import loi.authoring.asset.factory.AssetTypeId
import loi.cp.asset.assessmenttype.AssetAssessmentType
import loi.cp.lwgrade.GradeColumn
import loi.cp.reference.EdgePath

import java.time.Instant
import java.util.UUID

sealed trait LineItemMessage:
  def content: EdgePath
  def contextId: Long

/** @param lineItemsUrl
  * @param content
  * @param contextId
  */
case class CreateLineItemMessage(
  lineItemsUrl: String,
  content: EdgePath,
  contextId: Long,
  assetAssessmentType: Option[AssetAssessmentType],
  assetType: AssetTypeId,
  assetName: UUID,
  column: GradeColumn
) extends LineItemMessage

case class DeleteLineItemMessage(
  lineItemId: String, // a URI
  content: EdgePath,
  contextId: Long,
) extends LineItemMessage

case class SendResultMessage(
  content: EdgePath,
  contextId: Long,
  userId: Long,
  userLmsId: String,
  pointsAwarded: Option[Double],
  totalPossible: Double,
  gradeDate: Instant,
)
