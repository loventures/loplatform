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

import java.util.concurrent.locks.Lock

import scala.language.implicitConversions

/** Enhancements on locks.
  *
  * @param self
  *   the lock
  */
final class LockOps(private val self: Lock) extends AnyVal:

  /** Perform a function while holding a lock.
    * @param f
    *   the function
    * @tparam A
    *   the return type
    * @return
    *   the return value
    */
  def locked[A](f: => A): A =
    try
      self.lock()
      f
    finally self.unlock()
end LockOps

/** Implicit conversion for lock operations.
  */
trait ToLockOps:

  /** Implicit conversion from lock to the lock enhancements.
    * @param a
    *   the lock value
    */
  implicit def toLockOps(a: Lock): LockOps = new LockOps(a)
