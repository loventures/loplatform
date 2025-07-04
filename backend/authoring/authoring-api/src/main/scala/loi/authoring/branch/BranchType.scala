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

package loi.authoring.branch

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.scala.util.EnumDeserializer
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{ArgonautEnum, Enum, EnumEntry}

@JsonDeserialize(`using` = classOf[BranchTypeSerializer])
sealed trait BranchType extends EnumEntry with Uncapitalised

object BranchType extends Enum[BranchType] with ArgonautEnum[BranchType]:

  val values = findValues

  case object Default extends BranchType // can be edited in dcm, will show in projects app
  case object Master  extends BranchType // default initial branch, gets renamed with each new version/branch
  case object Library extends BranchType // published; currently has no significance in UI
  case object User    extends BranchType // each user's version; currently has no significance in UI

private class BranchTypeSerializer extends EnumDeserializer[BranchType](BranchType)
