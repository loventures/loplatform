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

package loi.authoring.validate

import cats.data.Validated.Valid
import cats.data.{Validated, ValidatedNel}

object Validate:

  def size(label: String, min: Int = 0, max: Int = Int.MaxValue): Sighs = new Sighs(label, min, max)

  class Sighs(label: String, min: Int, max: Int):
    def apply(value: Int): ValidatedNel[String, Unit] =
      Validated.condNel(value >= min && value <= max, (), s"$label: size [$value] is not between $min and $max")

    def apply(value: String): ValidatedNel[String, Unit] = apply(value.length)

    def apply(value: Option[String]): ValidatedNel[String, Unit] =
      value.map(x => apply(x.length)).getOrElse(Valid(()))

  def min(label: String, min: Long): Minh = new Minh(label, min)

  class Minh(label: String, min: Long):
    def apply(value: Number): ValidatedNel[String, Unit] =
      Validated.condNel(value.longValue >= min, (), s"$label: [$value] is not greater than or equal to $min")

    def apply(value: Option[Long]): ValidatedNel[String, Unit] =
      value.map(x => apply(x)).getOrElse(Valid(()))

  def notEmpty(label: String)(value: String): ValidatedNel[String, Unit] =
    Validated.condNel(value.length > 0, (), s"$label: field is required")
end Validate
