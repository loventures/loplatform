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

package loi.cp.content
package gate

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import loi.cp.course.lightweight.Lwc
import loi.cp.reference.EdgePath
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras.*

import scala.util.Try

/** A service for managing instructor gate overrides.
  *
  * An instructor can, from the gating page, override the gate for either the entire course or for a specific set of
  * users. This service manages the storage thereof.
  */
@Service
trait ContentGateOverrideService:
  import Accommodations.*

  /** Load the entire [[GateOverrides]] data for the given lightweight course.
    */
  def loadOverrides(course: Lwc): Try[GateOverrides]

  /** Convenience method to calculate the effective set of overridden content paths for a given student user in a
    * course.
    */
  def loadOverride(user: UserId, course: Lwc): Try[Set[EdgePath]] =
    loadOverrides(course).map(_.apply(user))

  /** Apply the provided `changes` to the overrides for the provided course.
    *
    * @see
    *   [[GateOverrides.Changes]]
    */
  def updateOverrides(course: Lwc)(changes: GateOverrides.Changes): Try[Unit]

  def loadAccommodations(course: ContextId, user: UserId): Try[Accommodations]

  def updateAccommodations(
    course: ContextId,
    user: UserId,
    edgePath: EdgePath,
    minutes: Option[Long]
  ): Try[Unit]
end ContentGateOverrideService

object Accommodations:
  // edgePath -> minutes | 0
  type Accommodations = Map[EdgePath, Long]

  val empty: Accommodations = Map.empty

  implicit val codec: CodecJson[Accommodations] = mapCodec[EdgePath, Long](_.toString, EdgePath.parse(_).some)
