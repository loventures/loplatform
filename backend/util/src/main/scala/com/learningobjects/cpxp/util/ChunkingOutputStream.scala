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

package com.learningobjects.cpxp.util

import java.io.*
import java.util.Date

/** An `OutputStream` that progressively fills up `File`s of a given size with the data passed to it, and returns those
  * files via a callback.
  * @param sz
  *   The size of all chunked `File`s, in bytes, except possibly the last one, which will be no bigger than this size.
  * @param seed
  *   An initial value for the accumulator parameter to `handler`
  * @param handler
  *   A callback which will be passed every `File` as it is produced. A parameter of type `R` is threaded through
  *   multiple invocations of this function.
  */
class ChunkingOutputStream[R](sz: Int, seed: R)(handler: (File, R) => R) extends OutputStream:
  private var pos: Int              = 0
  private var dst: File             = createTempFile()
  private var fos: FileOutputStream = new FileOutputStream(dst)

  private var accum: R = seed
  def result           = accum

  override def write(b: Int) =
    fos.write(b)
    pos += 1
    if pos >= sz then split()

  override def write(b: Array[Byte], off: Int, len: Int) =
    if len <= sz - pos then // simple case: it fits within this chunk, so just write it
      fos.write(b, off, len)
      pos += len
      if len == sz - pos then // exact! so split here
        split()
    else                    // hard case: finish this chunk, then fill chunks with the rest of the data
      var current: Int = off
      val end          = current + len

      // fill up this chunk
      val remainder = sz - pos
      fos.write(b, current, remainder)
      current += remainder
      split()

      // now, take `sz`-sized chunks from b[current, current+len]
      while current < end do
        val thisChunkSize = sz min (end - current)
        fos.write(b, current, thisChunkSize)
        if thisChunkSize == sz then split()
        else pos += thisChunkSize
        current += sz

  override def close(): Unit =
    println(s"CLOSE: $pos")
    fos.close()
    if pos > 0 then accum = handler(dst, accum)
    dst.delete()
    ()

  override def flush(): Unit = fos.flush()

  private def split() =
    fos.close()
    accum = handler(dst, accum)
    dst.delete()

    pos = 0
    dst = createTempFile()
    fos = new FileOutputStream(dst)

  private def createTempFile() =
    File.createTempFile(s"ChunkingOutputStream@$hashCode", new Date().getTime.toString)
end ChunkingOutputStream

object ChunkingOutputStream:
  def apply[R](sz: Int, seed: R)(handler: (File, R) => R): ChunkingOutputStream[R] =
    new ChunkingOutputStream[R](sz, seed)(handler)
