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

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.lti.Lti
import loi.asset.resource.model.Resource1
import loi.authoring.asset.*
import loi.authoring.edge.Group
import loi.cp.competency.Competency
import loi.cp.content.gate.*
import loi.cp.context.ContextId
import loi.cp.ltitool.LtiLaunchStyle
import loi.cp.lwgrade.Grade
import loi.cp.progress.report.Progress
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scalaz.std.option.*
import scalaz.std.tuple.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*

import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
object ContentDtoUtilsImpl extends ContentDtoUtils:

  def toDto(
    content: CourseContent,
    contextId: ContextId,
    index: Int,
    parents: List[EdgePath],
    gateSummary: GateSummary,
    dueDate: Option[DueDate],
    dueDateExempt: Option[Boolean],
    progress: Option[Progress],
    grade: Option[Grade],
    competencies: Seq[Competency],
    hasSurvey: Boolean,
    categoryMap: Map[UUID, GradebookCategory],
    edgePaths: Map[UUID, EdgePath],
  ): CourseContentDto =

    CourseContentDto(
      id = content.edgePath.toString, // This seems rather wrong, but this is what it was and will be less wrong
      parent_id = parents.headOption.map(_.toString),
      name = content.title,
      description = content.description,
      contentType = "",
      index = index,
      path = new loi.cp.path.Path((content.edgePath :: parents).reverse.tail.map(_.toString).asJava),
      logicalGroup = Group.Elements.entryName,
      depth = parents.size,
      assetId = content.asset.info.id,
      typeId = content.asset.info.typeId,
      subType = subType(content.asset),
      iconClass = IconClass(content.asset),
      dueDate = dueDate,
      dueDateExempt = dueDateExempt,
      maxMinutes = content.maxMinutes,
      duration = content.duration,
      hasGradebookEntry = content.gradingPolicy.nonEmpty,
      nodeName = content.asset.info.name,
      gatingInformation = createGatingInformation(gateSummary),
      progress = progress,
      grade = grade,
      contentId = ContentIdentifier(contextId, content.edgePath),
      competencies = competencies,
      metadata = content.overlay.metadata,
      hasSurvey = hasSurvey,
      isForCredit = content.overlay.isForCredit orElse content.isForCredit,
      accessControlled = content.accessRight.as(true),
      gradebookCategory = content.category.flatMap(categoryMap.get).map(_.asset.data.title),
      hyperlinks = content.hyperlinks.flatMap(_.traverse(edgePaths.get)),
      bannerImage = content.bannerImage,
    )

  private def createGatingInformation(gatingSummary: GateSummary) =
    GatingInformation(
      gate =
        if gatingSummary.isEmpty then None
        else
          Some {
            import ContentGateDto.*
            val temporalGatingPolicy    =
              gatingSummary.temporal.lockDate.map(TemporalGateInfo.apply)
            val performanceGatingPolicy = gatingSummary.performance.criteria.nonEmpty option {
              ActivityGateInfo(
                gatingSummary.performance.criteria.map { case (path, threshold) =>
                  SingleActivityGate(path.toString, threshold, gatingSummary.performance.disabled(path))
                }
              )
            }
            val rightsGatingPolicy      = gatingSummary.policy.map(RightsGatingInfo.apply)
            ContentGateDto(
              enabled = !gatingSummary.overridden,
              temporalGatingPolicy = temporalGatingPolicy,
              activityGatingPolicy = performanceGatingPolicy,
              rightsGatingPolicy = rightsGatingPolicy,
            )
          }
      ,
      gateStatus = gatingSummary.status
    )

  private def subType(node: Asset[?]): Option[String] = PartialFunction.condOpt(node.data) {
    case resource1: Resource1 => resource1.resourceType.entryName
    // TODO: Kill this Lti case once legacy course is gone. This does not look at the LTI tool config.
    case lti: Lti             => lti.lti.toolConfiguration.launchStyle.fold(LtiLaunchStyle.NEW_WINDOW.name)(_.name)
  }
end ContentDtoUtilsImpl
