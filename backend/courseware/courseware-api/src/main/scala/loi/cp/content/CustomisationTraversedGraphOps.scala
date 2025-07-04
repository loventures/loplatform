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

import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.edge.{Group, TraversedGraph}
import loi.cp.asset.edge.PerformanceGateEdgeData
import loi.cp.customisation.{ContentOverlay, Customisation}
import loi.cp.reference.EdgePath
import scalaz.std.list.*
import scalaz.syntax.std.option.*
import scaloi.data.ListTree
import scaloi.syntax.collection.*
import scaloi.syntax.map.*

import java.util.UUID
import scala.language.implicitConversions

/** Adds customisation powers to `TraversedGraph`.
  */
final class CustomisationTraversedGraphOps(private val self: TraversedGraph) extends AnyVal:
  import CustomisationTraversedGraphOps.{ExcludedEdgeGroups, RootLevelExcludedEdgeGroups}

  /** hides and sorts edges in the out-trees of `source` according to `customisation`
    *
    * @param edgePath
    *   the path to `asset`. Useful if `asset` is not the course but you want the paths in the returned trees to be
    *   rooted at the course.
    * @param customisation
    *   instructor customisations
    * @param overlays
    *   authored overlays for remote content
    * @param supportedCategories
    *   supported category names
    */
  def customisedOutTrees(
    asset: Asset[?],
    edgePath: List[UUID] = Nil,
    customisation: Customisation = Customisation.empty,
    supportedCategories: Set[UUID] = Set.empty,
  ): List[ListTree[Content]] =
    // Note that this is used both for getting customised course contents and for getting
    // assessments with customised questions (asset is assessment.1 etc), so the return type
    // being [Content] is really a misnomer, and the survey/category support below thoroughly
    // questionable for that other use case.
    val outTrees = self
      .outTrees(asset)
      .filterNot(tree => RootLevelExcludedEdgeGroups.contains(tree.rootLabel.group))
      .map(outTree =>
        // This is gross. We take the asset graph, including .. surveys, categories, hyperlinks,
        // and turn them all into a tree of Content. Then we take that tree of Content, extract
        // out the things that aren't content and mutate the real content to mix in the non-content.
        val pathedTree = outTree.tdhisto[Content]({
          case (parent :: _, edge) => Content(parent.edgeNames, edge, customisation)
          case _                   => Content(edgePath, outTree.rootLabel, customisation)
        })

        pathedTree.rebuild[Content]((parent, children) =>
          val parentOverlay     = customisation(parent.edgePath)
          val groupedChildren   = children.groupBy(_.rootLabel.edge.group)
          val surveys           = groupedChildren.getOrZero(Group.Survey)
          val categories        = groupedChildren.getOrZero(Group.GradebookCategory)
          val hyperlinks        = groupedChildren.getOrZero(Group.Hyperlinks)
          // This should be IncludedEdgeGroups.flatterMap(groupedChildren.get) but tests use the course
          // content service to load trees of just about anything.
          val includedChildren  = children.filterNot(tree => ExcludedEdgeGroups.contains(tree.rootLabel.edge.group))
          val instructorOverlay = parent.overlay

          // This should be a single-entry list of the overlaid or linked category name
          val categoryNames = categories.map(_.rootLabel.asset.info.name)

          val testsOut = for
            testOut <- groupedChildren.getOrZero(Group.TestsOut)
            data    <- testOut.rootLabel.edge.data.get[PerformanceGateEdgeData]
          yield testOut.rootLabel.asset.info.name -> data.threshold

          // The directly linked survey
          val edgedSurvey = surveys.headOption.map(tree => tree.rootLabel.edge.name -> tree.rootLabel.asset.info.name)

          ListTree.Node(
            parent.copy(
              accessRight = parent.asset.accessRight,
              category = categoryNames.find(supportedCategories.contains),
              survey = edgedSurvey,
              hyperlinks = hyperlinks.map(c => c.rootLabel.edge.edgeId -> c.rootLabel.edge.target.info.name).toMap,
              overlay = instructorOverlay,
              testsOut = testsOut.toMap
            ),
            hideAndSortChildren(parentOverlay, includedChildren)
          )
        )
      )

    val assetOverlay = customisation(EdgePath(edgePath))
    hideAndSortChildren(assetOverlay, outTrees)
  end customisedOutTrees

  private def hideAndSortChildren(
    parentOverlay: ContentOverlay,
    children: List[ListTree[Content]],
  ): List[ListTree[Content]] =

    def isVisible(child: ListTree[Content]): Boolean =
      val hidden = parentOverlay.hide.exists(_.contains(child.rootLabel.edgePath))
      !hidden

    val visibleChildren = children.filter(isVisible)
    parentOverlay.order.cata(
      order =>
        // new here means "new since the customisation document was last saved"
        val orderSet = order.toSet
        val newEdges = visibleChildren.filterNot(child => orderSet.contains(child.rootLabel.edgePath))

        // flatMap will remove deleted content
        // deleted here means "deleted since the customisation document was last saved"
        val visibleChildrenByPath = visibleChildren.groupUniqBy(_.rootLabel.edgePath)
        order.flatMap(visibleChildrenByPath.get) ++ newEdges
      ,
      visibleChildren
    )
  end hideAndSortChildren
end CustomisationTraversedGraphOps

object CustomisationTraversedGraphOps extends ToCustomisationTraversedGraphOps:
  // Morally the course content tree should select just the edges that make sense;
  // should be includes Group.Elements, Group.Questions..
  // However the tests use the course content tree to load everything, so we have
  // these crazy exclusions to match the tests.
  private val ExcludedEdgeGroups =
    Set[Group](
      Group.Survey,
      Group.Hyperlinks,
      Group.GradebookCategories,
      Group.GradebookCategory,
      Group.TestsOut,
    )

  // Because a test needs this
  private val RootLevelExcludedEdgeGroups = ExcludedEdgeGroups - Group.Survey
end CustomisationTraversedGraphOps

trait ToCustomisationTraversedGraphOps:
  final implicit def toCustomisationTraversedGraphOps(
    self: TraversedGraph
  ): CustomisationTraversedGraphOps =
    new CustomisationTraversedGraphOps(self)
