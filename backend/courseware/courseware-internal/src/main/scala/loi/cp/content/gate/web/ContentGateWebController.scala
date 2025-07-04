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

package loi.cp.content
package gate
package web

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType as EnrolmentType
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService as EnrolmentWebService
import com.learningobjects.cpxp.service.query.Comparison
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.course.lightweight.{LightweightCourse, Lwc}
import loi.cp.course.right.TeachCourseRight
import loi.cp.reference.EdgePath
import scalaz.\/
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scaloi.data.SetDelta
import scaloi.syntax.`try`.*
import scaloi.syntax.functor.*

import scala.jdk.CollectionConverters.*

@Controller(value = "lwc-gating", root = true)
trait ContentGateWebController extends ApiRootComponent:
  import Accommodations.*
  import ContentGateWebController.*

  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  @RequestMapping(path = "lwc/{context}/gateOverrides", method = Method.GET)
  def getGateOverrides(
    @PathVariable("context") @SecuredAdvice context: Long,
  ): ErrOr[ArgoBody[GateOverrides]]

  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  @RequestMapping(path = "lwc/{context}/gateOverrides", method = Method.PUT)
  def updateGateOverrides(
    @PathVariable("context") @SecuredAdvice context: Long,
    @RequestBody request: ArgoBody[UpdateGateOverridesRequest],
  ): ErrOr[ArgoBody[GateOverrides]]

  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  @RequestMapping(path = "lwc/{context}/accommodations/{user}", method = Method.GET)
  def getAccommodations(
    @PathVariable("context") @SecuredAdvice context: Long,
    @PathVariable("user") user: Long,
  ): ErrOr[ArgoBody[Accommodations]]

  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight]))
  @RequestMapping(path = "lwc/{context}/accommodations/{user}/{path}", method = Method.PUT)
  def updateAccommodation(
    @PathVariable("context") @SecuredAdvice context: Long,
    @PathVariable("user") user: Long,
    @PathVariable("path") path: EdgePath,
    @RequestBody request: ArgoBody[UpdateAccommodation],
  ): ErrOr[Unit]
end ContentGateWebController

@Component
class ContentGateWebControllerImpl(val componentInstance: ComponentInstance)(
  contentService: CourseContentService,
  overrideService: ContentGateOverrideService,
  enrolmentWebService: EnrolmentWebService,
)(implicit cs: ComponentService)
    extends ContentGateWebController
    with ComponentImplementation:
  import Accommodations.*
  import ContentGateWebController.*
  import ErrorResponse.*

  override def getGateOverrides(context: Long): ErrOr[ArgoBody[GateOverrides]] =
    for
      course    <- loadCourse(context)
      overrides <- overrideService.loadOverrides(course) \/> serverError
    yield ArgoBody(overrides)

  override def updateGateOverrides(
    context: Long,
    request0: ArgoBody[UpdateGateOverridesRequest],
  ): ErrOr[ArgoBody[GateOverrides]] = for
    request   <- request0.decode_!.disjoin(badRequest)
    course    <- loadCourse(context)
    _         <- request match
                   case usog: UpdateStudentOverridesRequest       =>
                     updateStudentOverrides(course, usog)
                   case uagor: UpdateActivityGateOverridesRequest =>
                     updateActivityGateOverrides(course, uagor)
    overrides <- overrideService.loadOverrides(course) \/> serverError
  yield ArgoBody(overrides)

  override def getAccommodations(context: Long, user: Long): ErrOr[ArgoBody[Accommodations]] =
    for
      course         <- loadCourse(context)
      accommodations <- overrideService.loadAccommodations(course, UserId(user)) \/> serverError
    yield ArgoBody(accommodations)

  override def updateAccommodation(
    context: Long,
    user: Long,
    path: EdgePath,
    request: ArgoBody[UpdateAccommodation]
  ): ErrOr[Unit] =
    for
      course <- loadCourse(context)
      // validate neither user nor edgepath
      update <- request.decode_! \/> serverError
    yield overrideService.updateAccommodations(course, UserId(user), path, update.maxMinutes)

  private def updateStudentOverrides(
    course: LightweightCourse,
    request: UpdateStudentOverridesRequest,
  ): ErrOr[Unit] = for
    _      <- ensureEnrolments(course, request.userIds)
    change  =
      if request.enabled then SetDelta.remove(request.content)
      else SetDelta.add(request.content)
    changes =
      import request.*
      if userIds.isEmpty then GateOverrides.Changes(overall = change)
      else GateOverrides.Changes(perUser = (userIds.toList <*- change).toMap)
    _      <- overrideService.updateOverrides(course)(changes) \/> serverError
  yield ()

  private def updateActivityGateOverrides(
    course: LightweightCourse,
    request: UpdateActivityGateOverridesRequest,
  ): ErrOr[Unit] =
    val change  =
      if request.enabled then SetDelta.remove(request.assignments)
      else SetDelta.add(request.assignments)
    val changes = GateOverrides.Changes(assignment = Map(request.content -> change))
    overrideService.updateOverrides(course)(changes) \/> serverError

  private def loadCourse(id: Long): ErrOr[LightweightCourse] =
    id.component_![LightweightCourse] \/>| notFound(s"no such course: $id")

  private def ensureEnrolments(course: Lwc, userIds: Set[Long]): ErrOr[List[UserId]] =
    val actualEnrolments  = enrolmentWebService
      .getGroupEnrollmentsQuery(course.id, EnrolmentType.ACTIVE_ONLY)
      .addCondition(DataTypes.META_DATA_TYPE_PARENT_ID, Comparison.in, userIds.asJava)
      .parentIds()
      .toSet
    val missingEnrolments = userIds &~ actualEnrolments
    missingEnrolments.isEmpty either userIds.view.map(UserId(_)).toList or notFound(
      s"users [${userIds.mkString(", ")}] not found in ${course.id}"
    )
  end ensureEnrolments
end ContentGateWebControllerImpl

object ContentGateWebController:

  private[web] type ErrOr[T] = ErrorResponse \/ T

  sealed abstract class UpdateGateOverridesRequest
  final case class UpdateStudentOverridesRequest(
    userIds: Set[Long],
    content: Set[EdgePath],
    enabled: Boolean,
  ) extends UpdateGateOverridesRequest
  final case class UpdateActivityGateOverridesRequest(
    content: EdgePath,
    assignments: Set[EdgePath],
    enabled: Boolean,
  ) extends UpdateGateOverridesRequest
  implicit val ugorCodec: CodecJson[UpdateGateOverridesRequest] =
    import argonaut.*
    import scaloi.json.ArgoExtras.*
    val usorCodec                                                     = CodecJson.derive[UpdateStudentOverridesRequest]
    val uagorCodec                                                    = CodecJson.derive[UpdateActivityGateOverridesRequest]
    def encode(ugor: UpdateGateOverridesRequest)                      = ugor match
      case usog: UpdateStudentOverridesRequest       => usorCodec encode usog
      case uagor: UpdateActivityGateOverridesRequest => uagorCodec encode uagor
    def decode(hc: HCursor): DecodeResult[UpdateGateOverridesRequest] =
      def usorly: DecodeResult[UpdateGateOverridesRequest]  =
        if (hc downField "userIds").succeeded then hc.as(using usorCodec).widen
        else DecodeResult.fail("not an UpdateStudentOverridesRequest", hc.history)
      def uagorly: DecodeResult[UpdateGateOverridesRequest] =
        if (hc downField "assignments").succeeded then hc.as(using uagorCodec).widen
        else DecodeResult.fail("not an UpdateActivityGateOverridesRequest", hc.history)
      (usorly ||| uagorly)
        .withHint("could not determine type of UpdateGateOverridesRequest")
    CodecJson(encode, decode)
  end ugorCodec

  final case class UpdateAccommodation(maxMinutes: Option[Long])

  implicit val uaCodec: CodecJson[UpdateAccommodation] = CodecJson.derive[UpdateAccommodation]
end ContentGateWebController
