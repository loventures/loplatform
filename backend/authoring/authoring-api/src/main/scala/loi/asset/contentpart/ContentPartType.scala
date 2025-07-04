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

package loi.asset.contentpart

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.EnumDeserializer
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{ArgonautEnum, Enum, EnumEntry}

@JsonDeserialize(`using` = classOf[ContentPartTypeDeserializer])
sealed trait ContentPartType extends EnumEntry with Uncapitalised

object ContentPartType extends Enum[ContentPartType] with ArgonautEnum[ContentPartType]:

  val values = findValues

  case object Block        extends ContentPartType
  case object Embeddable   extends ContentPartType
  case object Html         extends ContentPartType
  case object Image        extends ContentPartType
  case object MediaGallery extends ContentPartType

private class ContentPartTypeDeserializer extends EnumDeserializer[ContentPartType](ContentPartType)
