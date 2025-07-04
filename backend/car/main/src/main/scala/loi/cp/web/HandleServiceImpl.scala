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

package loi.cp.web

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.NumberUtils
import org.apache.commons.codec.digest.DigestUtils
import scalaz.syntax.std.boolean.*
import scaloi.syntax.AnyOps.*

import java.math.BigInteger
import java.nio.ByteBuffer
import scala.util.Try

/** PK masking service based on SHA-1 digests and a "secret" key.
  */
@Service
class HandleServiceImpl(
  domain: => DomainDTO,
  sm: ServiceMeta
) extends HandleService:

  import HandleServiceImpl.*

  override def maskId(id: Long): String = combine(id, hash(id))

  override def unmask(mid: String): Option[Long] =
    Try {
      val (id, sha1) = deinterleave(NumberUtils.fromBase36Encoding(mid))
      (sha1 == hash(id)).option(id)
    }.toOption.flatten

  /** Hash a PK, returning the leading 64 bits. */
  def hash(id: Long): Long = ByteBuffer.wrap(DigestUtils.sha1(s"$id:$key")).getLong

  /** The domain masking key. */
  def key: String = s"${sm.getCluster}:${domain.domainId}"
end HandleServiceImpl

object HandleServiceImpl:

  /** Combine a PK and a hash into a string. */
  def combine(id: Long, hash: Long): String = NumberUtils.toBase36Encoding(interleave(id, hash))

  /** Interleave the bits of two longs into a big integer. */
  def interleave(x: Long, y: Long): BigInteger =
    val hi = spread(x >>> 32) | (spread(y >>> 32) << 1)
    val lo = spread(x) | (spread(y) << 1)
    new BigInteger(1, ByteBuffer.allocate(16).putLong(hi).putLong(lo).array)

  /** Spread the bottom 32 bits of a number out to 64 bits with a blank bit between each. */
  def spread(x: Long): Long =
    val x0 = x & Mask0
    val x1 = (x0 | (x0 << 16)) & Mask1
    val x2 = (x1 | (x1 << 8)) & Mask2
    val x3 = (x2 | (x2 << 4)) & Mask3
    val x4 = (x3 | (x3 << 2)) & Mask4
    (x4 | (x4 << 1)) & Mask5

  /** Deinterleave the bits of a big integer into two longs. */
  def deinterleave(x: BigInteger): (Long, Long) =
    val bytes      = x.toByteArray
    val bb         =
      if bytes.length >= 16 then ByteBuffer.wrap(bytes, bytes.length - 16, 16)
      else ByteBuffer.allocate(16).put(Array.fill(16 - bytes.length)(0: Byte)).put(bytes) <| { _.flip() }
    val (hi0, lo0) = split(bb.getLong)
    val (hi1, lo1) = split(bb.getLong)
    join(hi0, hi1) -> join(lo0, lo1)

  /** Split the bits of a long into to 32-bit values. */
  def split(x: Long): (Long, Long) = unspread(x) -> unspread(x >>> 1)

  /** Combine two 32-bit values into a long. */
  def join(x: Long, y: Long): Long = (x << 32) | y

  /** Unspread the odd bits of a 64-bit number into the low 32 bits. */
  def unspread(x: Long): Long =
    val x0 = x & Mask5
    val x1 = (x0 | (x0 >>> 1)) & Mask4
    val x2 = (x1 | (x1 >>> 2)) & Mask3
    val x3 = (x2 | (x2 >>> 4)) & Mask2
    val x4 = (x3 | (x3 >>> 8)) & Mask1
    (x4 | (x4 >>> 16)) & Mask0

  /** Progressive bit-interleaving masks. */
  final val Mask0 = 0x00000000ffffffffL
  final val Mask1 = 0x0000ffff0000ffffL
  final val Mask2 = 0x00ff00ff00ff00ffL
  final val Mask3 = 0x0f0f0f0f0f0f0f0fL
  final val Mask4 = 0x3333333333333333L
  final val Mask5 = 0x5555555555555555L
end HandleServiceImpl
