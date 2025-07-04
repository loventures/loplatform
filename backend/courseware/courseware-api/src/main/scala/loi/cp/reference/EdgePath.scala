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

import java.util.UUID

import argonaut.{CodecJson, DecodeJson, EncodeJsonKey}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.component.web.converter.StringConverter
import com.learningobjects.cpxp.scala.json.SimpleConverter
import org.apache.commons.codec.digest.DigestUtils
import scalaz.*
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

/** A path comprised of an opaque string.
  *
  * Due to reuse and other forms of evil, a node name does not uniquely identify content in a course. The edge path
  * provides a way to differentiate content which is semantically different yet backed by the same asset node. Course
  * customisation renders these potentially confuseworthy so they are subsequently hashed.
  *
  * @param hash
  *   the edge name hash
  */
@JsonSerialize(`using` = classOf[ToStringSerializer])
@JsonDeserialize(converter = classOf[EdgePath.Deserializer])
@StringConvert(`using` = classOf[EdgePath.EdgePathStringConverter])
final case class EdgePath private[reference] (private val hash: String):
  override def toString: String = hash

object EdgePath extends EdgePathCodecs:
  private final val RootStr         = "_root_"
  private final val Separator       = '_'
  private final val SeparatorString = "" + Separator

  final val Root = new EdgePath(RootStr)

  private def apply(hash: String) = new EdgePath(hash)

  def apply(names: List[UUID]): EdgePath = new EdgePath(hash(names))

  def apply(names: UUID*): EdgePath = new EdgePath(hash(names))

  private def hash(names: Seq[UUID]): String =
    names.isEmpty.fold(RootStr, hash(names.mkString(SeparatorString)))

  private[reference] def hash(string: String): String =
    DigestUtils.md5Hex(string)

  /** Parse a string to an edge path. This supports decoding legacy edge paths. */
  def parse(in: String): EdgePath = // '-' implies uuid implies legacy edge path
    if !in.contains('-') then new EdgePath(in) else new EdgePath(hash(in))

  implicit val edgePathEqual: Equal[EdgePath] = Equal.equalBy(_.hash)

  private[reference] final class Deserializer extends SimpleConverter(parse)

  class EdgePathStringConverter extends StringConverter[EdgePath]:
    override def apply(input: StringConverter.Raw[EdgePath]): Option[EdgePath] =
      Option(input).map(i => EdgePath.parse(i.value))
end EdgePath

trait EdgePathCodecs:
  implicit val edgePathCodec: CodecJson[EdgePath] =
    CodecJson.derived[String].xmap(EdgePath.parse)(_.toString)

  implicit val edgePathKeyCodec: EncodeJsonKey[EdgePath] =
    EncodeJsonKey.from[EdgePath](_.toString)

  implicit def edgePathMapDecodeJson[T](implicit decode: DecodeJson[Map[String, T]]): DecodeJson[Map[EdgePath, T]] =
    DecodeJson(json => decode(json).map(_.map(t => EdgePath.parse(t._1) -> t._2)))
