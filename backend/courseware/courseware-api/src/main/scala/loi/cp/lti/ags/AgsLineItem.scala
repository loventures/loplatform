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

package loi.cp.lti.ags

import argonaut.CodecJson
import scaloi.json.ArgoExtras

/** @param id
  *   the url of this line item resource
  * @param scoreMaximum
  *   the maximum allowable points
  * @param label
  *   The display name for this line item
  * @param tag
  *   provider-defined metadata
  * @param resourceId
  *   A convenience provider-defined identifier
  * @param resourceLinkId
  */
case class AgsLineItem(
  id: String,
  scoreMaximum: BigDecimal,
  label: String,
  tag: Option[String],
  resourceId: String,
  resourceLinkId: Option[String]
)

object AgsLineItem:

  implicit val codec: CodecJson[AgsLineItem] = CodecJson.casecodec6(
    AgsLineItem.apply,
    ArgoExtras.unapply
  )("id", "scoreMaximum", "label", "tag", "resourceId", "resourceLinkId")
