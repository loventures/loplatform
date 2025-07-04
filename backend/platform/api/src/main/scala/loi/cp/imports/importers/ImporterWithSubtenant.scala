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

package loi.cp.imports.importers

import java.lang as jl

import com.learningobjects.cpxp.scala.json.{Absent, Null, OptionalField, Present}
import loi.cp.imports.ViolationNel
import loi.cp.imports.errors.FieldViolation
import loi.cp.subtenant.SubtenantService
import scalaz.std.option.*
import scalaz.syntax.std.option.*
import scaloi.syntax.cobind.*

trait ImporterWithSubtenant:

  implicit val subtenantService: SubtenantService

  /** Validates that a named subtenant is known. */
  protected def validateSubtenant(tenantId: String): ViolationNel[String] =
    subtenantService
      .findSubtenantByTenantId(tenantId)
      .toSuccessNel(FieldViolation("subtenant", tenantId, "Unknown subtenant").widen)
      .map(_.getTenantId)

  /** Applies a subtenant update function to the subtenant specified in an import item, if non-absent. */
  protected def importSubtenant(subtenant: OptionalField[String])(f: Option[jl.Long] => Unit): Unit =
    subtenant match
      case Absent()          => ()
      case Null()            => f(None)
      case Present(tenantId) =>
        subtenantService.findSubtenantByTenantId(tenantId).map(_.getId).coflatForeach(f)
end ImporterWithSubtenant
