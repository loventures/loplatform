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

package loi.asset.competency.service

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.competency.model.*
import loi.asset.root.model.Root
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.exchange.exprt.CompetencyUtil
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}

import java.util.UUID

@Service
class BaseCompetencyService(
  edgeService: EdgeService,
  nodeService: AssetNodeService,
) extends CompetencyService:

  override def findCompetencySets(
    ws: ReadWorkspace,
    assets: Seq[Asset[?]]
  ): Seq[Asset[CompetencySet]] =

    import scala.collection.mutable
    val compSetIds = mutable.Set.empty[Long]
    val lvl1Ids    = mutable.Set.empty[Long]
    val lvl2Ids    = mutable.Set.empty[Long]
    val lvl3Ids    = mutable.Set.empty[Long]
    assets.foreach({
      case CompetencySet.Asset(set)     => compSetIds += set.info.id
      case Level1Competency.Asset(lvl1) => lvl1Ids += lvl1.info.id
      case Level2Competency.Asset(lvl2) => lvl2Ids += lvl2.info.id
      case Level3Competency.Asset(lvl3) => lvl3Ids += lvl3.info.id
      case _                            =>
    })

    lvl3Ids.foreach(lvl3Id => lvl2Ids ++= ws.inEdgeInfos(lvl3Id, Group.Level3Competencies).map(_.sourceId))
    lvl2Ids.foreach(lvl2Id => lvl1Ids ++= ws.inEdgeInfos(lvl2Id, Group.Level2Competencies).map(_.sourceId))
    lvl1Ids.foreach(lvl1Id => compSetIds ++= ws.inEdgeInfos(lvl1Id, Group.Level1Competencies).map(_.sourceId))

    nodeService.loadA[CompetencySet](ws).byId(compSetIds)
  end findCompetencySets

  override def findParents(
    ws: ReadWorkspace,
    competency: Asset[?]
  ): Seq[Asset[?]] =
    competency match
      case Level3Competency.Asset(lvl3) =>
        edgeService
          .loadInEdgesS[Level2Competency](ws, Seq(lvl3), Set(Group.Level3Competencies))
          .map(_.source)
      case Level2Competency.Asset(lvl2) =>
        edgeService
          .loadInEdgesS[Level1Competency](ws, Seq(lvl2), Set(Group.Level2Competencies))
          .map(_.source)
      case _                            => Seq.empty

  override def getCompetenciesByName(ws: AttachedReadWorkspace): Map[String, UUID] =
    val root         = nodeService.loadA[Root](ws).byName(ws.rootName).get
    val graph        = edgeService.stravaigeOutGraph(CompetencyUtil.rootCompetenciesGraph(root), ws)
    val competencies = CompetencyUtil.rootCompetencies(root, graph)
    competencies.map({
      case (name, Level1Competency.Asset(l1)) => CompetencyService.normalize(l1.data.title) -> name
      case (name, Level2Competency.Asset(l2)) => CompetencyService.normalize(l2.data.title) -> name
      case (name, Level3Competency.Asset(l3)) => CompetencyService.normalize(l3.data.title) -> name
      case (name, _)                          => ""                                         -> name
    })
end BaseCompetencyService
