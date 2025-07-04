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

package loi.cp.i18n

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode}
import com.learningobjects.cpxp.service.exception.HttpApiException
import org.apache.http.HttpStatus

import java.text.MessageFormat
import java.time.Instant
import java.util
import java.util.{Locale, ResourceBundle}
import scala.annotation.varargs
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions
import scala.util.Try

/** A wrapper for a message that can be internationalized at a later point. The message is kept separate from the
  * parameters to allow multiple translations. A ResourceBundle is also carried with the message to allow translation
  * into multiple languages. This does however assume that the ResourceBundle will be available at the point it is used.
  * The parameters undergo some translation to ensure they are in a format that MessageFormatter can use. Anything that
  * isn't a Number, Date or Instant is converted to a String. Dates and Instances are converted to millis since 1970 -
  * see long comment in code for why Collection and Iterable are converted in a nice way for those who forgot to do that
  * null is converted to the string "NULL" Option is unpacked if present before conversion. If it is None is converted
  * to the string "None" Anything else is converted to a string using the object's toString method
  */
@JsonDeserialize(`using` = classOf[BundleMessageDeserializer])
trait BundleMessage:
  @JsonProperty
  def value: String

  @JsonProperty
  def key: String

  @JsonProperty
  def args: Vector[AnyRef]
end BundleMessage

object BundleMessage:
  def apply(key: String, args: AnyRef*)(implicit bundle: ResourceBundle): BundleMessage =
    new ResourceBundleMessage(bundle, key, args.toVector)

  // Used from ResourceBundleLoader.message but oddly invisible to intellij
  @varargs
  def apply(bundle: ResourceBundle, key: String, args: AnyRef*): BundleMessage =
    new ResourceBundleMessage(bundle, key, args.toVector)

@JsonIgnoreProperties(value = Array("bundle"))
@JsonDeserialize(`using` = classOf[BundleMessageDeserializer])
private[i18n] final class ResourceBundleMessage(
  val bundle: ResourceBundle,
  val key: String,
  creationArgs: Vector[AnyRef] = Vector.empty,
  storedValue: Option[String] = None
) extends BundleMessage:
  require(key ne null, s"Null message key (bundle: ${bundle.getBaseBundleName}, args: $creationArgs)")

  override val args: Vector[AnyRef] = creationArgs.map(unboxOption).map(convertToJsonTypes)

  /** Expands this bundle message using the built-in resource bundle. */
  override def value: String =
    Try(MessageFormat.format(bundle.getString(key), args*)).toOption
      .orElse(storedValue)
      .getOrElse(key + args.mkString("(", ", ", ")"))

  @JsonProperty
  def baseBundleName: String = bundle.getBaseBundleName

  override def equals(that: Any): Boolean = that match
    case that: ResourceBundleMessage =>
      baseBundleName == that.baseBundleName &&
      key == that.key &&
      args == that.args // Note: Doesn't handle recursive collections but we already flattened them to strings
    case _ => false

  override def hashCode: Int = baseBundleName.hashCode +
    31 * key.hashCode +
    1021 * args.hashCode

  private def unboxOption(input: AnyRef): Any = input match
    case Some(s) => s
    case None    => "None"
    case other   => other

  private def convertToJsonTypes(input: Any): AnyRef = input match
    case c: util.Collection[?] => c.asScala.mkString("(", ", ", ")")
    case i: Iterable[?]        => i.mkString("(", ", ", ")")
    case a: Array[?]           => a.mkString("(", ", ", ")")
    case n: Number             => n
    case b: java.lang.Boolean  => b
    case None                  => "None"
    case Some(s)               => String.valueOf(s)
    // This isn't a perfect choice for handling dates but seems the best compromise
    // - If the message includes no formatting then the date will appear as the number
    // + It doesn't change when serialized and de-serialized
    // + It is easily converted in Java and JS to a date
    case d: util.Date          => java.lang.Long.valueOf(d.getTime)
    case i: Instant            => java.lang.Long.valueOf(i.toEpochMilli)

    // Alternative 1 is to convert it to an ISO 8601 date stored as a String
    // - If the message includes date formatting then it will blow up
    // - Therefor the date can't be internationalized
    // + It doesn't change when serialized and de-serialized
    // + It is a better format for JSON see 'http://stackoverflow.com/questions/10286204/the-right-json-date-format'
    // + With no formatting it is still recognizable as a date
    //    case d: Date => DateTimeFormatter.ISO_INSTANT.format(d.toInstant)
    //    case i: Instant => DateTimeFormatter.ISO_INSTANT.format(i)

    // Alternative 2 is to just used Jackson's date serializer which I think we override to use ISO 8601 + milliseconds
    // - results in different behaviour based on if the object has been serialized or not (not previously an issue as it never successfully de-serialized!)
    // - If the message includes formatting it will blow up iff the message was serialized
    // - If the message doesn't include formatting it uses the locale default format and looses milliseconds and timezone
    // + It is a better format for JSON see 'http://stackoverflow.com/questions/10286204/the-right-json-date-format'
    // + With no formatting it is still recognizable as a date
    //    case d: Date => d
    //    case i: Instant => i

    // None of the options are perfect but the issue caused by using milliseconds can be fixed by altering the translated
    // messages to explicitly state that it is a date e.g. {1,date} or
    // {1,date,yyyy-MM-dd'T'HH:mm:ss.SSSX'} for an ISO8601 date in the format 2017-05-16T01:02:03.456-07 (There doesn't
    // appear to be a way to specify to convert it to UTC if it has a timezone.

    case null  => "null"
    case other => other.toString

  override def toString: String =
    s"BundleMessage(bundle = $baseBundleName, key = $key, args = $args, value = $value)"
end ResourceBundleMessage

@JsonDeserialize(`using` = classOf[BundleMessageDeserializer])
private[i18n] final case class LegacyBundleMessage(
  value: String,
  key: String = "",
  args: Vector[AnyRef] = Vector.empty
) extends BundleMessage

private[i18n] object LegacyBundleMessage:
  // Also used to create LegacyBundleMessage for testing
  def apply(value: Option[String], key: String, args: Vector[AnyRef]): BundleMessage =
    new LegacyBundleMessage(
      value.getOrElse(s"Could not extract value key='$key', params=" + args.mkString("(", ", ", ")")),
      key,
      args
    )

final class BundleMessageDeserializer extends StdDeserializer[BundleMessage](classOf[BundleMessage]):
  override def deserialize(parser: JsonParser, ctxt: DeserializationContext): BundleMessage =
    val codec          = parser.getCodec
    assert(codec != null, "No Jackson CODEC found")
    val json: JsonNode = codec.readTree(parser)
    assert(json != null, "Jackson couldn't extract the tree when parsing the JSON")

    val maybeKey    = extractStringParam(json, "key")
    val args        = extractArgValues(json.get("args"))
    val storedValue = extractStringParam(json, "value")

    val maybeMessage: Option[BundleMessage] = for
      key            <- maybeKey
      baseBundleName <- extractStringParam(json, "baseBundleName") // old versions didn't serialize the bundleName
      bundle         <- tryFindBundle(baseBundleName)              // There is no guarantee that the bundle still exists
      if bundle.containsKey(key)
    yield new ResourceBundleMessage(bundle, key, args, storedValue)
    maybeMessage.getOrElse {
      val key = maybeKey.getOrElse("Key missing")
      LegacyBundleMessage(storedValue, key, args)
    }
  end deserialize

  private def extractStringParam(json: JsonNode, name: String): Option[String] =
    Option(json.get(name)).filter(_.isValueNode).map(_.textValue())

  private def extractArgValues(values: JsonNode): Vector[AnyRef] =
    if Option(values).isEmpty then Vector.empty
    else if values.isArray then
      values
        .elements()
        .asScala
        .map((value) =>
          if value.isNumber then value.numberValue(): Number
          else if value.isBoolean then value.booleanValue(): java.lang.Boolean
          else value.asText(): java.lang.String
        )
        .toVector
    else // Just being paranoid
      Vector(values.textValue())

  /* We have to use the context loader, see `ResourceBundleLoader` */
  private def tryFindBundle(name: String): Option[ResourceBundle] =
    Try(ResourceBundle.getBundle(name, Locale.getDefault, Thread.currentThread().getContextClassLoader)).toOption
end BundleMessageDeserializer

trait BundleMessageSyntax:
  implicit final def bundleMessageOps(self: BundleMessage): BundleMessageOps = new BundleMessageOps(self)

final class BundleMessageOps(private val self: BundleMessage) extends AnyVal:

  def throw400: Nothing = throw HttpApiException.badRequest(self)
  def throw404: Nothing = throw HttpApiException.notFound(self)
  def throw409: Nothing = throw new HttpApiException(self, HttpStatus.SC_CONFLICT)
  def throw422: Nothing = throw HttpApiException.unprocessableEntity(self)
  def throw500: Nothing = throw new RuntimeException(self.value)
