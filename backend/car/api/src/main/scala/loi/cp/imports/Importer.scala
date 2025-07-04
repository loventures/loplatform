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

import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.registry.Bound
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.imports.errors.{PersistError, ValidationError}
import scalaz.\/
import scalaz.syntax.either.*

/** An interface which abstracts over logic to import items that are a subtype of [[ImportItem]].
  */
@Bound(value = classOf[ImportBinding])
trait Importer[Item <: ImportItem] extends ComponentInterface:
  import Importer.*

  // todo: maybe the csv-relevant methods (suportsCsv, deserializeCsvRow, requiredHeaders, allHeaders)
  // can be moved to a typeclass?)

  /** Takes Csv headers and a row of values and serializes them into
    */
  def deserializeCsvRow(headers: Seq[String], values: Seq[String]): ValidationError \/ Item

  /** Returns a case-insensitive list of headers that are required in order for this Importer to be matched.
    */
  def requiredHeaders: Set[String]

  /** Returns a case-insensitive list of all headers that are supported by this Importer.
    */
  def allHeaders: Set[String]

  /** Validates an [[ImportItem]]. In the context of an import, this method will be used to determine whether to
    * continue with importing this item. In the context of a CSV upload, This will be invoked on every row to help a
    * user notice errors.
    *
    * @param item
    *   An [[ImportItem]] to be validated.
    * @return
    *   A left value if validation failed, a right value if succeeded. The right value is assumed to be able to be
    *   passed to an invocation of [[execute]].
    */
  def validate(item: Item): ValidationError \/ Validated[Item] =
    mkValidated(item).right

  protected final def mkValidated(item: Item): Validated[Item] =
    new Validated[Item](item) {}

  /** Imports a valid [[ImportItem]].
    *
    * @param item
    *   The item to import
    * @return
    *   A left value if the import failed (along with an error message), and a right value if the item was successfully
    *   persisted.
    */
  def execute(invoker: UserDTO, item: Validated[Item]): PersistError \/ ImportSuccess
end Importer

object Importer:

  /* gonna roll the scala/bug#6794 dice here */
  sealed abstract case class Validated[+Item <: ImportItem] protected[Importer] (item: Item)
