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

package loi.deploy

import java.io.InputStream
import java.net.URL

import scala.util.Using

import scala.annotation.tailrec

/** Utilities for quick checksums. */
object Checksum:

  /** Checksum a sequence of integers. */
  def checksumChecksums(checksum: Iterable[Int]): Long =
    checksum.foldLeft(0L)(_ * 31 + _)

  /** Checksum the bytecodes of a class. */
  def checksumClass(cls: Class[?]): Int =
    Using.resource(classFile(cls)) { in =>
      checksumStream(in)
    }

  /** Checksum the contents of an URL. */
  def checksumURL(url: URL): Int =
    Using.resource(url.openStream()) { in =>
      checksumStream(in)
    }

  /** Checksum a stream of bytes. */
  @tailrec def checksumStream(in: InputStream, array: Array[Byte] = new Array(1024), cksum: Int = 0): Int =
    val n = in.read(array)
    if n <= 0 then cksum else checksumStream(in, array, checksumArray(array, n, cksum))

  @tailrec private def checksumArray(array: Array[Byte], length: Int, cksum: Int, index: Int = 0): Int =
    if index >= length then cksum else checksumArray(array, length, 31 * cksum + array(index), index + 1)

  private def classFile(cls: Class[?]): InputStream =
    Option(cls.getClassLoader.getResourceAsStream(classFileName(cls)))
      .getOrElse(throw new RuntimeException(s"Error finding classfile: ${cls.getName}"))

  private def classFileName(cls: Class[?]): String =
    cls.getName.replace('.', '/').concat(".class")
end Checksum
