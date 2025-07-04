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

package loi.asset.free

import cats.free.Free
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobRef
import loi.authoring.edge.Group.Elements
import loi.authoring.edge.{AssetEdge, Group}
import loi.cp.asset.edge.EdgeData

/** Represents a sequence of steps to build a graph of assets, containing the asset definitions and the edges that
  * connect them.
  * @tparam A
  *   the type of result of this series of steps
  */
sealed trait AssetGraphInstruction[A]

/** An instruction to add an asset of type [[A]] with metadata [[data]]
  * @param data
  *   The metadata to create this asset with
  * @tparam A
  *   The type of asset to create
  */
case class AddAsset[A](data: A)(implicit val assetType: AssetType[A], t: Titleable[A])
    extends AssetGraphInstruction[Asset[A]]:

  def title: String = t.title(data)

/** An instruction to create an edge connecting a source asset with a target asset
  * @param s
  *   the source asset
  * @param t
  *   the target asset
  * @param g
  *   the group to associate this edge with
  */
case class AddEdge[S, T](
  s: Asset[S],
  t: Asset[T],
  g: Group,
  edgeData: EdgeData = EdgeData.empty,
  traverse: Boolean
) extends AssetGraphInstruction[AssetEdge[S, T]]:
  def sourceType: AssetType[S] = s.assetType
  def targetType: AssetType[T] = t.assetType

final case class PutBlob(
  filename: String,
  mediaType: String,
  content: String
) extends AssetGraphInstruction[BlobRef];

object AssetGraphInstruction:

  implicit class AssetOps[A](asset: Asset[A]):
    def --(g: Group): Association[A] = Association(asset, g)

  case class Association[A](source: Asset[A], g: Group):
    def ->[B](target: Asset[B]): AssetGraphInstructionProgram[AssetEdge[A, B]] =
      associate(source, target, g)

  /** A sequence of AssetGraphInstructions, to be interpreted later into something runnable that will actually create
    * them.
    */
  type AssetGraphInstructionProgram[A] = Free[AssetGraphInstruction, A]

  implicit class AssetGraphProgramAssetOps[A](a: AssetGraphInstructionProgram[Asset[A]]):

    /** combines this assetgraph program with an AddEdge instruction, adding this asset to the target asset's elements
      * @return
      *   the original element
      */
    infix def to[S](source: Asset[S]): AssetGraphInstructionProgram[Asset[A]] =
      a.flatMap(target => associate(source, target, Elements)).map(_.target)

    infix def to[S](t: (Asset[S], Group)): AssetGraphInstructionProgram[AssetEdge[S, A]] =
      a.flatMap(target => associate(t._1, target, t._2))

    infix def to[S](t: (Asset[S], Group, Boolean)): AssetGraphInstructionProgram[AssetEdge[S, A]] =
      a.flatMap(target => associate(t._1, target, t._2, traverse = t._3))

    infix def to[S](
      source: Asset[S],
      group: Group,
      edgeData: EdgeData
    ): AssetGraphInstructionProgram[AssetEdge[S, A]] =
      a.flatMap(target => associate(source, target, group, edgeData))
  end AssetGraphProgramAssetOps

  /** For chaining edge connections
    */
  implicit class AssetGraphProgramEdgeOps[S, A](a: AssetGraphInstructionProgram[AssetEdge[S, A]]):
    // TODO why do I need empty params here
    def target(): AssetGraphInstructionProgram[Asset[A]] =
      a.map(_.target)
    def source(): AssetGraphInstructionProgram[Asset[S]] =
      a.map(_.source)
    def andTo[F](
      source: Asset[F],
      group: Group,
      traverse: Boolean = true
    ): AssetGraphInstructionProgram[AssetEdge[F, A]] =
      target().flatMap(t => associate(source, t, group, traverse = traverse))
  end AssetGraphProgramEdgeOps

  /** Add am edge from source asset s to target asset t in group g
    */
  def associate[S, T](
    s: Asset[S],
    t: Asset[T],
    g: Group,
    e: EdgeData = EdgeData.empty,
    traverse: Boolean = true,
  ): AssetGraphInstructionProgram[AssetEdge[S, T]] =
    Free.liftF[AssetGraphInstruction, AssetEdge[S, T]](AddEdge(s, t, g, e, traverse))

  /** Add an asset of type [[A]] with metadata data
    */
  def add[A: AssetType: Titleable](data: A): AssetGraphInstructionProgram[Asset[A]] =
    Free.liftF[AssetGraphInstruction, Asset[A]](AddAsset[A](data))

  def putBlob(filename: String, mediaType: String, content: String): AssetGraphInstructionProgram[BlobRef] =
    Free.liftF[AssetGraphInstruction, BlobRef](PutBlob(filename, mediaType, content))
end AssetGraphInstruction
