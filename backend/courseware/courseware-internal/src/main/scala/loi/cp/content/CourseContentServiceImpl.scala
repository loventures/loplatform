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
import loi.asset.course.model.Course
import loi.asset.gradebook.GradebookCategory1
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeService, Group, TraverseGraph}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.asset.edge.EdgeData
import loi.cp.competency.CompetentCompetencyService
import loi.cp.content.CustomisationTraversedGraphOps.*
import loi.cp.course.CourseAccessService.CourseRights
import loi.cp.course.lightweight.Lwc
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection, CourseWorkspaceService}
import loi.cp.customisation.{CourseCustomisationService, Customisation}
import loi.cp.reference.EdgePath
import loi.cp.right.RightService
import scalaz.std.list.*
import scalaz.std.set.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.data.ListTree.Node
import scaloi.syntax.listTree.*

import java.util.UUID
import scala.util.Try

@Service
class CourseContentServiceImpl(
  courseConfigurationService: CourseConfigurationService,
  customisationService: CourseCustomisationService,
  lightweightContentsCache: LightweightContentsCache,
  rightService: RightService,
  competentCompetencyService: CompetentCompetencyService,
  courseWorkspaceService: CourseWorkspaceService,
)(implicit edgeService: EdgeService)
    extends CourseContentService:

  // try is not necessary
  override def getCourseContents(course: Lwc, rights: Option[CourseRights]): Try[CourseContents] = Try {
    val contents = lightweightContentsCache.getOrComputeIfStaleGeneration(course)(() =>
      val customisation = customisationService.loadCustomisation(course)
      computeCourseContents(course, customisation)
    )
    // You can only access restricted content if this function is supplied with your
    // rights, and they indicate your permission. Most callers don't supply rights and
    // so will not grant anyone access to restricted content. Only the basic content
    // APIs supply rights because only basic types are intended to be so controlled.
    // Other types can be supported by adding to their controllers.
    contents.copy(tree =
      contents.tree
        .filtl(c => c.accessRight.forall(right => rights.exists(hasRight(right, _))))
        .get
    )
  }

  // If it's a known right, do you have it, otherwise are you an admin
  private def hasRight(rightName: String, rights: CourseRights) =
    Option(rightService.getRight(rightName)).cata(rights.hasRight, rights.isAdministrator)

  override def getCourseContent(
    course: Lwc,
    path: EdgePath,
    rights: Option[CourseRights]
  ): Try[Option[ContentPath]] =
    for contents <- getCourseContents(course, rights)
    yield
      val descendants = false
      contents.tree.findPath(_.edgePath == path) map { path =>
        val branch = pathTree(path.reverse.map(_.rootLabel), descendants ?? path.head.subForest).head
        ContentPath(path.head.rootLabel, branch = branch, contents = contents)
      }

  /** Given a path from course node to content node, return a skinny tree down to that node and its descendants.
    */
  private def pathTree(path: List[CourseContent], subForest: List[ContentTree]): List[ContentTree] =
    path match
      case head :: tail => List(Node(head, pathTree(tail, subForest)))
      case Nil          => subForest

  override def getCourseContentsInternal(course: Lwc, customisation: Customisation): Try[CourseContents] =
    Try(computeCourseContents(course, customisation))

  override def findUnassessables(ws: AttachedReadWorkspace, section: CourseSection): Set[UUID] =
    val coursePrefs = courseConfigurationService.getGroupConfig(CoursePreferences, section)
    if coursePrefs.hideUnassessableQuestions then

      val allContents   = getCourseContentsInternal(section.lwc, Customisation.empty).get
      val customisation = customisationService.loadCustomisation(section.lwc)

      val hiddenPaths   = customisation.overlays.values.flatMap(_.hide).flatten.toSet
      val hiddenTrees   = allContents.tree.findSubtrees(content => hiddenPaths.contains(content.edgePath))
      val hiddenContent = hiddenTrees.flatMap(_.flatten)

      hiddenContent.nonEmpty ?? {
        competentCompetencyService.getDirectlyTaughtCompetencies(ws, hiddenContent).values.fold(Set.empty[UUID])(_ ++ _)
      }
    else Set.empty
    end if
  end findUnassessables

  def computeCourseContents(lwc: Lwc, customisation: Customisation): CourseContents =
    computeCourseContentsImpl(courseWorkspaceService.loadReadWorkspace(lwc), lwc.course, customisation)

  def computeCourseContentsImpl(
    ws: AttachedReadWorkspace,
    course: Asset[Course],
    customisation: Customisation,
  ): CourseContents =
    val graph = edgeService.stravaigeOutGraphs(
      Seq(
        TraverseGraph
          .fromSource(course.info.name)
          .traverse(Group.GradebookCategories)
          .noFurther,
        TraverseGraph
          .fromSource(course.info.name)
          .traverse(Group.Elements)
          .traverse(Group.Elements, Group.Survey, Group.GradebookCategory, Group.Hyperlinks, Group.TestsOut)
          .traverse(Group.Elements, Group.Survey, Group.GradebookCategory, Group.Hyperlinks, Group.TestsOut)
          .traverse(Group.Elements, Group.Survey, Group.GradebookCategory, Group.Hyperlinks, Group.TestsOut)
          .traverse(Group.Survey, Group.GradebookCategory, Group.Hyperlinks, Group.TestsOut)
      ),
      ws
    )

    val categories          = for
      edge     <- graph.outEdgesInGroup(course, Group.GradebookCategories)
      category <- edge.target.filter[GradebookCategory1]
    yield GradebookCategory(EdgePath(edge.name), category)
    val supportedCategories = categories.map(_.asset.info.name).toSet

    val outTrees          = graph.customisedOutTrees(course, Nil, customisation, supportedCategories)
    val cacheableOutTrees = outTrees
      .filterNot(_.rootLabel.asset.is[GradebookCategory1])
      .map(_.map(_.cacheableContent))

    val root = CourseContent(
      Nil,
      course,
      EdgeData.empty,
      customisation(EdgePath.Root),
      accessRight = None,
      bannerImage = ws.outEdgeAttrs(ws.homeName, Group.Image).headOption.map(_.tgtName)
    )

    CourseContents(root.listNode(cacheableOutTrees*), categories.toList)
  end computeCourseContentsImpl
end CourseContentServiceImpl

final class LightweightContentsCache extends LightweightCourseCache[CourseContents]
