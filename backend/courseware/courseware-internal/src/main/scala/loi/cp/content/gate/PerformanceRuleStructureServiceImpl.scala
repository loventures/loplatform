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

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.edge.{EdgeService, Group, TraverseGraph}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.asset.edge.PerformanceGateEdgeData
import loi.cp.course.lightweight.Lwc
import scalaz.std.option.*
import scaloi.syntax.collection.*
import scaloi.syntax.functor.*

import scala.util.Try

@Service
class PerformanceRuleStructureServiceImpl(
  cache: PerformanceRuleStructureCache,
  lccs: CourseContentService,
  gos: ContentGateOverrideService,
)(implicit
  edgeService: EdgeService,
) extends PerformanceRuleStructureService:
  // Currently this pulls a right-restricted course contents so if an instructor-only
  // assignment gates student content, the student doesn't see the instructor content
  // so the gate is not applied. This prevents, for example, a hidden observation
  // assignment used to grant certain students access to certain content. This would
  // be easily remedied, but the front end would then see gating information for an
  // edge path that doesn't exist, so it would also need to be updated.
  def computePerformanceRuleStructure(ws: AttachedReadWorkspace, lwc: Lwc): Try[PerformanceRule.Structure] =
    def compute() = for
      ccs       <- lccs.getCourseContents(lwc)
      overrides <- gos.loadOverrides(lwc)
    yield
      val contents = ccs.nonRootElements

      val gateEdges = edgeService.stravaigeOutGraphs(
        TraverseGraph
          .fromSources(contents.map(_.asset.info.name)*)
          .traverse(Group.Gates)
          .noFurther :: Nil,
        ws
      )

      // First we search for assets that gate other assets
      val assetToGates = contents
        .flatMap(c =>
          for
            edge <- gateEdges.outEdgesInGroup(c.asset, Group.Gates)
            perf <- edge.data.get[PerformanceGateEdgeData]
          yield edge.target.info.name -> (c.edgePath -> perf.threshold)
        )
        .groupToMap

      // Then we convert to assets that are gated (reuse...)
      val contentGates = contents.flatMap(c => c.edgePath -*>: assetToGates.get(c.asset.info.name))

      PerformanceRule.Structure(contentGates.toMap, overrides.assignment)

    Try(cache.getOrComputeIfStaleGeneration(lwc)(() => compute().get))
  end computePerformanceRuleStructure
end PerformanceRuleStructureServiceImpl

class PerformanceRuleStructureCache extends LightweightCourseCache[PerformanceRule.Structure]
