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

package scaloi
package syntax

import java.util.concurrent.locks.ReadWriteLock

import scala.language.implicitConversions

/** Enhancements on read-write locks.
  *
  * @param self
  *   the read-write lock
  */
final class ReadWriteLockOps(private val self: ReadWriteLock) extends AnyVal:
  import lock.*

  /** Perform a function while holding the read lock.
    * @param f
    *   the function
    * @tparam A
    *   the return type
    * @return
    *   the return value
    */
  def reading[A](f: => A): A = self.readLock.locked(f)

  /** Perform a function while holding the write lock.
    * @param f
    *   the function
    * @tparam A
    *   the return type
    * @return
    *   the return value
    */
  def writing[A](f: => A): A = self.writeLock.locked(f)
end ReadWriteLockOps

/** Implicit conversion for read-write lock operations.
  */
trait ToReadWriteLockOps:

  /** Implicit conversion from lock to the read-write lock enhancements.
    * @param a
    *   the lock value
    */
  implicit def toReadWriteLockOps(a: ReadWriteLock): ReadWriteLockOps = new ReadWriteLockOps(a)
