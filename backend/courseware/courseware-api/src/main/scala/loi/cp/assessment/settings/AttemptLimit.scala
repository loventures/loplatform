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

package loi.cp.assessment.settings

import argonaut.*
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import org.apache.commons.lang3.StringUtils

/** A potential cap on the number of attempts a single user can start.
  */
@JsonSerialize(`using` = classOf[AttemptLimit.AttemptLimitSerializer])
@JsonDeserialize(`using` = classOf[AttemptLimit.AttemptLimitDeserializer])
sealed trait AttemptLimit

object AttemptLimit:

  /** Creates an {{AttemptLimit}} from a single number.
    *
    * @param limit
    *   the maximium number of attempts; otherwise, {{None}} if unlimited
    * @return
    *   the AttemptLimit for the given number.
    */
  def of(limit: Option[Long]): AttemptLimit =
    limit match
      case Some(max) => Limited(max.toInt)
      case None      => Unlimited

  /** A class for Jackson to use to serialize an {{AttemptLimit}}.
    */
  private[settings] class AttemptLimitSerializer extends JsonSerializer[AttemptLimit]:
    override def serialize(value: AttemptLimit, gen: JsonGenerator, serializers: SerializerProvider): Unit =
      value match
        case Unlimited    => gen.writeNull()
        case Limited(max) => gen.writeNumber(max)

  /** A class for Jackson to use to deserialize an {{AttemptLimit}}.
    */
  private[settings] class AttemptLimitDeserializer extends JsonDeserializer[AttemptLimit]:
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): AttemptLimit =
      val mapper      = new ObjectMapper
      val str: String = mapper.readValue(p, classOf[String])

      if StringUtils.isEmpty(str) then Unlimited
      else Limited(str.toInt)

    override def getNullValue(ctxt: DeserializationContext): AttemptLimit = Unlimited

  def fail: (String, CursorHistory) => AttemptLimit = (_, _) => Unlimited
  def success: Int => AttemptLimit                  = number => Limited(number)

  implicit val attemptLimitCodec: CodecJson[AttemptLimit] =
    CodecJson(
      {
        case Unlimited      => Json.jNull
        case Limited(value) => Json.jNumber(value)
      },
      cursor =>
        if cursor.focus.isNull then DecodeResult.ok(Unlimited)
        else cursor.jdecode[Int].map(Limited.apply)
    )
end AttemptLimit

/** An object representing a value that has no limit to the number of attempts.
  */
case object Unlimited extends AttemptLimit

/** An object representing a finite number of valid attempts allowed.
  *
  * @param max
  *   the maximum number of valid attempts allowed
  */
case class Limited(max: Int) extends AttemptLimit
