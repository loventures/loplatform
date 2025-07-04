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

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonMappingException, JsonNode, SerializerProvider}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.write.*
import loi.cp.asset.edge.EdgeData

import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "op")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[AddEdgeRequest], name = "addEdge"),
    new JsonSubTypes.Type(value = classOf[DeleteEdgeRequest], name = "deleteEdge"),
    new JsonSubTypes.Type(value = classOf[SetEdgeDataRequest], name = "setEdgeData"),
    new JsonSubTypes.Type(value = classOf[SetEdgeOrderRequest], name = "setEdgeOrder"),
    new JsonSubTypes.Type(value = classOf[SetNodeDataRequest], name = "setNodeData"),
    new JsonSubTypes.Type(value = classOf[AddNodeRequest], name = "addNode"),
  )
)
sealed trait WriteRequest

case class AddEdgeRequest(
  name: String,
  sourceName: String,
  targetName: String,
  group: Group,
  position: Option[WriteRequestPosition] = None,
  traverse: Option[Boolean] = Some(true),
  @JsonDeserialize(contentAs = classOf[JLong])
  remote: Option[Long] = None,
  data: EdgeData = EdgeData.empty,
  edgeId: Option[UUID] = None,
) extends WriteRequest

case class DeleteEdgeRequest(
  name: UUID,
) extends WriteRequest

case class SetEdgeDataRequest(
  name: UUID,
  data: EdgeData = EdgeData.empty
) extends WriteRequest

case class SetEdgeOrderRequest(
  sourceName: String,
  group: Group,
  ordering: Seq[String]
) extends WriteRequest

case class SetNodeDataRequest(
  name: UUID,
  data: JsonNode
) extends WriteRequest

case class AddNodeRequest(
  name: String,
  typeId: AssetTypeId,
  data: JsonNode
) extends WriteRequest

@JsonDeserialize(`using` = classOf[WriteRequestPositionDeserializer])
@JsonSerialize(`using` = classOf[WriteRequestPositionSerializer])
private[write] sealed trait WriteRequestPosition

object WriteRequestPosition:

  case object Start                   extends WriteRequestPosition
  case object End                     extends WriteRequestPosition
  case class Before(edgeName: String) extends WriteRequestPosition
  case class After(edgeName: String)  extends WriteRequestPosition

  def from(pos: Position): WriteRequestPosition = pos match
    case Position.Start        => Start
    case Position.End          => End
    case Position.Before(name) => Before(name.toString)
    case Position.After(name)  => After(name.toString)
end WriteRequestPosition

/** deserializes 4 shapes to WriteRequestPosition:
  *   - "start"
  *   - "end"
  *   - { "before": "1234" }
  *   - { "after": "1234" }
  */
private[write] class WriteRequestPositionDeserializer
    extends StdDeserializer[WriteRequestPosition](classOf[WriteRequestPosition]):

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): WriteRequestPosition =
    val node   = p.readValueAsTree[JsonNode]()
    val text   = Option(node.textValue())
    val before = Option(node.path("before").textValue())
    val after  = Option(node.path("after").textValue())

    (text, before, after) match
      case (Some("start"), None, None) => WriteRequestPosition.Start
      case (Some("end"), None, None)   => WriteRequestPosition.End
      case (None, Some(anchor), None)  => WriteRequestPosition.Before(anchor)
      case (None, None, Some(anchor))  => WriteRequestPosition.After(anchor)
      case _                           => throw JsonMappingException.from(p, "unknown WriteRequestPosition shape")
  end deserialize
end WriteRequestPositionDeserializer

private[write] class WriteRequestPositionSerializer
    extends StdSerializer[WriteRequestPosition](classOf[WriteRequestPosition]):

  override def serialize(pos: WriteRequestPosition, gen: JsonGenerator, prov: SerializerProvider): Unit = pos match
    case WriteRequestPosition.Start        => gen.writeString("start")
    case WriteRequestPosition.End          => gen.writeString("end")
    case WriteRequestPosition.After(name)  => gen.writeObject(Map("after" -> name))
    case WriteRequestPosition.Before(name) => gen.writeObject(Map("before" -> name))

private[write] case class WriteRequests private (
  all: Seq[WriteRequest],
  addNodes: Seq[AddNodeRequest],
  setNodeDatas: Seq[SetNodeDataRequest],
  addEdges: Seq[AddEdgeRequest],
  setEdgeDatas: Seq[SetEdgeDataRequest],
  setEdgeOrders: Seq[SetEdgeOrderRequest],
  deleteEdges: Seq[DeleteEdgeRequest],
)

object WriteRequest:
  private val mapper = JacksonUtils.getFinatraMapper

  def from(op: WriteOp): WriteRequest =
    op match
      case a: AddNode[?]     =>
        AddNodeRequest(
          name = a.name.toString,
          typeId = a.assetType.id,
          data = mapper.convertValue(a.data, classOf[JsonNode])
        )
      case e: AddEdge        =>
        AddEdgeRequest(
          sourceName = e.sourceName.toString,
          targetName = e.targetName.toString,
          group = e.group,
          name = e.name.toString,
          traverse = Some(e.traverse),
          data = e.data,
          position = e.position.map(WriteRequestPosition.from)
        )
      case s: SetEdgeOrder   =>
        SetEdgeOrderRequest(
          sourceName = s.sourceName.toString,
          group = s.group,
          ordering = s.ordering.map(_.toString)
        )
      case d: SetNodeData[?] =>
        SetNodeDataRequest(
          name = d.name,
          data = mapper.convertValue(d.data, classOf[JsonNode])
        )
      // add to this as you need it
      case _                 =>
        throw new RuntimeException(
          s"conversion of $op is not currently supported"
        )
end WriteRequest

object WriteRequests:
  def from(requests: Seq[WriteRequest]): WriteRequests =

    val addNodes      = List.newBuilder[AddNodeRequest]
    val setNodeDatas  = List.newBuilder[SetNodeDataRequest]
    val deleteEdges   = List.newBuilder[DeleteEdgeRequest]
    val addEdges      = List.newBuilder[AddEdgeRequest]
    val setEdgeOrders = List.newBuilder[SetEdgeOrderRequest]
    val setEdgeDatas  = List.newBuilder[SetEdgeDataRequest]

    requests foreach {
      case request: AddNodeRequest      => addNodes.addOne(request)
      case request: SetNodeDataRequest  => setNodeDatas.addOne(request)
      case request: DeleteEdgeRequest   => deleteEdges.addOne(request)
      case request: AddEdgeRequest      => addEdges.addOne(request)
      case request: SetEdgeOrderRequest => setEdgeOrders.addOne(request)
      case request: SetEdgeDataRequest  => setEdgeDatas.addOne(request)
    }

    WriteRequests(
      requests,
      addNodes.result(),
      setNodeDatas.result(),
      addEdges.result(),
      setEdgeDatas.result(),
      setEdgeOrders.result(),
      deleteEdges.result()
    )
  end from
end WriteRequests
