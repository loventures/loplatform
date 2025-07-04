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

package loi.authoring.exchange.exprt

import loi.asset.competency.model.CompetencySet
import loi.asset.root.model.Root
import loi.authoring.asset.Asset
import loi.authoring.edge.Group.*
import loi.authoring.edge.{Group, TraverseGraph, TraversedGraph}
import scaloi.syntax.collection.*

import java.util.UUID

/** Especially in the multiverse, but anyway, assets may have competency alignment from competencies not in the program
  * (perhaps they are remote, perhaps the competency set was edited). These functions assist in extracting the actual
  * alignment of things, by looking at overlays and by filtering to the competencies of the program.
  */
object CompetencyUtil:

  def rootCompetenciesGraph(root: Asset[Root]) =
    TraverseGraph
      .fromSource(root.info.name)
      .traverse(CompetencySets)
      .traverse(Level1Competencies)
      .traverse(Level2Competencies)
      .traverse(Level3Competencies)

  def csCompetenciesGraph(cs: Asset[CompetencySet]) =
    TraverseGraph
      .fromSource(cs.info.name)
      .traverse(Level1Competencies)
      .traverse(Level2Competencies)
      .traverse(Level3Competencies)

  def rootCompetencies(root: Asset[Root], graph: TraversedGraph): Map[UUID, Asset[?]] =
    graph
      .targetsInGroup(root, CompetencySets)
      .filter(asset => CompetencySet.Asset.unapply(asset).exists(cs => !cs.data.archived))
      .flatMap(cs => graph.outTrees(cs).flatMap(_.flatten.map(_.target)))
      .groupUniqBy(_.info.name)

  def csCompetencies(cs: Asset[CompetencySet], graph: TraversedGraph): Map[UUID, Asset[?]] =
    graph
      .outTrees(cs)
      .flatMap(_.flatten.map(_.target))
      .groupUniqBy(_.info.name)

  def assetCompetencies(
    asset: Asset[?],
    group: Group,
    graph: TraversedGraph,
    competencies: Map[UUID, Asset[?]]
  ): Seq[Asset[?]] =
    graph.targetsInGroup(asset, group).map(_.info.name).flatMap(competencies.get)
end CompetencyUtil
