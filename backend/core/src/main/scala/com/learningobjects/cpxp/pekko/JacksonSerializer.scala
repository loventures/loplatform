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

package com.learningobjects.cpxp.pekko

import org.apache.pekko.serialization.Serializer
import com.learningobjects.cpxp.component.web.util.JacksonUtils

class JacksonSerializer extends Serializer:
  import JacksonUtils.getMapper as mapper

  override def identifier: Int = 123321 // Magic

  override def includeManifest: Boolean = true

  override def fromBinary(
    bytes: Array[Byte],
    manifest: Option[Class[?]],
  ): AnyRef =
    manifest.map(mapper.readValue(bytes, _)).orNull.asInstanceOf[AnyRef]

  override def toBinary(o: AnyRef): Array[Byte] =
    mapper.writeValueAsBytes(o)
end JacksonSerializer
