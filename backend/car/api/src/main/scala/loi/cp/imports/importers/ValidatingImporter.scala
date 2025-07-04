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

import loi.cp.imports.errors.{FieldViolation, ValidationError}
import scalaz.\/
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*

trait ValidatingImporter[Item <: ImportItem] extends Importer[Item]:
  import Importer.*

  val log: org.log4s.Logger

  override def validate(item: Item): ValidationError \/ Validated[Item] = // such types
    log.debug(s"Validating: $item")
    validateItem(item).bimap(ValidationError.apply(_), mkValidated).toDisjunction

  protected def validateItem(item: Item): ViolationNel[Item]

  protected final def validateNonEmpty(i: Item, field: String, value: Item => String): ViolationNel[Item] =
    (value(i).isEmpty option FieldViolation(field, value(i), s"$field cannot be blank"))
      .toFailureNel(i)
end ValidatingImporter
