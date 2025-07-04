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

package loi.asset.fileBundle.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.EnumDeserializer
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{ArgonautEnum, Enum, EnumEntry}

@JsonDeserialize(`using` = classOf[DisplayFileRenderChoiceDeserializer])
sealed trait DisplayFileRenderChoice extends EnumEntry with Uncapitalised

object DisplayFileRenderChoice extends Enum[DisplayFileRenderChoice] with ArgonautEnum[DisplayFileRenderChoice]:

  val values = findValues

  case object Tab   extends DisplayFileRenderChoice
  case object Embed extends DisplayFileRenderChoice

private class DisplayFileRenderChoiceDeserializer
    extends EnumDeserializer[DisplayFileRenderChoice](DisplayFileRenderChoice)
