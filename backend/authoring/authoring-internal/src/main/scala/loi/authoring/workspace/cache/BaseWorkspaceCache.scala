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

package loi.authoring.workspace
package cache

import org.apache.pekko.util.{ByteIterator, ByteString, ByteStringBuilder}
import com.learningobjects.cpxp.component.annotation.{Instrument, PostLoad, Service}
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.redis.serialization.{Format, Parse}
import loi.asset.platform.cache.{ValkeyCodec, ValkeyLoadingCache}
import loi.authoring.edge.Group
import loi.authoring.workspace.cache.BaseWorkspaceCache.{DeserializeMetric, SerializeMetric}
import scaloi.syntax.boolean.*

import java.nio.ByteOrder
import java.util.UUID

@Service
@Instrument
class BaseWorkspaceCache(
  cache: ValkeyLoadingCache[LocalWorkspaceData],
  config: com.typesafe.config.Config,
) extends WorkspaceCache:
  import BaseWorkspaceCache.*

  override def get(commitId: Long): Option[LocalWorkspaceData] =
    cache.get(commitId.toString)

  override def getOrLoad(commitId: Long, load: () => LocalWorkspaceData): LocalWorkspaceData =
    cache.getOrLoad(commitId.toString, load)

  override def put(commitId: Long, workspace: LocalWorkspaceData): Boolean =
    cache.put(commitId.toString, workspace) <|! log.warn("failed to cache workspace in redis")

  def flushDb(): Boolean =
    cache.flushDb()

  def delete(commitId: Long): Unit = cache.delete(commitId.toString)

  @PostLoad private[cache] def init(): Unit =
    validateSerializedWorkspaces = config.getBoolean("valkey.validateSerializedWorkspaces")

  private[authoring] def enableValidation(): Unit =
    validateSerializedWorkspaces = true
end BaseWorkspaceCache

object BaseWorkspaceCache:
  final val SerializeMetric   = "Custom/Authoring/serializeWorkspace"
  final val DeserializeMetric = "Custom/Authoring/deserializeWorkspace"

  private[cache] var validateSerializedWorkspaces = false

  implicit val localWorkspaceRedisCodec: ValkeyCodec[LocalWorkspaceData] =
    new ValkeyCodec[LocalWorkspaceData]:

      override val format: Format = new Format({ case a: LocalWorkspaceData =>
        LocalWorkspaceCodec.serialize(a).toArrayUnsafe()
      })

      override val parse: Parse[LocalWorkspaceData] =
        new Parse(bytes => LocalWorkspaceCodec.deserialize(ByteString.fromArrayUnsafe(bytes)))

  private val log = org.log4s.getLogger
end BaseWorkspaceCache

object LocalWorkspaceCodec:
  import loi.apm.Apm.recordMetric

  implicit val byteOrder: ByteOrder = java.nio.ByteOrder.BIG_ENDIAN

  // ByteArray*Stream and Data*Stream are likely more efficient in this
  // use case than the pekko ByteString classes, but entropy.

  // desired access:
  // private[cache]
  // private[loi.branchy.authoring.workspace.cache]
  def deserialize(bs: ByteString): LocalWorkspaceData =
    val (entry, duration) = Stopwatch.profiled {
      val iter = bs.iterator
      if iter.getInt == -1 then contemporaryDeserialize(iter) else legacyDeserialize(bs.iterator)
    }
    recordMetric(DeserializeMetric, duration.toMillis.toFloat)
    entry

  private def contemporaryDeserialize(iter: ByteIterator): LocalWorkspaceData =
    val nodeIdsSize, edgeInfosSize = iter.getInt

    var nodeIdsByName   = Map.empty[UUID, Long]
    var edgeInfosByName = Map.empty[UUID, EdgeInfo]
    var rootNodeNames   = Set.empty[UUID]

    var i = 0
    while i < nodeIdsSize do
      val nameHi, nameLo, id = iter.getLong
      nodeIdsByName = nodeIdsByName.updated(new UUID(nameHi, nameLo), id)
      i += 1

    var j = 0
    while j < edgeInfosSize do
      val id, nameHi, nameLo, src, tgt, eidHi, eidLo = iter.getLong
      val grp                                        = iter.getInt
      val posLo, traverseWithPosHi                   = iter.getShort

      val info = EdgeInfo(
        id = id,
        name = new UUID(nameHi, nameLo),
        sourceId = src,
        targetId = tgt,
        edgeId = new UUID(eidHi, eidLo),
        group = Group.byTag(grp),
        position = decodePositionFromPositionAndCombinedTraverse(posLo, traverseWithPosHi),
        traverse = decodeTraverseFlagFromShort(traverseWithPosHi),
      )

      edgeInfosByName = edgeInfosByName.updated(info.name, info)
      j += 1
    end while

    while iter.hasNext do
      val nameHi, nameLo = iter.getLong
      rootNodeNames = rootNodeNames + new UUID(nameHi, nameLo)

    LocalWorkspaceData(
      nodeIdsByName = nodeIdsByName,
      edgeInfosByName = edgeInfosByName,
      rootNodeNames = rootNodeNames,
    )
  end contemporaryDeserialize

  private def legacyDeserialize(iter: ByteIterator): LocalWorkspaceData =
    val nodeIdsSize, edgeInfosSize = iter.getInt

    var nodeIdsByName   = Map.empty[UUID, Long]
    var edgeInfosByName = Map.empty[UUID, EdgeInfo]
    var rootNodeNames   = Set.empty[UUID]

    var i = 0
    while i < nodeIdsSize do
      val nameHi, nameLo, id = iter.getLong
      nodeIdsByName = nodeIdsByName.updated(new UUID(nameHi, nameLo), id)
      i += 1

    var j = 0
    while j < edgeInfosSize do
      val id, nameHi, nameLo, src, tgt, eidHi, eidLo = iter.getLong
      val grp                                        = iter.getInt
      val pos, traverseRaw                           = iter.getShort
      val createdRaw, modifiedRaw                    = iter.getLong

      val info = EdgeInfo(
        id = id,
        name = new UUID(nameHi, nameLo),
        sourceId = src,
        targetId = tgt,
        edgeId = new UUID(eidHi, eidLo),
        group = Group.byTag(grp),
        position = pos.toLong,
        traverse = traverseRaw != 0,
      )

      edgeInfosByName = edgeInfosByName.updated(info.name, info)
      j += 1
    end while

    while iter.hasNext do
      val nameHi, nameLo = iter.getLong
      rootNodeNames = rootNodeNames + new UUID(nameHi, nameLo)

    LocalWorkspaceData(
      nodeIdsByName = nodeIdsByName,
      edgeInfosByName = edgeInfosByName,
      rootNodeNames = rootNodeNames,
    )
  end legacyDeserialize

  // desired access:
  // private[cache]
  // private[loi.branchy.authoring.workspace.cache]
  def serialize(data: LocalWorkspaceData): ByteString =
    val (bytestring, duration) = Stopwatch.profiled {
      import data.*

      val sizeData  =
        val builder = new ByteStringBuilder; builder sizeHint 12
        builder.putInt(-1)
        builder.putInt(nodeIdsByName.size)
        builder.putInt(edgeInfosByName.size)
        builder.result()

      val nodesData =
        // 16 for the name, 8 for the PK
        val sz      = nodeIdsByName.size * (16 + 8)
        val builder = new ByteStringBuilder; builder sizeHint sz
        nodeIdsByName.foreach { case (name, id) =>
          builder.putLong(name.getMostSignificantBits)
          builder.putLong(name.getLeastSignificantBits)
          builder.putLong(id)
        }
        builder.result()
      end nodesData
      val edgesData =
        // name, edgeId          = 16 * 2 = 32
        // id, src, tgt          =  8 * 3 = 24
        // group                 =  4 * 1 =  4
        // positionLO            =  2 * 1 =  2   [pos[15]...pos[0]]
        // traverse + positionHI =  2 * 1 =  2   [pos[30]...pos[16] :: traverse]
        //                       ----
        // total                 = 64
        val sz      = edgeInfosByName.size * 64
        val builder = new ByteStringBuilder; builder sizeHint sz
        edgeInfosByName.valuesIterator.foreach { info =>
          import builder.*
          import info.*
          putLong(id)
          putLong(name.getMostSignificantBits)
          putLong(name.getLeastSignificantBits)
          putLong(sourceId)
          putLong(targetId)
          putLong(edgeId.getMostSignificantBits)
          putLong(edgeId.getLeastSignificantBits)
          putInt(group.tag)
          putShort(position.toInt)
          putShort(encodeTraverseFlagAndHighBitsOfPosition(position, traverse))
        }
        builder.result()
      end edgesData
      val rootNodes =
        val sz      = rootNodeNames.size * 16
        val builder = new ByteStringBuilder; builder sizeHint sz
        rootNodeNames.foreach { name =>
          builder.putLong(name.getMostSignificantBits)
          builder.putLong(name.getLeastSignificantBits)
        }
        builder.result()

      sizeData ++ nodesData ++ edgesData ++ rootNodes
    }
    recordMetric(SerializeMetric, duration.toMillis.toFloat)

    if BaseWorkspaceCache.validateSerializedWorkspaces then
      val rt = deserialize(bytestring)
      assert(rt == data, (data, rt))

    bytestring
  end serialize

  // A long time ago we made the clever decision to encode position in 16 bits and traverse in 16 bits.
  // Then "we" decided to space positions apart so that fewer edge rewrites were needed. However, the
  // spaced positions no longer fit in 16 bits. So we now encode the bottom 15 bits of the top 16 bits
  // of the position in the top 15 bits of the encoded traverse boolean, allowing us to make this change
  // without altering the workspace cache encoding and having to deal with some transition plan.
  private def encodeTraverseFlagAndHighBitsOfPosition(position: Long, traverse: Boolean): Int =
    (position.toInt >> 16 << 1) + (if traverse then 1 else 0) // [pos[30]...pos[16] :: traverse]

  // Traverse is the bottom bit of this short
  private def decodeTraverseFlagFromShort(traverseWithPosHi: Short): Boolean =
    (traverseWithPosHi & 1) == 1

  // Position combines the unsigned low-bit short and the top 15 bits of the conjoined position/traverse short
  private def decodePositionFromPositionAndCombinedTraverse(posLo: Short, traverseWithPosHi: Short): Long =
    (posLo.toLong & 65535) + (traverseWithPosHi.toLong >> 1 << 16)
end LocalWorkspaceCodec
