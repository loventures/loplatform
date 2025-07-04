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

import java.time.Instant

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.json.OptionalField
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.EntityContext
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.config.ConfigurationService
import loi.cp.course.*
import loi.cp.course.lightweight.{LightweightCourse, LightweightCourseService}
import loi.cp.imports.errors.{FieldViolation, PersistError, ValidationError, Violation}
import loi.cp.integration.IntegrationComponent
import loi.cp.subtenant.SubtenantService
import org.log4s.Logger
import scalaz.Isomorphism.<~>
import scalaz.{Isomorphism, \/}
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.syntax.validation.*
import scaloi.GetOrCreate
import scaloi.misc.JavaOptionalInstances.*
import scaloi.misc.TimeSource
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import scala.jdk.CollectionConverters.*

@Component(name = "$$name=Courses")
@ImportBinding(
  value = classOf[CourseImportItem],
  label = "loi.cp.imports.CourseSectionImportItem.label"
)
class CourseImporter(val componentInstance: ComponentInstance)(implicit
  coursewareAnalyticsService: CoursewareAnalyticsService,
  ec: EntityContext,
  fs: FacadeService,
  cs: ConfigurationService,
  val integrationWebService: IntegrationWebService,
  lwcs: LightweightCourseService,
  ts: TimeSource,
  val subtenantService: SubtenantService,
  val componentService: ComponentService
) extends ValidatingImporter[CourseImportItem]
    with DeserializingCsvImporter
    with ImporterWithIntegration
    with ImporterWithSubtenant
    with ComponentImplementation:
  import Importer.*

  override val log: Logger = org.log4s.getLogger

  override def requiredHeaders: Set[String] = Set("courseId", "name")

  override def allHeaders: Set[String] = Set(
    "name",
    "externalId",
    "endDate",
    "courseId",
    "integrationConnectorId",
    "integrationUniqueId",
    "termUniqueId",
    "masterCourseId",
    "offeringId",
    "disabled",
    "startDate",
    "subtenant"
  )

  override def validateItem(item: CourseImportItem): ViolationNel[CourseImportItem] =
    (validateOffering(item.offeringId)
      *> validateNonEmpty(item, "courseId", _.courseId)
      *> validateNonEmpty(item, "name", _.name)
      *> item.subtenant.traverse(validateSubtenant)).map(_ => item)

  override def deserializeCsvRow(headers: Seq[String], values: Seq[String]): ValidationError \/ CourseImportItem =
    ifHeadersMatchValues(headers, values) { columns =>
      val courseId                        = columns.failIfNone("courseId")
      val name                            = columns.failIfNone("name")
      val externalId                      = columns.getOptionalField("externalId")
      val offeringId                      = columns.getOptionalField("offeringId")
      val termUniqueId                    = columns.getOptionalField("termUniqueId")
      val disabled: ViolationNel[Boolean] = columns
        .failIfNone("disabled")
        .map({ disabled =>
          deserializeJson[Boolean](disabled) {
            FieldViolation("disabled", disabled, "disabled was not formatted appropriately")
          }
        })
        .getOrElse(false.successNel[Violation])

      // either both integration values must be included, or none should be included
      val integrationUniqueId    = columns.getOptional("integrationUniqueId")
      val integrationConnectorId =
        columns.getOptional("integrationConnectorId")

      val startDate = columns
        .getOptionalField("startDate")
        .map(parseInstant("startDate"))
        .toValidationNel[Violation, Instant]
      val endDate   = columns
        .getOptionalField("endDate")
        .map(parseInstant("endDate"))
        .toValidationNel[Violation, Instant]

      val offering = validateOffering(offeringId)

      val integration: ViolationNel[Option[IntegrationImportItem]] =
        getIntegrationFromColumns(integrationUniqueId.flatten, integrationConnectorId.flatten)

      val subtenant = columns.getOptionalField("subtenant").traverse(validateSubtenant)

      val validated =
        (courseId |@| name |@| disabled |@| integration |@| startDate |@| endDate |@| offering |@| subtenant) {
          (courseId, name, disabled, integration, startDate, endDate, offering, subtenant) =>
            CourseImportItem(
              externalId = externalId,
              courseId = courseId,
              name = name,
              offeringId = OptionalField(Some(offering)),
              termUniqueId = termUniqueId,
              startDate = startDate,
              endDate = endDate,
              disabled = Some(disabled),
              integration = integration,
              subtenant = subtenant,
            )
        }

      validated
        .leftMap(violations => ValidationError(violations))
        .toDisjunction
    }

  private def validateOffering(offeringId: OptionalField[String]): ViolationNel[String] =
    offeringId.toOption match
      case None     =>
        FieldViolation("offeringId", null, "offeringId must be specified").failureNel
      case Some(id) => id.successNel

  implicit def naturalRefl[F[_]]: F <~> F = Isomorphism.naturalRefl // wtf, why no longer implicit in scalaz?

  override def execute(invoker: UserDTO, validated: Validated[CourseImportItem]): PersistError \/ ImportSuccess =
    val course = validated.item
    // If either the integration connector id or the library id is bad, we want to bail out
    // If either are empty, then it's okay that we don't have them
    // otherwise, go through with the course creation.
    for
      _         <- getSystem(course.integration)
      offeringId = course.offeringId.get
      offering  <- getOffering(offeringId)
      gocCourse <- getOrCreateCourse(course, offering, invoker)
      newCourse <- gocCourse.result.right
      _         <- lightweightValidate(offering)(newCourse)
    yield
      newCourse.setName(course.name)
      newCourse.setDisabled(course.disabled.isTrue)

      course.startDate.coapplyTo(newCourse.setStartDate)
      course.endDate.coapplyTo(newCourse.setEndDate)
      course.externalId.coapplyTo(newCourse.setExternalId)

      // only create the integration if both of these values are present
      for
        integration <- course.integration
        system      <- getSystemForConnectorId(integration.connectorId)
      yield
        // create one if not existing, otherwise just update
        getIntegrationForCourseAndSystem(newCourse, system.getId) match
          case Some(oldIntegration) =>
            oldIntegration.setUniqueId(integration.uniqueId)
          case None                 =>
            val init = new IntegrationComponent.Init()
            init.systemId = system.getId
            init.uniqueId = integration.uniqueId
            newCourse.getIntegrationRoot.addIntegration(init)
      end for

      LightweightCourse.unapply(newCourse) foreach { lwc =>
        if gocCourse.isCreated then lwcs.initializeSection(lwc, Some(offering))
        else // TODO: ought to only do this if there's a meaningful (date) change
          lwcs.updateSection(lwc)
          coursewareAnalyticsService.emitSectionUpdateEvent(lwc)
      }

      importSubtenant(course.subtenant)(sOpt => newCourse.setSubtenant(sOpt.orNull))

      ImportSuccess(Some(course))
    end for
  end execute

  def getOffering(offeringId: String): PersistError \/ LightweightCourse =
    for
      off <- offeringsFolder.findCourseByGroupId(offeringId) \/> PersistError(
               s"Offering with id: $offeringId not found"
             )
      _   <- (off.getDisabled || off.isArchived) \/>! PersistError(s"Offering with id: $offeringId is suspended")
    yield off.asInstanceOf[LightweightCourse]

  import GroupConstants.*
  def groupFolder     = ID_FOLDER_COURSES.facade[CourseFolderFacade]
  def libraryFolder   = ID_FOLDER_LIBRARIES.facade[CourseFolderFacade]
  def offeringsFolder = ID_FOLDER_COURSE_OFFERINGS.facade[CourseFolderFacade]

  def getOrCreateCourse(
    course: CourseImportItem,
    offering: LightweightCourse,
    user: UserDTO
  ): PersistError \/ GetOrCreate[CourseComponent] =
    val (impl, init) = lightweightInit(course, offering, user)
    groupFolder
      .getOrCreateCourse(course.courseId, impl, init)
      .right

  private def lightweightInit(
    course: CourseImportItem,
    offering: LightweightCourse,
    user: UserDTO
  ): (String, CourseComponent.Init) =
    LightweightCourse.Identifier -> new CourseComponent.Init(
      course.name,
      course.courseId,
      GroupType.CourseSection,
      user,
      offering.left
    )

  private def getIntegrationForCourseAndSystem(
    course: CourseComponent,
    systemId: Long
  ): Option[IntegrationComponent] =
    course.getIntegrationRoot
      .getIntegrations(ApiQuery.ALL)
      .asScala
      .find(_.getSystemId == systemId)

  private def lightweightValidate(offering: LightweightCourse)(course: CourseComponent): PersistError \/ Unit =
    for
      cic           <- course.component_?[LightweightCourse] \/> PersistError(s"Course ${course.getGroupId} is not lightweight")
      cicBranch      = cic.loadBranch()
      offeringBranch = offering.loadBranch()
      _             <- (cicBranch.id == offeringBranch.id) \/> PersistError(
                         s"Course ${course.getGroupId} does not match offering ${offering.getGroupId} branch"
                       )
      _             <- (cic.loadCourse().info.id == offering.loadCourse().info.id) \/> PersistError(
                         s"Course ${course.getGroupId} does not match offering ${offering.getGroupId} asset"
                       )
    yield ()
end CourseImporter
