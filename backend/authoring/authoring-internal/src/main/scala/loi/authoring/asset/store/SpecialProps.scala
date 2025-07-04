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
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import loi.authoring.asset.factory.SpecialPropsConfig

/** The special properties of an asset data structure. Special properties are framework- reserved properties that are
  * mixed in with the other properties of an asset data structure. The framework will imbue various special meanings to
  * the values of special properties. For example, the title value can be used as a search term.
  *
  * An asset data type does not extend this case class. It is for internal use.
  */
final case class SpecialProps(
  title: String,
  subtitle: String,
  description: String,
  keywords: String,
  archived: Boolean,
  attachmentId: Option[Long]
):

  /** Creates a JSON representation of these properties according to the given special props config. If the config omits
    * a property than the JSON representation omits the value.
    *
    * @param config
    *   the special props config
    * @return
    *   json
    */
  def toJson(config: SpecialPropsConfig[?]): ObjectNode =

    val objNode = JsonNodeFactory.instance.objectNode()
    if config.title then objNode.put("title", title)
    if config.subtitle then objNode.put("subtitle", subtitle)
    if config.desc then objNode.put("description", description)
    if config.keywords then objNode.put("keywords", keywords)
    if config.archived then objNode.put("archived", archived)
    if config.attachmentId && attachmentId.isDefined then
      objNode
        .put("attachmentId", attachmentId.get)

    objNode
  end toJson
end SpecialProps

object SpecialProps:

  /** Factory method for `SpecialProps`, extracting values from JSON
    */
  def extract(node: JsonNode): SpecialProps =

    val title        = Option(node.path("title").textValue()).getOrElse("")
    val subtitle     = Option(node.path("subtitle").textValue()).getOrElse("")
    val desc         = Option(node.path("description").textValue()).getOrElse("")
    val keywords     = Option(node.path("keywords").textValue()).getOrElse("")
    val archived     = node.path("archived").asBoolean(false)
    val attachmentId = Option(node.get("attachmentId")).map(_.asLong(0L))

    SpecialProps(title, subtitle, desc, keywords, archived, attachmentId)
  end extract
end SpecialProps
