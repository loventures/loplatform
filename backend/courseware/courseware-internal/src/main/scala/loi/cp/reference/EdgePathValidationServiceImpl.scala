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

package loi.cp.reference
import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{EdgeInfo, ReadWorkspace}
import loi.cp.reference.EdgePathValidationError.*
import scalaz.std.option.*
import scalaz.syntax.applicative.*
import scalaz.syntax.std.list.*
import scalaz.{NonEmptyList, Validation, ValidationNel, \/}
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

@Service
class EdgePathValidationServiceImpl(
  nodeService: AssetNodeService
) extends EdgePathValidationService:

  import EdgePathValidationServiceImpl.*

  override def validate(
    ws: ReadWorkspace,
    edgeInfos: List[EdgeInfo]
  ): NonEmptyList[EdgePathValidationError] \/ EdgePath =
    import Validation.FlatMap.*
    val vNodes = for
      edges <- validateNonEmpty(edgeInfos)
      _     <- validateInLogicalOrder(edges)
    yield loadNodes(ws, edgeInfos)

    val vAttempt = vNodes flatMap { nodes =>
      validateHeadIsCourse(nodes.head, edgeInfos.head) *>
        validateLastMayBePlayable(edgeInfos.last, nodes.last) *>
        validateLastIsNotRemediationResource(nodes, edgeInfos.last)
    }

    vAttempt
      .as(EdgePath.apply(edgeInfos.map(_.name)))
      .toDisjunction
  end validate

  private def validateNonEmpty(
    edges: List[EdgeInfo]
  ): ValidationNel[EdgePathValidationError, NonEmptyList[EdgeInfo]] =
    edges.toNel.toOption.elseInvalidNel[EdgePathValidationError](EmptyEdgeList)

  /** Ensure that each edge and its successor have matching source -> target ids.
    *
    * Valid: Seq( EdgeInfo(sourceId: 5, targetId: 6), EdgeInfo(sourceId: 6, targetId: 3) )
    *
    * Invalid: Seq( EdgeInfo(sourceId: 5, targetId: 6), EdgeInfo(sourceId: 3, targetId: 2) )
    */
  private def validateInLogicalOrder(
    edges: NonEmptyList[EdgeInfo]
  ): ValidationNel[EdgePathValidationError, Unit] =

    def traverseEdge(from: Option[Long], edgeInfo: EdgeInfo): Option[Long] =
      from.filter(_ == edgeInfo.sourceId).as(edgeInfo.targetId)

    edges.list
      .foldLeft(Option(edges.head.sourceId))({ case (acc, edgeInfo) =>
        traverseEdge(acc, edgeInfo)
      })
      .elseInvalidNel[EdgePathValidationError](IllogicalOrdrErr(edges.map(_.name)))
      .as(())
  end validateInLogicalOrder

  private def validateHeadIsCourse(
    headNode: Asset[?],
    headEdge: EdgeInfo
  ): ValidationNel[EdgePathValidationError, Unit] =
    (headNode.info.typeId == AssetTypeId.Course)
      .elseInvalidNel[EdgePathValidationError, Unit](
        HeadAssNotCourseErr(headEdge.name, headNode.info.typeId.entryName),
        ()
      )

  /** Some asset types never have a direct edge path, i.e., they're not "playable"
    */
  private def validateLastMayBePlayable(
    lastEdge: EdgeInfo,
    lastNode: Asset[?]
  ): ValidationNel[EdgePathValidationError, Unit] =
    playableLastAssetTypeIds
      .contains(lastNode.info.typeId)
      .elseInvalidNel[EdgePathValidationError, Unit](
        LastAssNotPlayable(lastEdge.name, lastNode.info.typeId.entryName),
        ()
      )

  /** Some asset types can be playable, but if they are a remediation resource as well, they are not! e.g., html.1,
    * resource.1
    */
  private def validateLastIsNotRemediationResource(
    nodes: AssetCol,
    lastEdge: EdgeInfo,
  ): ValidationNel[EdgePathValidationError, Unit] =
    nodes.all
      .filter(_ != nodes.last)
      .forall(node => !remediationResourceParents.contains(node.info.typeId))
      .elseInvalidNel[EdgePathValidationError, Unit](
        LastAssIsRemedtnRsrce(lastEdge.name, nodes.last.info.id),
        ()
      )

  private def loadNodes(
    ws: ReadWorkspace,
    edges: Seq[EdgeInfo]
  ): AssetCol =
    val nodeIds  = edges
      .flatMap(_.vertexIds)
      .distinct
    // `get` should be safe if directly from edgeInfo/ws
    val nodes    = nodeService.load(ws).byId(nodeIds)
    val headNode = nodes.find(_.info.id == edges.head.sourceId).get
    val lastNode = nodes.find(_.info.id == edges.last.targetId).get
    AssetCol(nodes, headNode, lastNode)
  end loadNodes
end EdgePathValidationServiceImpl

private case class AssetCol(
  all: Seq[Asset[?]],
  head: Asset[?],
  last: Asset[?]
)

object EdgePathValidationServiceImpl:
  lazy val playableLastAssetTypeIds: Set[AssetTypeId] = Set(
    AssetTypeId.Assignment,
    AssetTypeId.Assessment,
    AssetTypeId.Checkpoint,
    AssetTypeId.Diagnostic,
    AssetTypeId.Discussion,
    AssetTypeId.FileBundle,
    AssetTypeId.Html,
    AssetTypeId.Scorm,
    AssetTypeId.ObservationAssessment,
    AssetTypeId.CourseLink,
    AssetTypeId.Unit,
    AssetTypeId.Module,
    AssetTypeId.Lesson,
    AssetTypeId.PoolAssessment,
    AssetTypeId.Resource1,
    AssetTypeId.Lti
  )

  lazy val remediationResourceParents: Set[AssetTypeId] = Set(
    AssetTypeId.BinDropQuestion,
    AssetTypeId.EssayQuestion,
    AssetTypeId.FillInTheBlankQuestion,
    AssetTypeId.MatchingQuestion,
    AssetTypeId.MultipleChoiceQuestion,
    AssetTypeId.MultipleSelectQuestion,
    AssetTypeId.OrderingQuestion,
    AssetTypeId.TrueFalseQuestion,
  )
end EdgePathValidationServiceImpl
