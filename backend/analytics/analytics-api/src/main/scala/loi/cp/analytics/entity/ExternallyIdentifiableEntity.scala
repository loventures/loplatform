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

package loi.cp.analytics.entity

import com.fasterxml.jackson.annotation.JsonInclude
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.user.UserComponent
import argonaut.CodecJson

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class ExternallyIdentifiableEntity(
  id: Long,
  externalId: Option[String]
)

object ExternallyIdentifiableEntity:
  import scala.compat.java8.OptionConverters.*
  import scala.language.implicitConversions

  implicit def uDto2ExtId(u: UserDTO): ExternallyIdentifiableEntity =
    ExternallyIdentifiableEntity(
      id = u.id,
      externalId = u.externalId
    )

  implicit def uComp2ExtId(u: UserComponent): ExternallyIdentifiableEntity =
    ExternallyIdentifiableEntity(
      id = u.id(),
      externalId = u.getExternalId.asScala
    )
  implicit val codec: CodecJson[ExternallyIdentifiableEntity]              = CodecJson.derive[ExternallyIdentifiableEntity]
end ExternallyIdentifiableEntity
