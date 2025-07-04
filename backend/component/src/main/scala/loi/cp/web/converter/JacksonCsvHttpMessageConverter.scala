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

package loi.cp.web.converter

import java.io.ByteArrayOutputStream
import java.lang.reflect.Type
import java.util

import com.fasterxml.jackson.databind.node.{ArrayNode, NullNode, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.tototoshi.csv.CSVWriter
import com.learningobjects.cpxp.component.AbstractComponent
import com.learningobjects.cpxp.component.annotation.{Component, RequestBody}
import com.learningobjects.cpxp.component.web.WebRequest
import com.learningobjects.cpxp.component.web.converter.{ConvertOptions, HttpMessageConverter}
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotWriteableException
import com.learningobjects.cpxp.util.{HttpUtils, ManagedUtils, StringUtils}
import com.learningobjects.de.web.MediaType
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.web.mediatype.CollectionEntity
import scala.util.Using
import scalaz.std.list.*
import scalaz.std.set.*
import scalaz.syntax.traverse.*
import scaloi.syntax.AnyOps.*

import scala.jdk.CollectionConverters.*

/** HTTP message converter that can transform a collection of preferably-homogeneous elements into a CSV. Elements are
  * transformed to JSON using our standard Jackson machinery.
  */
@Component
class JacksonCsvHttpMessageConverter(mapper: ObjectMapper, io: JacksonHttpMessageIO)
    extends AbstractComponent
    with HttpMessageConverter[CollectionEntity]:

  override def getSupportedMediaTypes: util.List[MediaType] = JacksonCsvHttpMessageConverter.SupportedMediaTypes

  private def supports(mediaType: MediaType): Boolean = MediaType.TEXT_CSV.includes(mediaType)

  override def canRead(tpe: Type, mediaType: MediaType): Boolean = false

  override def read(requestBody: RequestBody, request: WebRequest, targetType: Type): CollectionEntity =
    throw new UnsupportedOperationException

  override def canWrite(value: AnyRef, mediaType: MediaType): Boolean =
    supports(mediaType) && value.isInstanceOf[CollectionEntity]

  override def write(
    source: CollectionEntity,
    options: ConvertOptions,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): Unit =
    response.setContentType(MediaType.TEXT_CSV_UTF_8_VALUE)
    val fn       = request.getPathInfo.replaceAll("[;\\.].*", "").replaceAll(".*/", "").capitalize.concat(".csv")
    response.setHeader(HttpUtils.HTTP_HEADER_CONTENT_DISPOSITION, HttpUtils.getDisposition("download", fn))
    val entities = source.getEntities.asScala.toSeq
    if entities.nonEmpty then
      try
        val elements = jsonify(entities)
        val columns  = elements.foldMap(columnify).toList.sorted
        val csv      = csvify(elements, columns)
        ManagedUtils.end()
        io.write(csv.toByteArray, request, response)
      catch
        case ex: Exception =>
          throw new HttpMessageNotWriteableException("Could not write CSV: " + ex.getMessage, ex)
    end if
  end write

  /** Serialize a collection. */
  private def jsonify(entities: Seq[Any]): List[ObjectNode] =
    io.valueToTree(entities)
      .asInstanceOf[ArrayNode]
      .elements
      .asScala
      .map(_.asInstanceOf[ObjectNode])
      .toList

  /** Get the column names from an element. */
  private def columnify(element: ObjectNode): Set[String] =
    element.fieldNames.asScala.filterNot(_ == "_type").toSet

  /** Serialize a collection as CSV. */
  private def csvify(elements: Seq[ObjectNode], columns: Seq[String]): ByteArrayOutputStream =
    new ByteArrayOutputStream() <| { bos =>
      Using.resource(CSVWriter.open(bos)) { csv =>
        csv.writeRow(columns.map(_.capitalize).map(StringUtils.toSeparateWords))
        elements foreach { element =>
          csv.writeRow(columns.map(column => stringify(element.get(column))))
        }
      }
    }

  /** Stringify an object field to a non-escaped CSV string */
  private def stringify(field: JsonNode): String = field match
    case null | _: NullNode => ""
    case text: TextNode     => text.textValue
    case array: ArrayNode   =>
      array.elements.asScala
        .map({
          case text: TextNode => text.textValue
          case value          => value.toString
        })
        .mkString(",")
    case obj: ObjectNode    => mapper.writeValueAsString(obj)
    case value              => value.toString
end JacksonCsvHttpMessageConverter

object JacksonCsvHttpMessageConverter:
  private val SupportedMediaTypes = util.Collections.singletonList(MediaType.TEXT_CSV)
