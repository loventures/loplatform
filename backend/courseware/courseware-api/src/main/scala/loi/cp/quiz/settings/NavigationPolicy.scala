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

package loi.cp.quiz.settings

import argonaut.CodecJson
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}

/** A policy describing how a user may navigate through a quiz attempt.
  */
@JsonSerialize(`using` = classOf[NavigationPolicy.NavigationPolicySerializer])
@JsonDeserialize(`using` = classOf[NavigationPolicy.NavigationPolicyDeserializer])
sealed trait NavigationPolicy

object NavigationPolicy:
  private final val typeProperty         = "policyType"
  private final val singlePageType       = "singlePage"
  private final val pagedType            = "paged"
  private final val skippingProperty     = "skippingAllowed"
  private final val backtrackingProperty = "backtrackingAllowed"

  /** A class for Jackson to use to serialize a [[NavigationPolicy]].
    *
    * Jackson abhors case objects, so we have to do this manually.
    */
  // TODO: just use the argo stuff below
  private[settings] class NavigationPolicySerializer extends JsonSerializer[NavigationPolicy]:
    override def serialize(value: NavigationPolicy, gen: JsonGenerator, serializers: SerializerProvider): Unit =
      gen.writeStartObject()

      value match
        case SinglePage =>
          gen.writeStringField(typeProperty, singlePageType)

        case Paged(skippingAllowed, backtrackingAllowed) =>
          gen.writeStringField(typeProperty, pagedType)
          gen.writeBooleanField(skippingProperty, skippingAllowed)
          gen.writeBooleanField(backtrackingProperty, backtrackingAllowed)

      gen.writeEndObject()
    end serialize
  end NavigationPolicySerializer

  /** A class for Jackson to use to deserialize an {{AttemptLimit}}.
    */
  private[settings] class NavigationPolicyDeserializer extends JsonDeserializer[NavigationPolicy]:
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): NavigationPolicy =
      val mapper         = new ObjectMapper
      val json: JsonNode = mapper.readTree[JsonNode](p)

      val typeValue: String = json.get(typeProperty).asText()
      if typeValue.equals(singlePageType) then SinglePage
      else if typeValue.equals(pagedType) then
        val skippingAllowed: Boolean     = json.get(skippingProperty).asBoolean()
        val backtrackingAllowed: Boolean = json.get(backtrackingProperty).asBoolean()
        Paged(skippingAllowed, backtrackingAllowed)
      else
        throw new IllegalArgumentException(
          s"Illegal type ($typeValue) encountered parsing NavigationPolicy: $typeValue"
        )
    end deserialize
  end NavigationPolicyDeserializer

  implicit val codec: CodecJson[NavigationPolicy] =
    import argonaut.*, Argonaut.*
    implicit val pagedCodec: CodecJson[Paged] = CodecJson.derive[Paged]
    def encode(np: NavigationPolicy)          = np match
      case SinglePage          =>
        Json.jSingleObject(typeProperty, jString(singlePageType))
      case paged @ Paged(_, _) =>
        paged.asJson.withObject((typeProperty := pagedType) +: _)
    def decode(hc: HCursor)                   =
      (hc --\ typeProperty).as[String].flatMap[NavigationPolicy] {
        case `singlePageType` => DecodeResult.ok(SinglePage)
        case `pagedType`      => hc.as[Paged].map(p => p) // .widen
        case other            =>
          DecodeResult.fail(s"unknown navigation policy type: $other", hc.history)
      }
    CodecJson(encode, decode)
  end codec
end NavigationPolicy

/** Attempts are navigated as a single page. Skipping does not exist for this navigation policy.
  */
case object SinglePage extends NavigationPolicy

/** Attempts are navigated as a page per question. Skipping a page/question may or may not be allowed.
  *
  * @param skippingAllowed
  *   whether skipping of a page is allowed
  * @param backtrackingAllowed
  *   whether the learner is allowed to re-answer submitted responses
  */
case class Paged(skippingAllowed: Boolean, backtrackingAllowed: Boolean) extends NavigationPolicy
