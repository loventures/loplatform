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

package loi.cp.asset.edge

import argonaut.EncodeJson
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.json.JacksonCodecs
import scaloi.syntax.classTag.*

import scala.annotation.meta.getter
import scala.reflect.ClassTag

@JsonDeserialize(`using` = classOf[EdgeDataDeserializer])
case class EdgeData(@(JsonValue @getter) node: ObjectNode):
  def get[T](implicit ops: EdgeDataOps[T]): Option[T] =
    ops.fetch(node)

  def set[T](newData: T)(implicit ops: EdgeDataOps[T]): EdgeData =
    copy(node = ops.update(node, newData))

  def +[T](newData: T)(implicit ops: EdgeDataOps[T]): EdgeData = set(newData)

  def extract[A: EdgeDataOps]: Option[EdgeDataOp[A]] = get[A].map(new EdgeDataOp(_))
end EdgeData

object EdgeData:
  def of[A: EdgeDataOps](a: A): EdgeData = empty + a

  val empty = EdgeData(JacksonUtils.objectNode())

  implicit val encodeJsonForEdgeData: EncodeJson[EdgeData] = JacksonCodecs.jsonNodeEnc.contramap(_.node)

class EdgeDataDeserializer extends JsonDeserializer[EdgeData]:
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): EdgeData =
    val node = jp.getCodec.readValue(jp, classOf[ObjectNode])

    val rawEdgeData = EdgeData(node)
    List(
      rawEdgeData.extract[GateEdgeData],
      rawEdgeData.extract[DueDateGateEdgeData],
      rawEdgeData.extract[PerformanceGateEdgeData],
    ).flatten.foldLeft(EdgeData.empty)(_ +: _)
  end deserialize
end EdgeDataDeserializer

final class EdgeDataOp[A](a: A)(implicit ops: EdgeDataOps[A]):
  def +:(data: EdgeData): EdgeData = data.set(a)

class EdgeDataOps[T](key: String)(implicit tTag: ClassTag[T]):
  final def fetch(node: JsonNode): Option[T] =
    if node.has(key) then Option(JacksonUtils.getFinatraMapper.treeToValue(node.path(key), classTagClass[T]))
    else Option.empty

  def update(node: ObjectNode, newData: T): ObjectNode =
    val newNode = JacksonUtils.getFinatraMapper.valueToTree[JsonNode](newData)
    node.deepCopy[ObjectNode]().set[ObjectNode](key, newNode)

case class GateEdgeData(offset: Long = 0L)

object GateEdgeData:
  implicit val gateEdgeDataOps: EdgeDataOps[GateEdgeData] = new EdgeDataOps[GateEdgeData]("gate")

case class DueDateGateEdgeData(
  offset: Long = 0L,
  @JsonDeserialize(contentAs = classOf[java.lang.Long]) dueDateDayOffset: Option[Long] = None
)

object DueDateGateEdgeData:
  implicit val dueDateGateEdgeDataOps: EdgeDataOps[DueDateGateEdgeData] =
    new EdgeDataOps[DueDateGateEdgeData]("dueDateGate")

case class PerformanceGateEdgeData(threshold: Double = 0.8)

object PerformanceGateEdgeData:
  implicit val performanceGateEdgeDataOps: EdgeDataOps[PerformanceGateEdgeData] =
    new EdgeDataOps[PerformanceGateEdgeData]("performanceGate")
