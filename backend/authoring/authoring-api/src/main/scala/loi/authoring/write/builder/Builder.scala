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

package loi.authoring.write.builder

import cats.data.State
import cats.syntax.option.*
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, Group}
import loi.authoring.write.*
import loi.cp.asset.edge.*
import scalaz.Functor as Zunctor
import scaloi.syntax.FauxnadOps

import java.util.UUID
import scala.language.implicitConversions

/** State constructors that make building a List[WriteOp] easier:
  *
  *   1. The SetEdgeOrder operations are created automatically based on the order of addEdge use 2. The State monad and
  *      the for-comprehension syntax allows access to prior values
  *
  * {{{
  * import loi.authoring.write.builder._
  *
  * val builder = for {
  *   a  <- addNode(Module("a"))
  *   b  <- addNode(Lesson("b"))
  *   ab <- addEdge(a, b)
  * } yield ()
  *
  * val ops: List[WriteOp] = builder.runEmptyS.value.writeOps
  * // ops is four ops: AddNode-A, AddNode-B, AddEdge-A-B, SetEdgeOrder-A-Elements
  * // runEmptyS.value is from cats
  * }}}
  *
  * An arrow syntax also exists
  * {{{
  * import loi.authoring.write.syntax.builder._
  *
  * val builder = for {
  *   (a, b, ab) <- Module("a") ->> Lesson("b")
  *   (_, c, ac) <- a ->> Lesson("c")
  * } yield ()
  *
  * val ops = builder.runEmptyS.value.writeOps
  * // ops is six ops: AddNode-A, AddNode-B, AddEdge-A-B, AddNode-C, AddEdge-A-C, SetEdgeOrder-A-Elements
  * // runEmptyS.value is from cats
  *
  * }}}
  */
trait Builder extends BuilderSyntax:

  /** Append an AddNode operation
    */
  def addNode[A: AssetType](data: A, name: UUID = UUID.randomUUID()): State[List[WriteOp], NodeName[A]] =
    State(ops => (ops :+ AddNode(data, name), new NodeName[A](name)))

  def addNode[A: AssetType](data: A, name: String): WriteOpsState[NodeName[A]] =
    addNode(data, UUID.fromString(name))

  def setNodeData[A: AssetType](node: NodeName[A], data: A): State[List[WriteOp], NodeName[A]] =
    State(ops => (ops :+ SetNodeData(node.name, data), node))

  def setNodeData[A: AssetType](node: Asset[A], data: A): State[List[WriteOp], NodeName[A]] =
    setNodeData(new NodeName[A](node.info.name), data)

  /** Append an AddEdge operation
    * @return
    *   edge representation
    */
  def addEdge[A, B](
    src: NodeName[A],
    tgt: NodeName[B],
    grp: Group = Group.Elements,
    name: UUID = UUID.randomUUID(),
    position: Option[Position] = Position.End.some,
    traverse: Boolean = true,
    data: EdgeData = EdgeData.empty,
    edgeId: UUID = UUID.randomUUID(),
    remote: Option[Long] = None,
  ): State[List[WriteOp], EdgeName[A, B]] =
    State(ops =>
      (
        ops :+ AddEdge(src.name, tgt.name, grp, name, position, traverse, data, edgeId, remote),
        new EdgeName[A, B](name, src, tgt)
      )
    )

  def setEdgeData[A, B](
    edge: EdgeName[A, B],
    data: EdgeData
  ): State[List[WriteOp], EdgeName[A, B]] = State(ops => (ops :+ SetEdgeData(edge.name, data), edge))

  // :shame: but some usages of the builder commit changes instead of building the graph
  // from whole cloth. They need this to accommodate pre-existing edges.
  def setEdgeOrder(src: NodeName[?], grp: Group = Group.Elements)(
    ordering: EdgeName[?, ?]*
  ): State[List[WriteOp], Unit] =
    State(ops => ((ops :+ SetEdgeOrder(src.name, grp, ordering.map(_.name)), ())))

  // :shame: doesn't interact at all with `ordering` you'll have to provide your own
  // setEdgeOrder... when I started this I only meant for it to build new graphs from
  // scratch, not edit them too
  def deleteEdge(edge: EdgeName[?, ?]): State[List[WriteOp], Unit] = State(ops => (ops :+ DeleteEdge(edge.name), ()))

  def setHomeName(name: NodeName[?]): WriteOpsState[Unit] = State.modify(ops => ops :+ SetHomeName(name.name))
  def setRootName(name: NodeName[?]): WriteOpsState[Unit] = State.modify(ops => ops :+ SetRootName(name.name))

  implicit def asset2NodeName[A](asset: Asset[A]): NodeName[A]            = new NodeName(asset.info.name)(using asset.assetType)
  implicit def edge2EdgeName[A, B](edge: AssetEdge[A, B]): EdgeName[A, B] =
    EdgeName(edge.name, edge.source, edge.target)

  implicit def bifauxnadState[A, B](state: State[A, B]): FauxnadOps[State[A, *], B] = new FauxnadOps(state)

  implicit def zunctorState[A]: Zunctor[State[A, *]] = new Zunctor[State[A, *]]:
    override def map[B, C](fb: State[A, B])(f: B => C): State[A, C] = fb.map(f)
end Builder

/** Enough information to load a node from a workspace fully typed
  */
class NodeName[A](val name: UUID)(implicit val assetType: AssetType[A])

/** Enough information to load an edge from a workspace fully typed.
  *
  * Having the source and target names is unrelated to loading an edge fully typed. Their presence allows for functions
  * that combine adding one or both vertices and the edge. Such functions can return the NodeNames of the new vertices
  * in this class.
  */
case class EdgeName[A, B](name: UUID, src: NodeName[A], tgt: NodeName[B])
