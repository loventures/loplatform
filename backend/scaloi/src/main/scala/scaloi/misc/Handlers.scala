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
package misc

import scala.reflect.ClassTag

object Handlers:

  /** Construct a `PartialFunction` which is defined only on values of type `E`, for which it returns `()`.
    *
    * Use for ignoring exceptions, such as
    * ```
    * try foo()
    * catch ignoring [SpuriousException]
    * ```
    */
  def ignoring[E <: Throwable: ClassTag]: PartialFunction[Throwable, Unit] = { case _: E =>
  }
end Handlers
