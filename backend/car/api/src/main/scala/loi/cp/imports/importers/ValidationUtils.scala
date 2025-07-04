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

package loi.cp.imports
package importers

import loi.cp.imports.errors.{FieldViolation, ItemViolation, Violation}
import scalaz.syntax.nel.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{NonEmptyList, \/}
import scaloi.syntax.BooleanOps.*

import java.util.UUID

object ValidationUtils:

  /** Helper method that validates that only one optional nel is a success with a value. But only when toggle is true!
    */
  def exactlyOneWhen[T](_type: String, precondition: Boolean)(vals: Seq[ViolationNel[Option[T]]]): ViolationNel[Unit] =
    precondition.flatOption(exactlyOne(_type, vals)).toFailureNel(())

  /** Helper method that validates that at most one optional nel is a success with a value. But only when toggle is
    * true!
    */
  def atMostOneWhen[T](_type: String, precondition: Boolean)(vals: Seq[ViolationNel[Option[T]]]): ViolationNel[Unit] =
    precondition.flatOption(atMostOne(_type, vals)).toFailureNel(())

  /** Helper method that validates that only one optional nel is a success with a value.
    */
  def exactlyOne[T](_type: String, vals: Seq[ViolationNel[Option[T]]]): Option[Violation] =
    countDefinedSuccesses(vals) match
      case 1 => None
      case 0 => Some(missingIdentifierError(_type))
      case _ => Some(multipleIdentifierError(_type))

  def validateNumberField[N <: AnyVal](field: String, parse: String => N)(
    disj: NonEmptyList[Violation] \/ String
  ): NonEmptyList[Violation] \/ N =
    disj.flatMap { idStr =>
      \/.attempt(parse(idStr))(_ => FieldViolation(field, idStr, "expected a number").wrapNel)
    }

  def validateUUIDField(field: String)(disj: NonEmptyList[Violation] \/ String): NonEmptyList[Violation] \/ UUID =
    disj.flatMap { idStr =>
      \/.attempt(UUID `fromString` idStr)(_ => FieldViolation(field, idStr, "expected a UUID").wrapNel)
    }

  /** Helper method that validates that at most one optional nel is a success with a value.
    */
  def atMostOne[T](_type: String, vals: Seq[ViolationNel[Option[T]]]): Option[Violation] =
    (countDefinedSuccesses(vals) > 1).option(multipleIdentifierError(_type))

  /** Count the number of defined successes. */
  def countDefinedSuccesses[T](vals: Seq[ViolationNel[Option[T]]]): Int =
    vals.count(_.exists(_.isDefined))

  /** A missing identifier violation. */
  def missingIdentifierError(_type: String): Violation =
    ItemViolation(s"Missing identifier for ${_type}")

  /** A multiple identifier violation. */
  def multipleIdentifierError(_type: String): Violation =
    ItemViolation(s"Found more than one identifier for ${_type}")
end ValidationUtils
