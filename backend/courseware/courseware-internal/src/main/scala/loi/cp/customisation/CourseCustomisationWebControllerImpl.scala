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

package loi.cp.customisation

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ArgoBody
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.resource.model.Resource1
import loi.cp.content.{ContentAccessService, ContentDateUtils, ContentDates, CourseContent}
import loi.cp.course.CourseSectionService
import loi.cp.course.lightweight.LightweightCourseService
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.progress.LightweightProgressService
import loi.cp.reference.EdgePath
import loi.asset.util.Assex.*
import loi.authoring.syntax.index.*
import scalaz.syntax.std.option.*
import scaloi.data.ListTree
import scaloi.json.ArgoExtras.*
import scaloi.syntax.OptionOps.*
import scaloi.syntax.TryOps.*

import scala.util.Try

@Component
class CourseCustomisationWebControllerImpl(
  val componentInstance: ComponentInstance,
  lwcService: LightweightCourseService,
  domain: => DomainDTO
)(implicit
  contentAccessService: ContentAccessService,
  courseSectionService: CourseSectionService,
  gateDateSchedulingService: GateDateSchedulingService,
  customisationService: CourseCustomisationService,
  progressService: LightweightProgressService,
  user: UserDTO
) extends CourseCustomisationWebController
    with ComponentImplementation:

  import CourseCustomisationService.*
  import CourseCustomisationWebController.*

  override def resetCustomisation(context: Long): Try[Unit] =
    for lwc <- contentAccessService.getCourseAsInstructor(context, user)
    yield
      customisationService.updateCustomisation(lwc, ResetCustomisation)
      lwcService.incrementGeneration(lwc)
      progressService.scheduleProgressUpdate(context)

  override def updateContent(
    context: Long,
    path: EdgePath,
    argoUpdate: ArgoBody[ContentOverlayUpdate]
  ): Try[ArgoBody[ContentOverlay]] =
    for
      update       <- argoUpdate.decode_!
      course       <- contentAccessService.getCourseAsInstructor(context, user)
      customisation = customisationService.updateCustomisation(course, path, UpdateOverlay(update))
      _             = lwcService.incrementGeneration(course)
      _             = progressService.scheduleProgressUpdate(context)
      _            <- gateDateSchedulingService.scheduleGateDateEvents(course).success
    yield ArgoBody(customisation.overlays(path))

  override def updateContents(
    context: Long,
    argoUpdate: ArgoBody[Map[EdgePath, ContentOverlayUpdate]]
  ): Try[ArgoBody[Map[EdgePath, ContentOverlay]]] =
    for
      update       <- argoUpdate.decode_!
      lwc          <- contentAccessService.getCourseAsInstructor(context, user)
      customisation = customisationService.updateCustomisation(lwc, BulkUpdateOverlay(update))
      _             = lwcService.incrementGeneration(lwc)
      _             = progressService.scheduleProgressUpdate(context)
      _            <- gateDateSchedulingService.scheduleGateDateEvents(lwc).success
    yield ArgoBody {
      val result = update map { case (path, _) =>
        path -> customisation(path)
      }
      result
    }

  override def customisableContents(context: Long): Try[ArgoBody[ListTree[CustomisableContent]]] =
    // TODO: Remove extraneous content load
    for
      lwc          <- contentAccessService.getCourseAsInstructor(context, user)
      customisation = customisationService.loadCustomisation(lwc)
      unhidden      = customisation.copy(overlays = customisation.overlays.view.mapValues(_.copy(hide = None)).toMap)
      section      <- courseSectionService.getCourseSectionInternal(lwc.id, unhidden) <@~* new Exception("No course")
    yield ArgoBody(
      section.contents.tree
        .map(toDto(_, ContentDates(section.courseAvailabilityDates, section.courseDueDates), customisation))
    )

  override def getCourseInfo(context: Long): Try[CourseInfo] =
    for lwc <- contentAccessService.getCourseAsInstructor(context, user)
    yield CourseInfo(lwc.getName, lwc.getUrl)

  private def toDto(
    content: CourseContent,
    contentDates: ContentDates,
    customisation: Customisation
  ): CustomisableContent =
    CustomisableContent(
      id = content.edgePath,
      title = content.title,
      instructions = content.asset.instructions.map(_.htmls.mkString),
      resourceType = resourceType(content),
      gateDate = contentDates.gateDate(content.edgePath),
      dueDate = contentDates.dueDate(content.edgePath),
      gateDateOffset = ContentDateUtils.edgeGateOffset(content),
      dueDateOffset = ContentDateUtils.dueDateOffset(content),
      gradable = content.gradingPolicy.isDefined,
      titleCustomised = content.overlay.title.isDefined,
      instructionsCustomised = content.overlay.instructions.isDefined,
      dueDateCustomised = content.overlay.dueDate.nonAbsent,
      gateDateCustomised = content.overlay.gateDate.isDefined,
      pointsPossible = content.gradingPolicy.map(_.pointsPossible),
      pointsPossibleCustomised = content.overlay.pointsPossible.isDefined,
      isForCredit = content.gradingPolicy.map(_.isForCredit),
      isForCreditCustomised = content.overlay.isForCredit.isDefined,
      typeId = content.asset.info.typeId,
      hide = customisation(content.edgePath).hide.cata(_.toList, Nil),
      orderCustomised = customisation(content.edgePath).order.isDefined,
      metadata = content.overlay.metadata
    )

  private def resourceType(content: CourseContent): Option[String] = PartialFunction.condOpt(content.asset.data) {
    case r1: Resource1 => r1.resourceType.toString
  }
end CourseCustomisationWebControllerImpl
