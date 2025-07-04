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

package loi.cp.attachment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.learningobjects.cpxp.service.exception.{RestErrorType, RestExceptionInterface}
import org.apache.http.HttpStatus
import scalaz.NonEmptyList
import scaloi.syntax.any.*

final case class InvalidUploads(fileNames: NonEmptyList[String])
    extends RuntimeException("Invalid uploads")
    with RestExceptionInterface:
  override def getErrorType = RestErrorType.CLIENT_ERROR

  override def getHttpStatusCode: Int = HttpStatus.SC_UNPROCESSABLE_ENTITY

  override def getJson: JsonNode =
    JsonNodeFactory.instance.objectNode
      .put("message", getMessage)
      .set[ObjectNode]("fileNames", JsonNodeFactory.instance.arrayNode <| { fileNames map _.add })
end InvalidUploads
