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

package loi.cp

import scalaz.*
import scalaz.Scalaz.*

package object password:

  type Password          = String
  type Error             = String
  type PasswordValidator = Password => ValidationNel[Error, Password]

  /*private[password]*/
  def updateHistory[T](getter: => Iterable[T], setter: List[T] => Unit, historySize: Int)(value: T): List[T] =
    val historyMaybe     = Option(getter)
    val history          = historyMaybe.fold(List.empty[T])(_.toList) :+ value
    val truncatedHistory = history.takeRight(historySize)
    setter(truncatedHistory)
    truncatedHistory

  private[password] def reduceValidation(
    validators: List[Option[PasswordValidator]],
    password: Password
  ): ValidationNel[Error, Password] =
    validators flatMap (_.toList) foldMap (_(password))
  private[password] def reduceValidation(
    validator: Option[PasswordValidator],
    password: Password
  ): ValidationNel[Error, Password] =
    validator.foldMap(_(password))
end password
