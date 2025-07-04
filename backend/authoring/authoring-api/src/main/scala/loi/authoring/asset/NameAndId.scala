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

package loi.authoring.asset

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode}

import java.util.UUID

/** Asset name and id, serializes/deserializes as `name;id`
  */
// I hate the Jackson scala module so much
@JsonDeserialize(`using` = classOf[NameAndIdDeserializer])
final case class NameAndId(name: UUID, id: Long):
  @JsonValue
  def json: Any = if name == null then id else s"$name;$id"

  // unlike `.json`, this method uses a string in the legacy `id` form. Note that the
  // legacy form will cease to exist after we are satisfied that
  // AddNodeNameToGradebookTemplateStartupTask has done its duty
  override def toString: String = if name == null then s"$id" else s"$name;$id"

class NameAndIdDeserializer extends StdDeserializer[NameAndId](classOf[NameAndId]):
  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ): NameAndId =

    val node = p.readValueAsTree[JsonNode]()
    NameAndIdDeserializer.deserialize(node.asText())

object NameAndIdDeserializer:
  def deserialize(value: String): NameAndId =

    try
      val idOnly = value.toLong

      // 9/6/2017: remove me once all gradebook templates are upgraded to have name and id
      NameAndId(null, idOnly)

    catch
      case ex: NumberFormatException =>
        val tokens = value.split(";")
        if tokens.length == 2 then
          val uuid = if tokens(0) == null then null else parseUuid(tokens(0))
          NameAndId(uuid, tokens(1).toLong)
        else throw new RuntimeException(s"failed to split NameAndId: $value")

  private def parseUuid(value: String): UUID =
    try UUID.fromString(value)
    catch case ex: IllegalArgumentException => null
end NameAndIdDeserializer
