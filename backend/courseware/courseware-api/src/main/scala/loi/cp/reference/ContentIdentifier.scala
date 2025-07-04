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

package loi.cp.reference

import argonaut.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.component.web.converter.StringConverter
import loi.cp.context.{ContextId, HasContextId}
import org.apache.commons.lang3.StringUtils
import scalaz.syntax.std.map.*
import scaloi.syntax.string.*

/** A reference to a usage of content inside a particular context's structure.
  *
  * This is analogous to a content item id in the old course model.
  *
  * The form is `contextId:edgePath`, where `edgePath` is the stringified form of an [[EdgePath]].
  *
  * @param contextId
  *   the particular context in question
  * @param edgePath
  *   the location in the particular content being referenced
  */
@StringConvert(`using` = classOf[ContentIdentifier.ContentIdentifierStringConverter])
@JsonSerialize(`using` = classOf[ToStringSerializer])
@JsonDeserialize(`using` = classOf[ContentIdentifier.ContentIdentifierDeserializer])
case class ContentIdentifier(override val contextId: ContextId, edgePath: EdgePath) extends HasContextId:
  override def toString: String =
    contextId.value.toString + ContentIdentifier.Separator + edgePath.toString

object ContentIdentifier:
  final val Separator = '.'

  implicit val contentIdentifierKeyEncoder: EncodeJsonKey[ContentIdentifier] = EncodeJsonKey.from(_.toString)

  implicit def identifierDecode: DecodeJson[ContentIdentifier] = DecodeJson { cursor =>
    cursor.as[String] flatMap { strVal =>
      ContentIdentifier(strVal) match
        case Some(ci) => DecodeResult.ok(ci)
        case None     => DecodeResult.fail(s"Invalid identifier format: $strVal", cursor.history)
    }
  }

  implicit def identifierEncode: EncodeJson[ContentIdentifier] = EncodeJson { identifier =>
    Argonaut.jString(identifier.toString)
  }

  implicit def contentIdentifierMapCodec[V: EncodeJson: DecodeJson]: CodecJson[Map[ContentIdentifier, V]] =
    CodecJson
      .derived[Map[String, V]]
      .xmap(_.mapKeys(v => ContentIdentifier(v).get))(_.mapKeys(_.toString))

  def apply(str: String): Option[ContentIdentifier] =
    val split: Array[String]               = str.split(Separator)
    val possibleContextId: Option[Long]    = Option(split(0))
      .filter({ str =>
        if str.startsWith("-") then StringUtils.isNumeric(str.substring(1))
        else StringUtils.isNumeric(str)
      })
      .flatMap(_.toLong_?)
    val onlyOneSeparator: Boolean          = str.count(_ == Separator) == 1 // Split can't detect this in some situations
    val possiblePathString: Option[String] =
      if onlyOneSeparator && split.length == 2 then Some(split(1))
      else None

    for
      contextId  <- possibleContextId
      pathString <- possiblePathString
      path        = EdgePath.parse(pathString)
    yield ContentIdentifier(ContextId(contextId), path)
  end apply

  def unapply(contentIdentifier: String): Option[(Long, EdgePath)] =
    apply(contentIdentifier).map(ci => (ci.contextId.value, ci.edgePath))

  /** A class for Jackson to use to deserialize a [[ContentIdentifier]].
    */
  private[reference] class ContentIdentifierDeserializer extends JsonDeserializer[ContentIdentifier]:
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): ContentIdentifier =
      val str: String = p.getValueAsString
      ContentIdentifier(str).getOrElse {
        throw new ContentIdentifierFormatException(str)
      }

  class ContentIdentifierStringConverter extends StringConverter[ContentIdentifier]:
    override def apply(input: StringConverter.Raw[ContentIdentifier]): Option[ContentIdentifier] =
      if input == null || StringUtils.isEmpty(input.value) then None
      else ContentIdentifier(input.value)

  implicit class ContentIdsOps(contentIdentifiers: Seq[ContentIdentifier]):
    def toContentMap: Map[Long, Seq[EdgePath]] = contentIdentifiers.groupMap(_.contextId.value)(_.edgePath)
end ContentIdentifier
