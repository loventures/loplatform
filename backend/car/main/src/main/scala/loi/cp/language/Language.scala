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

package loi.cp.language

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, SerializationFeature}
import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.exception.HttpMediaTypeNotAcceptableException
import com.learningobjects.cpxp.component.web.{FileResponse, WebRequest, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.language.LanguageService
import com.learningobjects.cpxp.util.{ClassUtils, FileUtils, InternationalizationUtils}
import com.learningobjects.cpxp.web.ExportFile
import org.apache.commons.io.FilenameUtils
import scaloi.syntax.AnyOps.*
import scaloi.syntax.OptionOps.*

import java.io.File
import java.util.Map.Entry as JEntry
import java.util.{Locale, Properties}
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NonFatal

@Component
class Language(val componentInstance: ComponentInstance, self: LanguageFacade)(
  ls: LanguageService,
)(implicit
  mapper: ObjectMapper,
) extends LanguageComponent
    with ComponentImplementation:
  import Language.*

  override def getId = self.getId

  override def delete() =
    self.delete()
    ls.invalidateDomainMessages()

  @PostCreate
  private def create(language: LanguageComponent): Unit =
    update(language)
    ()

  override def update(l: LanguageComponent): LanguageComponent =
    self.setName(l.getName)
    val locale =
      Locale.forLanguageTag(l.getLanguage + l.getCountry.fold("")(c => s"-$c"))
    if locale.getLanguage != l.getLanguage then
      throw new ValidationException("language", l.getLanguage, "Invalid language")
    l.getCountry.filter(_ != locale.getCountry) foreach { country =>
      throw new ValidationException("country", country, "Invalid country")
    }
    self.setLocale(locale.toString)
    self.setDisabled(l.isDisabled)
    if self.getMessages == null then self.setMessages(Map[String, String]())
    ls.invalidateDomainMessages()
    this
  end update

  override def getName: String = self.getName

  override def getLanguage: String = getLocale.getLanguage

  override def getCountry: Option[String] =
    Option(getLocale.getCountry).filterNot(_.isEmpty)

  // threadlocal sidechannel badness, could pull locale from web request by making this an embed
  override def getLanguageName: String =
    getLocale.getDisplayName(InternationalizationUtils.getLocale)

  override def getLanguageCode: String =
    getLocale.toLanguageTag

  override def isDisabled: Boolean = self.getDisabled.isTrue

  override def getMessages: Map[String, String] = self.getMessages

  override def setMessages(messages: Map[String, String]): Unit =
    self.setMessages(messages)
    ls.invalidateDomainMessages()

  override def upload(upload: UploadInfo): Unit =
    self.setMessages(parseMessages(upload))
    ls.invalidateDomainMessages()

  override def download(request: WebRequest): WebResponse =
    getResponseType(request).fold(throw new HttpMediaTypeNotAcceptableException()) { mediaType =>
      val fileName   = FileUtils.cleanFilename(getName) + "_" + getLanguage + getCountry
        .fold("")(s => "-" + s) + "." + mediaType.subtype
      val exportFile = ExportFile.create(fileName, mediaType, request)
      saveMessages(self.getMessages, exportFile.file, mediaType)
      FileResponse(exportFile.toFileInfo)
    }

  private lazy val getLocale: Locale =
    ClassUtils.parseLocale(self.getLocale)
end Language

object Language:
  private final val ApplicationProperties =
    MediaType.create("application", "properties")

  private def getResponseType(request: WebRequest) =
    if request.acceptsMediaType(MediaType.CSV_UTF_8.toString) then Some(MediaType.CSV_UTF_8)
    else if request.acceptsMediaType(MediaType.JSON_UTF_8.toString) then Some(MediaType.JSON_UTF_8)
    else if request.acceptsMediaType(ApplicationProperties.toString) then Some(ApplicationProperties)
    else None

  private def parseMessages(upload: UploadInfo)(implicit mapper: ObjectMapper): Map[String, String] =
    try
      FilenameUtils.getExtension(upload.getFileName).toLowerCase match
        case "csv"        =>
          expandPrefix(Using.resource(CSVReader open upload.getFile)(parseCsv))
        case "properties" =>
          expandPrefix(FileUtils.loadProperties(upload.getFile).asScala.toMap)
        case "json"       =>
          parseJson(mapper.readTree(upload.getFile))
        case f            =>
          throw new IllegalArgumentException(
            s"Unsupported format: $f"
          ) // not acceptable? .. this gets turned into a validation exception
    catch
      case NonFatal(e) =>
        throw new ValidationException("upload", upload.getFileName, Option(e.getMessage).getOrElse("An error occurred"))

  private def saveMessages(
    messages: Map[String, String],
    file: File,
    mediaType: MediaType,
  )(implicit mapper: ObjectMapper) = mediaType match
    case MediaType.JSON_UTF_8  =>
      mapper
        .writer(SerializationFeature.INDENT_OUTPUT)
        .writeValue(file, encodeJson(messages))
    case ApplicationProperties =>
      val prefix     = commonPrefix(messages)
      val prefixMap  = prefix.fold(messages)(prefix => removePrefix(messages, prefix) + (PrefixKey -> prefix))
      val properties = new Properties() // +1 to sort this but ugh
      prefixMap foreach { case (k, v) =>
        properties.setProperty(k, v)
      }
      FileUtils.saveProperties(file, properties)
    case MediaType.CSV_UTF_8   =>
      Using.resource(CSVWriter open file) { csv =>
        val prefix = commonPrefix(messages)
        prefix foreach { p =>
          csv.writeRow(List(PrefixKey, p))
        }
        prefix
          .fold(messages)(removePrefix(messages, _))
          .toList
          .sortBy(_._1.toLowerCase) foreach { case (k, v) =>
          csv.writeRow(List(k, v))
        }
      }

  private def PrefixKey = "Prefix"

  private[language] def expandPrefix(messages: Map[String, String]) =
    messages.get(PrefixKey).fold(messages) { prefix =>
      (messages - PrefixKey) map { case (key, value) =>
        prefix.concat(key) -> value
      }
    }

  /** Returns a map with the specified common prefix removed from all keys, and all non-matching keys discarded. */
  private[language] def removePrefix(messages: Map[String, String], prefix: String): Map[String, String] =
    messages.collect {
      case (key, value) if key.startsWith(prefix) =>
        key.substring(prefix.length) -> value
    }

  private val PrefixSlash = "[^/]+/".r

  /** Returns the common prefix ending in slash (/), if any, of the specified map, as long as the map does not contain a
    * $prefix/Prefix key.
    */
  private[language] def commonPrefix(messages: Map[String, String]): Option[String] =
    messages.keys.headOption
      .flatMap(PrefixSlash.findPrefixOf(_))
      .filterNot(prefix => messages.contains(prefix.concat(PrefixKey)))
      .filter(prefix => messages.keys.forall(_.startsWith(prefix)))

  /** Parses a CSV into a message map, ignoring blank lines. */
  private[language] def parseCsv(csv: CSVReader): Map[String, String] =
    val tuples = csv.all() collect {
      case key :: value :: _ if !key.isEmpty => key -> value
    }
    tuples.toMap

  /** Parses a json tree into a message map. { "a": "b", "c": { "d": "e" } } becomes [ a -> b, c/d -> e ] */
  private[language] def parseJson(json: JsonNode): Map[String, String] =
    def json2tuples(
      accumulator: Seq[(String, String)],
      iterator: Iterator[JEntry[String, JsonNode]],
      prefix: String
    ): Seq[(String, String)] =
      if iterator.isEmpty then accumulator
      else
        val entry       = iterator.next()
        val accumulated =
          if entry.getValue.isTextual then (prefix.concat(entry.getKey) -> entry.getValue.textValue) +: accumulator
          else if entry.getValue.isObject then
            json2tuples(
              accumulator,
              entry.getValue.properties.asScala.iterator,
              prefix.concat(entry.getKey).concat("/")
            )
          else accumulator
        json2tuples(accumulated, iterator, prefix)
    json2tuples(List.empty, json.properties.asScala.iterator, "").toMap
  end parseJson

  /** Encodes a message map into a json tree, nesting prefixed keys. [ a -> b, c/d -> e ] becomes { "a": "b", "c": {
    * "d": "e" } }
    */
  private[language] def encodeJson(
    map: Map[String, String],
  )(implicit mapper: ObjectMapper): ObjectNode =
    mapper.createObjectNode <| { node =>
      map.toList.sortBy(_._1.toLowerCase) foreach { case (key, value) =>
        @tailrec
        def insert(segments: List[String], n: ObjectNode): Unit =
          segments match
            case Nil            =>
            case suffix :: Nil  => n.put(suffix, value); ()
            case prefix :: tail => insert(tail, n.withObject("/" + prefix))
        insert(key.split('/').toList, node)
      }
    }
end Language
