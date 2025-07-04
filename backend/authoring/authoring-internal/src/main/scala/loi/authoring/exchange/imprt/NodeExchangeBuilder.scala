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

package loi.authoring.exchange.imprt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import loi.asset.license.License
import loi.authoring.exchange.model.{EdgeExchangeData, NodeExchangeData}
import org.apache.commons.lang3.StringUtils

class NodeExchangeBuilder(var exchange: NodeExchangeData):

  def title(newTitle: String): NodeExchangeBuilder =
    addToData("title", StringUtils.abbreviate(Option(StringUtils.trimToNull(newTitle)).getOrElse("[no title]"), 255))
    this

  def html(html: String): NodeExchangeBuilder =
    addToData("html", html)
    this

  def licenseAndAuthor(licenseAndAuthor: (License, String)): NodeExchangeBuilder =
    val data: ObjectNode = exchange.data.deepCopy()
    data
      .put("license", licenseAndAuthor._1.abbreviation)
      .put("author", licenseAndAuthor._2)
    exchange = exchange.copy(data = data)
    this

  def description(description: Option[String]): NodeExchangeBuilder =
    description.map(addToData("description", _))
    this

  def keywords(keywords: Option[String]): NodeExchangeBuilder =
    keywords.map(kwords => addToData("keywords", StringUtils.abbreviate(kwords, 255)))
    this

  def edges(edges: Seq[EdgeExchangeData]): NodeExchangeBuilder =
    exchange = exchange.copy(edges = edges)
    this

  def attachment(attachment: String): NodeExchangeBuilder =
    exchange = exchange.copy(attachment = Some(attachment))
    this

  def build(): NodeExchangeData = exchange

  // ////

  private def addToData(field: String, value: String) =
    val data: ObjectNode = exchange.data.deepCopy()
    if field == "subtitle" then exchange = exchange.copy(data = data.put(field, StringUtils.abbreviate(value, 255)))
    else exchange = exchange.copy(data = data.put(field, value))
end NodeExchangeBuilder

object NodeExchangeBuilder:
  def builder(guid: String, typeId: String): NodeExchangeBuilder =
    builder(guid, typeId, JsonNodeFactory.instance.objectNode())

  def builder(guid: String, typeId: String, data: JsonNode) =
    val exchange = NodeExchangeData(
      id = guid,
      typeId = typeId,
      data = data,
      edges = Seq(),
      attachment = None
    )
    new NodeExchangeBuilder(exchange)
end NodeExchangeBuilder
