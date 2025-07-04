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

package loi.authoring.asset.store

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils

/** A low-level representation of asset data that separates the special properties from the regular properties of asset
  * data. The special properties are separated for easy access since they are stored in their own columns.
  */
case class JsonAndSpecialProps(
  jsonNode: JsonNode,
  specialProps: SpecialProps
):
  val json: String = jsonNode.toString
object JsonAndSpecialProps:

  private val mapper = JacksonUtils.getFinatraMapper

  /** Extracts the special properties from the regular properties in `node`, but leaves the special properties in the
    * json
    */
  def extract(data: Any): JsonAndSpecialProps =
    val node: JsonNode = mapper.valueToTree[JsonNode](data)
    val specialProps   = SpecialProps.extract(node)
    JsonAndSpecialProps(node, specialProps)
end JsonAndSpecialProps
