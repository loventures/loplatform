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

package loi.authoring.write.web

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils.getFinatraMapper
import loi.authoring.AssetType
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.WriteWorkspace
import loi.authoring.write.*
import loi.authoring.write.web.WriteRequestValidationError.*
import scalaz.std.list.listInstance
import scalaz.syntax.apply.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.syntax.validation.*
import scalaz.{NonEmptyList, ValidationNel, \/}
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.collection.View
import scala.util.Try

@Service
class BaseWriteRequestValidationService(
  nodeService: AssetNodeService,
) extends WriteRequestValidationService:

  def validate(
    reqs: Seq[WriteRequest],
    ws: WriteWorkspace
  ): NonEmptyList[WriteRequestValidationError] \/ WriteValidationBundle =
    val requests = WriteRequests.from(reqs)
    import scalaz.Validation.FlatMap.*

    val validation = for
      _        <- validateNoDuplicateNamesAmongAddRequests(requests)
      names    <- validateNamesAsUuidsOrFromAddRequests(requests, ws)
      writeOps <- deserialize(requests, names, ws)
    yield WriteValidationBundle(
      writeOps,
      NewRequestNamesByUuid(
        names.newNodeNamesByRequestName.map(_.swap),
        names.newEdgeNamesByRequestName.map(_.swap)
      )
    )
    validation.toDisjunction
  end validate

  /** Fails if there is more than one Add* request with the same name. Examples: #1. Seq(AddNode(name = '123'...),
    * AddEdge(name = '123'...)) #2 Seq(AddNode(name = '123'...), AddNode(name = '123'...))
    */
  private def validateNoDuplicateNamesAmongAddRequests(
    requests: WriteRequests
  ): ValidationNel[WriteRequestValidationError, Unit] =
    val addNames       = requests.addNodes.view.map(_.name).concat(requests.addEdges.view.map(_.name))
    val uniqueNewNames = addNames.distinct
    (uniqueNewNames.size != addNames.length)
      .thenInvalidNel(DuplicateNameAmongAddRequests(addNames.length, uniqueNewNames.size))

  /** Converts WriteRequests to ValidatedWriteRequests and maps request names with uuids.
    *
    * Fails if a WriteRequest name cannot be parsed into a valid UUID and is not part of an Add* request.
    */
  private def validateNamesAsUuidsOrFromAddRequests(
    requests: WriteRequests,
    ws: WriteWorkspace,
  ): ValidationNel[WriteRequestValidationError, UuidsAndRequestNames] =
    def parseOrGenerateUUID(name: String): UUID = Try(UUID.fromString(name)).getOrElse(UUID.randomUUID())

    val mustAlreadyExistEdgeRequestNames =
      requests.deleteEdges.view
        .map(_.name.toString)
        .concat(requests.setEdgeDatas.view.map(_.name.toString))
        .concat(requests.setEdgeOrders.view.flatMap(_.ordering))
        .concat(
          requests.addEdges.view
            .flatMap(_.position)
            .collect({
              case WriteRequestPosition.Before(e) => e
              case WriteRequestPosition.After(e)  => e
            })
        )

    val mustAlreadyExistNodeRequestNames =
      requests.setEdgeOrders.view
        .map(_.sourceName)
        .concat(requests.addEdges.view.flatMap(r => View(r.sourceName, r.targetName)))
        .concat(requests.setNodeDatas.view.map(_.name.toString))

    val remoteTargetNames = requests.addEdges.view.filter(_.remote.isDefined).map(_.targetName).toSet

    def isKnownNodeName(name: UUID): Boolean = remoteTargetNames.contains(name.toString) || ws.knowsNode(name)
    def isKnownEdgeName(name: UUID): Boolean = ws.knowsEdge(name)

    ^^^(
      generateNames(
        requests.addNodes.view.map(op => op.name),
        isKnownNodeName,
        NodeNameAlreadyInWorkspace.apply,
      ),
      generateNames(
        requests.addEdges.view.map(op => op.name),
        isKnownEdgeName,
        NodeNameAlreadyInWorkspace.apply,
      ),
      parseNames(
        mustAlreadyExistNodeRequestNames,
        requests.addNodes.view.map(_.name).toSet,
        isKnownNodeName,
        NoSuchNodeInWorkspaceOrAddNodeRequest.apply
      ),
      parseNames(
        mustAlreadyExistEdgeRequestNames,
        requests.addEdges.view.map(_.name).toSet,
        ws.getEdgeId(_).isDefined,
        NoSuchEdgeInWorkspaceOrAddEdgeRequest.apply
      ),
    )(
      (
        newNodeNamesByRequestName,
        newEdgeNamesByRequestName,
        mustAlreadyExistNodeNamesByRequestName,
        mustAlreadyExistEdgeNamesByRequestName,
      ) =>
        new UuidsAndRequestNames(
          newNodeNamesByRequestName,
          newEdgeNamesByRequestName,
          mustAlreadyExistNodeNamesByRequestName,
          mustAlreadyExistEdgeNamesByRequestName
        )
    )
  end validateNamesAsUuidsOrFromAddRequests

  private def validateTypeId(add: AddNodeRequest): ValidationNel[WriteRequestValidationError, AssetType[?]] =
    AssetType.types.get(add.typeId).elseInvalidNel(NoSuchType(add.typeId))

  /** For each collection of request names, check if node/edge name is part of related Add* request names. If so,
    * generate a UUID.
    */
  private def parseNames(
    allNames: View[String],
    addNames: Set[String],
    isInWorkspace: UUID => Boolean,
    errorMsg: String => WriteRequestValidationError
  ): ValidationNel[WriteRequestValidationError, Map[String, UUID]] =

    def parse(n: String): ValidationNel[WriteRequestValidationError, (String, UUID)] =
      Try(UUID.fromString(n)).toOption.filter(isInWorkspace).map(uuid => n -> uuid).elseInvalidNel(errorMsg(n))

    allNames
      .filterNot(addNames.contains)
      .toList
      .traverse(parse)
      .map(_.toMap)
  end parseNames

  private def generateNames(
    names: View[String],
    isInWorkspace: UUID => Boolean,
    errorMsg: String => WriteRequestValidationError
  ): ValidationNel[WriteRequestValidationError, Map[String, UUID]] =

    def parse(n: String): ValidationNel[WriteRequestValidationError, (String, UUID)] =
      val uuid = Try(UUID.fromString(n)).getOrElse(UUID.randomUUID)
      !isInWorkspace(uuid) either (n -> uuid) `orInvalidNel` errorMsg(n)

    names.toList
      .traverse(parse)
      .map(_.toMap)
  end generateNames

  /** Deserializes the JSON documents of the AddNode/SetNodeData requests.
    */
  private def deserialize(
    requests: WriteRequests,
    names: UuidsAndRequestNames,
    ws: WriteWorkspace
  ): ValidationNel[WriteRequestValidationError, List[WriteOp]] =

    val sndAssetTypes =
      nodeService.load(ws).byName(requests.setNodeDatas.map(_.name)).get.map(a => a.info.name -> a.assetType).toMap

    requests.all.toList.traverse[ValidationNel[WriteRequestValidationError, *], WriteOp] {
      case r: AddNodeRequest      => decodeAddNode(names)(r)
      case r: SetNodeDataRequest  => decodeSetNodeData(sndAssetTypes)(r)
      case r: AddEdgeRequest      => toAddEdge(names, r).successNel
      case r: SetEdgeDataRequest  => toSetEdgeData(names, r).successNel
      case r: DeleteEdgeRequest   => DeleteEdge(r.name).asInstanceOf[WriteOp].successNel
      case r: SetEdgeOrderRequest => toSetEdgeOrder(names, r).successNel
    }
  end deserialize

  private def deser[A](json: JsonNode)(
    assetType: AssetType[A]
  ): ValidationNel[WriteRequestValidationError, A] =
    \/.attempt(getFinatraMapper.treeToValue(json, assetType.dataClass))(err => DeserFailure(err.getMessage))
      .toValidationNel[WriteRequestValidationError]

  private def decodeAddNode(
    names: UuidsAndRequestNames
  )(request: AddNodeRequest): ValidationNel[WriteRequestValidationError, WriteOp] =
    validateTypeId(request).andThen({ case assetType: AssetType[a] =>
      deser(request.data)(assetType).map(data =>
        val name = names.forNode(request.name)
        AddNode(assetType)(data, name)
      )
    })

  private def decodeSetNodeData(
    assetTypes: Map[UUID, AssetType[?]]
  )(request: SetNodeDataRequest): ValidationNel[WriteRequestValidationError, WriteOp] =
    val assetType = assetTypes(request.name)
    assetType match
      case assetType: AssetType[a] =>
        deser(request.data)(assetType).map(data => SetNodeData(assetType)(data, request.name))

  private def toAddEdge(names: UuidsAndRequestNames, r: AddEdgeRequest): WriteOp = AddEdge(
    names.forNode(r.sourceName),
    names.forNode(r.targetName),
    r.group,
    names.forEdge(r.name),
    r.position.map({
      case WriteRequestPosition.Start     => Position.Start
      case WriteRequestPosition.End       => Position.End
      case WriteRequestPosition.Before(e) => Position.Before(names.forEdge(e))
      case WriteRequestPosition.After(e)  => Position.After(names.forEdge(e))
    }),
    r.traverse.isTrue,
    r.data,
    r.edgeId | UUID.randomUUID(),
    r.remote,
  )

  private def toSetEdgeData(names: UuidsAndRequestNames, r: SetEdgeDataRequest): WriteOp =
    SetEdgeData(r.name, r.data)

  private def toSetEdgeOrder(names: UuidsAndRequestNames, r: SetEdgeOrderRequest): WriteOp =
    SetEdgeOrder(names.forNode(r.sourceName), r.group, r.ordering.map(names.forEdge))
end BaseWriteRequestValidationService

private class UuidsAndRequestNames(
  val newNodeNamesByRequestName: Map[String, UUID],
  val newEdgeNamesByRequestName: Map[String, UUID],
  mustAlreadyExistNodeNamesByRequestName: Map[String, UUID],
  mustAlreadyExistEdgeNamesByRequestName: Map[String, UUID]
):
  private val nodeUuidsByRequestName: Map[String, UUID] =
    mustAlreadyExistNodeNamesByRequestName ++ newNodeNamesByRequestName
  private val edgeUuidsByRequestName: Map[String, UUID] =
    mustAlreadyExistEdgeNamesByRequestName ++ newEdgeNamesByRequestName

  def forNode(requestName: String): UUID = nodeUuidsByRequestName(requestName)
  def forEdge(requestName: String): UUID = edgeUuidsByRequestName(requestName)
end UuidsAndRequestNames
