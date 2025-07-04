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

package loi.cp.course

import argonaut.{CodecJson, Json}
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.asset.resource.model.Resource1
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.CourseAssessmentPolicy
import loi.cp.content.{Content, ContentDateUtils, CourseContent}
import loi.cp.context.ContextId
import loi.cp.course.InstructorCustomisationWebController.{
  DueDateAccommodationDto,
  DueDateAccommodationRequestDto,
  DueDatePolicyDto,
  PointsPossibleDto
}
import loi.cp.course.right.TeachCourseRight
import loi.cp.customisation.ContentOverlay
import loi.cp.reference.EdgePath
import scalaz.\/
import scaloi.json.ArgoExtras

import java.time.Instant

/** Customise properties on a course. Performs similarly to loi.cp.customisation.CourseCustomisationWebController but
  * with extra validation if necessary.
  */
@Controller(root = true)
trait InstructorCustomisationWebController extends ApiRootComponent:

  /** Change the points possible for course content. If attempts or work has been done against the assignment, the
    * points will not be allowed to change.
    *
    * @param contextId
    *   The Course ID.
    * @param path
    *   The path to the content whose points will be changed.
    * @param pointConfig
    *   The new point value for the content.
    * @return
    *   The updated content overlay.
    */
  @RequestMapping(path = "contentConfig/pointsPossible/{path}", method = Method.POST)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def setPointsPossible(
    @SecuredAdvice @MatrixParam("context") contextId: Long,
    @PathVariable("path") path: EdgePath,
    @RequestBody pointConfig: ArgoBody[PointsPossibleDto]
  ): ErrorResponse \/ ArgoBody[ContentOverlay]

  @RequestMapping(path = "contentConfig/dueDate", method = Method.POST)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def updateDueDatePolicy(
    @SecuredAdvice @MatrixParam("context") contextId: ContextId,
    @RequestBody preferences: ArgoBody[DueDatePolicyDto]
  ): ErrorResponse \/ Unit

  @RequestMapping(path = "contentConfig/dueDateAccommodation", method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def loadDueDateAccommodation(
    @SecuredAdvice @MatrixParam("context") contextId: ContextId
  ): ArgoBody[DueDateAccommodationDto]

  @RequestMapping(path = "contentConfig/dueDateAccommodation", method = Method.POST)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def updateDueDateAccommodation(
    @SecuredAdvice @MatrixParam("context") contextId: ContextId,
    @RequestBody accommodations: ArgoBody[DueDateAccommodationRequestDto]
  ): ErrorResponse \/ Unit
  @RequestMapping(path = "contentConfig/{context}/assessmentPolicies", method = Method.PUT)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def updateCourseAssessmentPolicies(
    @SecuredAdvice @PathVariable("context") contextId: Long,
    @RequestBody policies: ArgoBody[List[CourseAssessmentPolicy]]
  ): ErrorResponse \/ Unit

  @RequestMapping(path = "contentConfig/{context}/assessmentPolicies", method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight]))
  def getCourseAssessmentPolicies(
    @SecuredAdvice @PathVariable("context") contextId: Long
  ): ErrorResponse \/ List[CourseAssessmentPolicy]
end InstructorCustomisationWebController

object InstructorCustomisationWebController:
  import argonaut.*

  implicit val pointsPossibleEncoder: EncodeJson[PointsPossibleDto] = EncodeJson.derive[PointsPossibleDto]
  implicit val pointsPossibleDecoder: DecodeJson[PointsPossibleDto] = DecodeJson.derive[PointsPossibleDto]
  final case class PointsPossibleDto(pointsPossible: Double)

  implicit val dueDateEncoder: EncodeJson[DueDatePolicyDto] = EncodeJson.derive[DueDatePolicyDto]
  implicit val dueDateDecoder: DecodeJson[DueDatePolicyDto] = DecodeJson.derive[DueDatePolicyDto]
  final case class DueDatePolicyDto(strictDueDate: Boolean)

  implicit val exemptLearnerEncoder: EncodeJson[ExemptLearnerDto] = EncodeJson.derive[ExemptLearnerDto]
  implicit val exemptLearnerDecoder: DecodeJson[ExemptLearnerDto] = DecodeJson.derive[ExemptLearnerDto]
  final case class ExemptLearnerDto(id: Long, givenName: String, familyName: String)

  implicit val dueDateAccommodationEncoder: EncodeJson[DueDateAccommodationDto] =
    EncodeJson.derive[DueDateAccommodationDto]
  implicit val dueDateAccommodationDecoder: DecodeJson[DueDateAccommodationDto] =
    DecodeJson.derive[DueDateAccommodationDto]
  final case class DueDateAccommodationDto(exemptLearners: List[ExemptLearnerDto])

  implicit val dueDateAccommodationRequestEncoder: EncodeJson[DueDateAccommodationRequestDto] =
    EncodeJson.derive[DueDateAccommodationRequestDto]
  implicit val dueDateAccommodationRequestDecoder: DecodeJson[DueDateAccommodationRequestDto] =
    DecodeJson.derive[DueDateAccommodationRequestDto]
  final case class DueDateAccommodationRequestDto(exemptLearners: Set[Long])
end InstructorCustomisationWebController

// CustomisableContent but with the asset data
final case class CustomisableContentWithAssetData(
  id: EdgePath,
  title: String,
  resourceType: Option[String],
  gateDate: Option[Instant],
  dueDate: Option[Instant],
  gateDateOffset: Option[Long],
  dueDateOffset: Option[Long],
  gradable: Boolean,
  titleCustomised: Boolean,
  dueDateCustomised: Boolean,
  gateDateCustomised: Boolean,
  typeId: AssetTypeId,
  hide: List[EdgePath],
  metadata: Option[Json],
  assetData: JsonNode,
)

object CustomisableContentWithAssetData:

  def fromContent(content: Content, section: CourseSection): CustomisableContentWithAssetData =
    fromContent(content.cacheableContent, section)

  def fromContent(
    content: CourseContent,
    section: CourseSection,
  ): CustomisableContentWithAssetData =

    def resourceType(content: CourseContent): Option[String] = PartialFunction.condOpt(content.asset.data) {
      case r1: Resource1 => r1.resourceType.toString
    }

    CustomisableContentWithAssetData(
      id = content.edgePath,
      title = content.title,
      resourceType = resourceType(content),
      gateDate = section.courseAvailabilityDates.get(content.edgePath),
      dueDate = section.courseDueDates.get(content.edgePath),
      gateDateOffset = ContentDateUtils.edgeGateOffset(content),
      dueDateOffset = ContentDateUtils.dueDateOffset(content),
      gradable = content.gradingPolicy.isDefined,
      titleCustomised = content.overlay.title.isDefined,
      dueDateCustomised = content.overlay.dueDate.isDefined,
      gateDateCustomised = content.overlay.gateDate.isDefined,
      typeId = content.asset.info.typeId,
      hide = content.overlay.hide.map(_.toList).getOrElse(Nil),
      metadata = content.overlay.metadata,
      assetData = JacksonUtils.getFinatraMapper.valueToTree[JsonNode](content.asset.data),
    )
  end fromContent

  import com.learningobjects.cpxp.scala.json.JacksonCodecs.*
  import scaloi.json.ArgoExtras.*

  implicit val codecJson: CodecJson[CustomisableContentWithAssetData] =
    CodecJson.casecodec15(CustomisableContentWithAssetData.apply, ArgoExtras.unapply)(
      "id",
      "title",
      "resourceType",
      "gateDate",
      "dueDate",
      "gateDateOffset",
      "dueDateOffset",
      "gradable",
      "titleCustomised",
      "dueDateCustomised",
      "gateDateCustomised",
      "typeId",
      "hide",
      "metadata",
      "assetData",
    )
end CustomisableContentWithAssetData
