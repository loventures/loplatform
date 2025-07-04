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

package loi.cp.imports
package importers

import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFinder}
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.de.enrollment.EnrollmentOwner
import com.learningobjects.de.group.GroupComponent
import loi.cp.course.CourseComponent
import loi.cp.imports.errors.{FieldViolation, PersistError, Violation}
import loi.cp.role.{RoleComponent, RoleService}
import scalaz.*
import scalaz.syntax.bind.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*
import scaloi.syntax.BooleanOps.*

import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/** Contains common functionality for importers dealing with enrollments.
  */
// I am now ugly and somewhat strange, but that's because I'm trying to not
// acknowledge that Program <:< Group (it's not) but the database insists on reminding me
trait ImporterWithEnrollment extends ImporterWithIntegration with DeserializingCsvImporter:

  val queryService: QueryService

  def getCourse(
    courseId: Option[String],
    courseExternalId: Option[String],
    courseIntg: Option[IntegrationImportItem]
  ): PersistError \/ CourseComponent =
    import GroupFinder.*

    ((courseId, courseExternalId, courseIntg) match
      case (Some(s), None, None) =>
        filterByData[GroupComponent](DATA_TYPE_GROUP_ID, s)
      case (None, Some(e), None) =>
        filterByData[GroupComponent](DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER, e)
      case (None, None, Some(c)) =>
        findGroupByConnector[GroupComponent](c)
      case _                     => // this should be caught way earlier
        PersistError("No group identifier provided").left[GroupComponent]
    ).>>!(g => (g.getGroupType == GroupType.CourseSection) \/> typeError("course", g))
      .map(_.component[CourseComponent])
  end getCourse

  def findGroupByConnector[T <: ComponentInterface: ClassTag](c: IntegrationImportItem): PersistError \/ T =
    findComponentByConnector[T](c, GroupConstants.ITEM_TYPE_GROUP)

  private def filterByData[T <: ComponentInterface: ClassTag](data: String, value: String): PersistError \/ T =
    queryService
      .queryRoot(GroupFinder.ITEM_TYPE_GROUP)
      .addCondition(data, Comparison.eq, value)
      .setLimit(1)
      .getItems()
      .asScala
      .headOption
      .flatMap(_.tryComponent[T])
      .toRightDisjunction(PersistError(s"Could not find '$value'"))

  private def typeError(tpe: String, g: EnrollmentOwner) =
    PersistError(s"Expected $g to be a $tpe, but it was a ${g.getComponentInstance.getIdentifier}")

  def getRole(roleId: String)(implicit roleService: RoleService): PersistError \/ RoleComponent =
    Option(roleService.getRoleByRoleId(roleId)) match
      case Some(r) => r.right
      case None    =>
        PersistError(s"Role with id: $roleId doesn't exist").left

  def getStatus(columns: CsvRow, statusField: String) =
    columns
      .getOptional(statusField)
      .flatten
      .map({ status =>
        deserializeJson[EnrollmentImportItemStatus](s""""$status"""") {
          FieldViolation(statusField, status, s"$statusField was not formatted appropriately")
        }
      })
      .getOrElse(EnrollmentImportItemStatus.enabled.successNel[Violation])
end ImporterWithEnrollment
