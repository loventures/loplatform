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

package loi.cp.lwgrade
package api

import java.time.Instant
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.user.{UserDTO, UserId as Uzr}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.course.right.*
import loi.cp.reference.EdgePath
import enumeratum.*
import com.learningobjects.cpxp.scala.json.JacksonCodecs.codecFromJackson
import loi.cp.lti.CourseColumnIntegrations
import loi.cp.lti.LtiItemSyncStatus.LtiItemSyncStatusHistory
import loi.cp.lti.ags.AgsLineItem
import scalaz.\/
import scaloi.json.ArgoExtras

import scala.util.Try

/** This endpoint mimics the the legacy gradebook API. A temporary stop gap while front-end changes are in flux. Many
  * endpoints are going to use ApiQuery. However for the most part, I'm going to ignore it.
  * @see
  *   GradebookApiComponent
  */
@Controller(value = "lightweightgradelegacy", root = true)
@RequestMapping(path = "lwgrade2")
trait LegacyGradebookWebController extends ApiRootComponent:
  import LegacyGradebookWebController.*
  @RequestMapping(path = "{course}/gradebook/grades", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[CourseAdminRight]))
  def getGrades(
    @SecuredAdvice @PathVariable("course") course: Long,
    q: ApiQuery,
    @QueryParam(value = "syncHistory", decodeAs = classOf[Boolean]) history: Option[Boolean],
  ): ArgoBody[ApiQueryResults[GradeComponentDto]]

  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/{edgePath}/{userId}", method = Method.GET)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def getGradeHistory(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: String,
    @PathVariable("userId") userId: Long,
  ): ErrorResponse \/ ArgoBody[SingleUserGradeHistoryDto]

  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/{edgePath}/{userId}", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def syncExternalGrade(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: String,
    @PathVariable("userId") userId: Long,
  ): ErrorResponse \/ ArgoBody[SingleUserGradeHistoryDto]

  @RequestMapping(path = "{course}/gradebook/grades/syncHistory", method = Method.GET)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def getLastManualSync(
    @SecuredAdvice @PathVariable("course") course: Long
  ): ErrorResponse \/ ArgoBody[LastManualSyncDto]

  // full gradebook Force Sync button
  @RequestMapping(path = "{course}/gradebook/grades/syncHistory", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def syncEntireCourse(
    @SecuredAdvice @PathVariable("course") course: Long,
  ): ErrorResponse \/ ArgoBody[LastManualSyncDto]

  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/{edgePath}", method = Method.GET)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def getColumnHistory(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: String,
  ): ErrorResponse \/ ArgoBody[SingleColumnSyncHistoryDto]

  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/{edgePath}", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def syncExternalColumn(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: String,
  ): ErrorResponse \/ ArgoBody[SingleColumnSyncHistoryDto]

  // single column Sync All Grades button
  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/{edgePath}/all", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def syncAllGradesForColumn(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: String,
  ): ErrorResponse \/ ArgoBody[Map[Uzr, SingleGradeHistory]]

  // *not* the full gradebook Force Sync button, this syncs LTI column integrations
  @RequestMapping(path = "{course}/gradebook/grades/syncHistory/all", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def syncAllColumnsForCourse(
    @SecuredAdvice @PathVariable("course") course: Long
  ): ErrorResponse \/ Unit

  @RequestMapping(path = "{course}/gradebook/columns", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[LearnCourseRight], classOf[CourseAdminRight]))
  def getColumns(
    @SecuredAdvice @PathVariable("course") course: Long,
  ): Try[ArgoBody[ApiQueryResults[GradebookColumnComponentDto]]]

  @RequestMapping(path = "{course}/gradebook/columns/{edgePath}", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[LearnCourseRight], classOf[CourseAdminRight]))
  def getColumn(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("edgePath") edgePath: EdgePath,
  ): ErrorResponse \/ GradebookColumnComponentDto

  @RequestMapping(path = "{course}/gradebook/categories", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[LearnCourseRight], classOf[CourseAdminRight]))
  def getCategories(
    @SecuredAdvice @PathVariable("course") course: Long,
  ): ArgoBody[ApiQueryResults[GradebookCategoryComponentDto]]

  /* Not Actually supported, but implemented with explicit error/dummy values to get the frontend to not crash. */
  @RequestMapping(path = "{course}/gradebook/settings", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[CourseAdminRight]))
  def getSettings(
    @SecuredAdvice @PathVariable("course") course: Long,
  ): ArgoBody[GradebookSettingsDto]

  @RequestMapping(path = "{course}/gradebook/export", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[CourseAdminRight]))
  def doExport(
    @SecuredAdvice @PathVariable("course") course: Long,
    @QueryParam(required = false) config: ArgoBody[GradebookExportConfig]
  ): TextResponse

  @RequestMapping(path = "{course}/gradebook/{student}/export", method = Method.GET)
  @Secured(Array(classOf[ViewCourseGradeRight], classOf[LearnCourseRight], classOf[CourseAdminRight]))
  def exportStudentGradebook(
    @SecuredAdvice @PathVariable("course") course: Long,
    @PathVariable("student") student: Long
  ): ErrorResponse \/ TextResponse
end LegacyGradebookWebController
object LegacyGradebookWebController:
  import argonaut.*
  import Argonaut.*

  // this import is used for deriving the codecs, despite what IntelliJ says
  import scaloi.json.ArgoExtras.*

  final case class GradebookColumnComponentDto(
    id: EdgePath,
    name: String,
    credit: String,
    weight: Double,
    hideGradeFromStudents: Boolean,
    `type`: String,
    dueDate: Option[Instant],
    displayOrder: Long,
    externalId: String,
    externalColumnConfiguration: Unit,
    `category_id`: EdgePath,
    maximumPoints: Double,
    Category: GradebookCategoryComponentDto,
    contentItemId: Long,
    contentItem: Unit,
    gradeTransformationStrategy: String,
    syncHistory: Option[ColumnSyncHistoryDto],
  )
  object GradebookColumnComponentDto:
    val empty                                                  = GradebookColumnComponentDto(
      id = EdgePath.Root,
      name = "",
      credit = "",
      weight = 0.0,
      hideGradeFromStudents = false,
      `type` = "",
      dueDate = None,
      displayOrder = 0L,
      externalId = "",
      externalColumnConfiguration = (),
      `category_id` = EdgePath.Root,
      maximumPoints = 0.0,
      Category = GradebookCategoryComponentDto.empty,
      contentItemId = 0L,
      contentItem = (),
      gradeTransformationStrategy = "loi.cp.gradebook.gradingstrategy.IdentityGradeTransformationStrategy",
      syncHistory = None,
    )
    implicit val codec: CodecJson[GradebookColumnComponentDto] =
      CodecJson.derive[GradebookColumnComponentDto]
  end GradebookColumnComponentDto

  final case class ColumnSyncHistoryDto(
    ags: Option[LtiItemSyncStatusHistory[AgsLineItem]],
  )

  object ColumnSyncHistoryDto:
    def from(histories: CourseColumnIntegrations, ep: EdgePath): ColumnSyncHistoryDto =
      ColumnSyncHistoryDto(
        ags = histories.lineItems.get(ep),
      )
    implicit val codec: CodecJson[ColumnSyncHistoryDto]                               =
      CodecJson.derive[ColumnSyncHistoryDto]

  case class LastManualSyncDto(lastManualSync: Option[Instant])
  object LastManualSyncDto:
    implicit val codec: CodecJson[LastManualSyncDto] =
      CodecJson.derive[LastManualSyncDto]

  case class SingleColumnSyncHistoryDto(
    column: GradeColumn,
    history: ColumnSyncHistoryDto
  )
  object SingleColumnSyncHistoryDto:
    implicit val codec: CodecJson[SingleColumnSyncHistoryDto] =
      CodecJson.derive[SingleColumnSyncHistoryDto]

  final case class GradebookCategoryComponentDto(
    id: EdgePath,
    name: String,
    weight: Double,
    displayOrder: Long,
    isUncategorized: Boolean,
    dropLowest: Boolean,
    gradeDisplay: String,
    gradeAggregationStrategy: String,
  )
  object GradebookCategoryComponentDto:
    val empty                                                    = GradebookCategoryComponentDto(
      id = EdgePath.Root,
      name = "",
      weight = 0,
      displayOrder = 0,
      isUncategorized = false,
      dropLowest = false,
      gradeDisplay = "",
      gradeAggregationStrategy = ""
    )
    implicit val codec: CodecJson[GradebookCategoryComponentDto] =
      CodecJson.derive[GradebookCategoryComponentDto]
  end GradebookCategoryComponentDto

  final case class GradebookSettingsDto(
    hideGradeColors: Boolean,
    showGradeHistory: Boolean,
    hideFullyGradedAssignments: Boolean,
    showAccurateGrades: Boolean,
    showDroppedStudents: Boolean,
    gradeDisplayDefault: String,
    gradingScales: List[GradingScaleDto],
  )
  object GradebookSettingsDto:
    val empty                                           = GradebookSettingsDto(
      hideGradeColors = false,
      showGradeHistory = false,
      hideFullyGradedAssignments = false,
      showAccurateGrades = false,
      showDroppedStudents = false,
      gradeDisplayDefault = "points",
      gradingScales = List.empty
    )
    implicit val codec: CodecJson[GradebookSettingsDto] =
      CodecJson.derive[GradebookSettingsDto]
  end GradebookSettingsDto

  final case class GradingScaleDto(percentage: Long, color: String, mark: String)
  object GradingScaleDto:
    val empty                                      = GradingScaleDto(
      percentage = 0L,
      color = "",
      mark = ""
    )
    implicit val codec: CodecJson[GradingScaleDto] = CodecJson.derive[GradingScaleDto]

  final case class GradebookExportConfig(
    assignmentFilter: Option[List[Filter[ContentAttribute]]],
    userFilter: Option[List[Filter[UserAttribute]]],
    userOrder: Option[List[Order[UserAttribute]]],
    userAttributes: Option[List[UserAttribute]],
    gradeOrder: Option[List[Order[ContentAttribute]]],
    userIds: Option[List[Long]],
    categoryAverage: Option[Boolean],
    userColumnHeader: Option[Boolean], // Ignored, not sure what this is supposed to do.
    categoryPoints: Option[Boolean],
    assignmentPoints: Option[Boolean]
  )
  object GradebookExportConfig:
    val empty                                                                 = GradebookExportConfig(
      assignmentFilter = None,
      userFilter = None,
      userOrder = None,
      userAttributes = None,
      gradeOrder = None,
      userIds = None,
      userColumnHeader = None,
      categoryAverage = None,
      categoryPoints = None,
      assignmentPoints = None
    )
    implicit val gradebookExportConfigCodec: CodecJson[GradebookExportConfig] = CodecJson.derive[GradebookExportConfig]
  end GradebookExportConfig

  final case class Filter[P](property: P, operator: Option[PredicateOperator], value: Option[String])
  object Filter:
    implicit def filterCodec[P: CodecJson]: CodecJson[Filter[P]] = CodecJson.derive[Filter[P]]
  final case class Order[P](property: P, direction: OrderDirection)
  object Order:
    implicit def orderCodec[P: CodecJson]: CodecJson[Order[P]] = CodecJson.derive[Order[P]]
  sealed abstract class UserAttribute extends EnumEntry with Product with Serializable:
    def msg: I18nMessage
    def extract(user: UserDTO): String // TODO: optics
  object UserAttribute extends Enum[UserAttribute]:
    case object id           extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_id")
      override def extract(user: UserDTO): String = user.id.toString
    case object userName     extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_userName")
      override def extract(user: UserDTO): String = user.userName
    case object givenName    extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_givenName")
      override def extract(user: UserDTO): String = user.givenName
    case object middleName   extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_middleName")
      override def extract(user: UserDTO): String = user.middleName
    case object familyName   extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_familyName")
      override def extract(user: UserDTO): String = user.familyName
    case object emailAddress extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_emailAddress")
      override def extract(user: UserDTO): String = user.emailAddress
    case object externalId   extends UserAttribute:
      override def msg: I18nMessage               = I18nMessage("user_externalId")
      override def extract(user: UserDTO): String = user.externalId.getOrElse("")

    val values = findValues

    implicit val codec: CodecJson[UserAttribute] =
      CodecJson
        .derived[String]
        .xmap(_.toLowerCase)(_.toLowerCase)
        .xmap({
          case "id"           => id
          case "username"     => userName
          case "givenname"    => givenName
          case "middlename"   => middleName
          case "familyname"   => familyName
          case "emailaddress" => emailAddress
          case "externalid"   => externalId
        })(_.toString)
  end UserAttribute

  sealed abstract class ContentAttribute extends EnumEntry with Product with Serializable
  object ContentAttribute                extends Enum[ContentAttribute]:
    case object edgePath extends ContentAttribute
    case object title    extends ContentAttribute
    case object grade    extends ContentAttribute

    val values = findValues

    implicit val codec: CodecJson[ContentAttribute] =
      CodecJson
        .derived[String]
        .xmap(_.toLowerCase)(_.toLowerCase)
        .xmap({
          case "edgepath" => edgePath
          case "title"    => title
          case "grade"    => grade
        })(_.toString)
  end ContentAttribute

  case class SingleUserGradeHistoryDto(
    column: Option[GradeColumn],
    user: UserDTO,
    history: SingleGradeHistory
  )

  object SingleUserGradeHistoryDto:
    import SingleGradeHistory.*

    private implicit val userDtoCodec: CodecJson[UserDTO] = codecFromJackson[UserDTO]

    implicit val codec: CodecJson[SingleUserGradeHistoryDto] = CodecJson.casecodec3(
      SingleUserGradeHistoryDto.apply,
      ArgoExtras.unapply
    )("column", "user", "history")

  case class SingleUserIdGradeHistoryDto(
    column: GradeColumn,
    user: Uzr,
    history: SingleGradeHistory
  )
end LegacyGradebookWebController
