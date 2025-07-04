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

package loi.cp.structure

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.{
  ID_FOLDER_COURSES as SectionFolder,
  ID_FOLDER_COURSE_OFFERINGS as OfferingFolder
}
import loi.cp.content.CourseContentService
import loi.cp.course.{CourseComponent, CourseFolderFacade}
import scalaz.\/
import scalaz.syntax.bifunctor.*
import scalaz.syntax.std.option.*
import scaloi.syntax.ʈry.*

import scala.compat.java8.OptionConverters.*
import scala.util.Try

@Service
trait CourseStructureService:
  def getCourseStructure(externalId: String): StructureError \/ Structure

  def getOfferingStructure(groupId: String): StructureError \/ Structure

@Service
class CourseStructureServiceImpl(
  courseContentService: CourseContentService
)(implicit
  facadeService: FacadeService
) extends CourseStructureService:
  import StructureError.*

  private final val logger = org.log4s.getLogger

  def getCourseStructure(externalId: String) =
    getStructure(findSection, externalId)

  def getOfferingStructure(groupId: String) =
    getStructure(findOffering, groupId)

  /** Get course or offering contents. */
  private def getStructure(
    finder: String => Option[CourseComponent],
    identifier: String
  ): StructureError \/ Structure =
    for
      course    <- (finder(identifier) \/> CourseNotFound(identifier)).widen
      contents  <- courseContentService.getCourseContents(course) \/> internalError(identifier)
      structure <- Try(Structure(course, contents)) \/> internalError(identifier)
    yield structure

  /** Find a section by identifier. */
  private def findSection(identifier: String): Option[CourseComponent] =
    SectionFolder.facade[CourseFolderFacade].findCourseByExternalId(identifier).asScala

  /** Find an offering by identifier. */
  private def findOffering(identifier: String): Option[CourseComponent] =
    OfferingFolder.facade[CourseFolderFacade].findCourseByGroupId(identifier).asScala

  private def internalError(identifier: String)(t: Throwable): StructureError =
    logger.warn(t)(s"Error extracting structure for course $identifier")
    InternalError(identifier)
end CourseStructureServiceImpl
