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

package loi.cp.group

import com.learningobjects.cpxp.service.group.GroupConstants.*
import enumeratum.{Enum, EnumEntry}

/** A service object for a folder containing offerings. This allows for locking on a set of offerings and offerings.
  *
  * @param id
  *   the id of the folder
  */
case class OfferingFolder(id: Long)

object OfferingFolder:
  val legacyType = GroupType.CourseOffering
  val folderName = legacyType.getFolderId
  val typeName   = legacyType.getName
  val url        = legacyType.getName

/** A service object for a folder containing sections. This allows for locking on a set of sections.
  *
  * @param id
  *   the id of the folder
  * @param sectionType
  *   the type of sections this folder serves
  */
case class SectionFolder(id: Long, sectionType: SectionType)

// This hides legacy choices in groupType
/** An enumeration of all the types of sections in our system.
  *
  * @param legacyGroupType
  *   a reference to the legacy type
  */
sealed abstract class SectionType(val legacyGroupType: GroupType) extends EnumEntry:

  /** the unique name of the folder of this type of sections */
  val folderName: String = legacyGroupType.getFolderId

  /** the system name for the section type */
  val name: String = legacyGroupType.getName

  /** the base url of the folder */
  val url: String = legacyGroupType.getName
end SectionType

case object SectionType extends Enum[SectionType]:
  val values = findValues

  /** the sections type, of which students do their work in */
  case object Sections extends SectionType(GroupType.CourseSection)

  /** the test sections type, of which content designers test their work in */
  case object TestSections extends SectionType(GroupType.TestSection)

  /** the preview sections type, of which authors test their work in */
  case object PreviewSections extends SectionType(GroupType.PreviewSection)
end SectionType
