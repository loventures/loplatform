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

package loi.cp.competency
import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.competency.model.CompetencySet
import loi.authoring.asset.Asset
import loi.authoring.edge.Group.*
import loi.authoring.edge.{EdgeService, Group}
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.content.{CourseContent, CourseContents}
import loi.cp.course.CourseSection
import loi.cp.reference.EdgePath
import scalaz.syntax.std.option.*
import scaloi.data.ListTree
import scaloi.syntax.collection.*

import java.util.UUID
import scala.collection.mutable

@Service
class CompetentCompetencyService()(implicit
  nodeService: AssetNodeService,
  edgeService: EdgeService
):

  // Ordered competencies of the program, excluding archived competency sets
  def getCompetencyForest(ws: AttachedReadWorkspace): List[ListTree[Competency]] =
    // Historically, competency sets were archived so we have to load them first to retain only used ones
    val csIds          = ws.outEdgeInfos(ws.rootName, Group.CompetencySets).toSeq.sortBy(_.position).map(_.targetId)
    val competencySets = nodeService.loadA[CompetencySet](ws).byId(csIds)

    def loop(name: UUID): List[ListTree[UUID]] =
      ws.outEdgeAttrs(name, CompetencyGroups)
        .toSeq
        .sortBy(_.position)
        .map(edge => ListTree.Node(edge.tgtName, loop(edge.tgtName)))
        .toList

    val forest = competencySets
      .filter(cs => !cs.data.archived)
      .flatMap(cs => loop(cs.info.name))

    val nodes =
      nodeService.load(ws).byName(forest.flatMap(_.flatten)).get.flatMap(Competency.fromAsset).groupUniqBy(_.nodeName)
    forest.map(_.map(nodes.apply))
  end getCompetencyForest

  def getCompetencyMap(ws: AttachedReadWorkspace): Map[UUID, Competency] =
    getCompetencyForest(ws).flatMap(_.flatten).groupUniqBy(_.nodeName)

  // Get all aligned contents by competency. Some competencies not be in the program competency set anymore.
  def getContentsByCompetency(ws: AttachedReadWorkspace, section: CourseSection): Map[UUID, Set[EdgePath]] =
    getContentsByCompetencyImpl(ws, section.contents)

  def getContentsByCompetencyImpl(ws: AttachedReadWorkspace, contents: CourseContents): Map[UUID, Set[EdgePath]] =
    val result = mutable.Map.empty[UUID, Set[EdgePath]]
    contents.nonRootElements foreach { content =>
      def loop(name: UUID): Unit =
        ws.outEdgeAttrs(name, DescendantGroups).map(_.tgtName).foreach(loop)
        for
          group      <- AlignmentGroups
          competency <- ws.outEdgeAttrs(name, group).map(_.tgtName)
        do result.updateWith(competency)(o => Some(o.cata(_ + content.edgePath, Set(content.edgePath))))

      // We have to look at all the content rather than just traversing the elements hierarchy
      // because some content may be instructor hidden.
      loop(content.name)
    }
    result.toMap
  end getContentsByCompetencyImpl

  def getDirectlyTaughtCompetencies(
    ws: AttachedReadWorkspace,
    contents: Seq[CourseContent],
  ): Map[EdgePath, Set[UUID]] =
    val result = mutable.Map.empty[EdgePath, Set[UUID]]
    contents foreach { content =>
      for competency <- ws.outEdgeAttrs(content.name, Teaches).map(_.tgtName)
      do result.updateWith(content.edgePath)(o => Some(o.cata(_ + competency, Set(competency))))
    }
    result.toMap
  end getDirectlyTaughtCompetencies

  def getAssessedCompetencies(
    ws: AttachedReadWorkspace,
    contents: Seq[CourseContent],
  ): Map[EdgePath, Set[UUID]] =
    val result = mutable.Map.empty[EdgePath, Set[UUID]]
    contents foreach { content =>
      def loop(name: UUID): Unit =
        ws.outEdgeAttrs(name, DescendantGroups).map(_.tgtName).foreach(loop)
        for competency <- ws.outEdgeAttrs(name, Assesses).map(_.tgtName)
        do result.updateWith(content.edgePath)(o => Some(o.cata(_ + competency, Set(competency))))
      loop(content.name)
    }
    result.toMap
  end getAssessedCompetencies

  def getDirectlyAssessedCompetencies(
    ws: AttachedReadWorkspace,
    assets: Seq[Asset[?]],
  ): Map[UUID, Seq[Competency]] =
    val allCompetencies = getCompetencyForest(ws).flatMap(_.flatten)
    val result          = mutable.Map.empty[UUID, Set[UUID]]
    assets foreach { asset =>
      for competency <- ws.outEdgeAttrs(asset.info.name, Assesses).map(_.tgtName)
      do result.updateWith(asset.info.name)(o => Some(o.cata(_ + competency, Set(competency))))
    }
    // The tests require the competencies be ordered so this.
    result.view.mapValues(assessed => allCompetencies.filter(c => assessed.contains(c.nodeName))).toMap
  end getDirectlyAssessedCompetencies

  final val AlignmentGroups  = Seq(Assesses, Teaches)
  final val CompetencyGroups = Set[Group](Level1Competencies, Level2Competencies, Level3Competencies)
  final val DescendantGroups = Set[Group](Questions, CblRubric, Criteria) // no Elements
end CompetentCompetencyService
