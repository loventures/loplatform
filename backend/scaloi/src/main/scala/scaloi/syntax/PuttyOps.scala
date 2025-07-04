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

package scaloi.syntax
import scaloi.putty.{RemoveAll, RemoveOne, Select}

trait PuttyOps:
  extension [T <: Tuple](self: T)
    def select[A](using Sel: Select.Aux[A, T]): A = Sel(self)

    def removeOne[A](using Rm: RemoveOne.Aux[A, T]): (A, RemoveOne[A, T]) = Rm(self)

    def removeAll[A <: Tuple](using Rm: RemoveAll.Aux[A, T]): (A, RemoveAll[A, T]) = Rm(self)

  end extension

end PuttyOps
