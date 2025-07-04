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

import argonaut.*
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.reference.EdgePath
import scalaz.Endo
import scaloi.data.SetDelta
import scaloi.json.ArgoExtras
import scaloi.syntax.MapOps.*

/** Gate overrides in a course.
  *
  * For both individual users and the entire course, an instructor can choose to unlock specific gated content. The sets
  * contained in this structure are those edge paths which the instructor has opened.
  *
  * @param perUser
  *   a map from student ID to the edge paths which are open to them
  * @param overall
  *   the set of edge paths which are opened to the entire course
  * @param assignment
  *   a map from content to the edge paths which no longer gate it
  */
final case class GateOverrides(
  perUser: Map[Long, Set[EdgePath]] = Map.empty,
  overall: Set[EdgePath] = Set.empty,
  assignment: Map[EdgePath, Set[EdgePath]] = Map.empty,
):

  /** Compute the effective set of overridden content paths for a given student.
    *
    * The effectively-overridden content paths are the union of the overrides for `user` and those for the entire
    * course.
    */
  def apply(user: UserId): Set[EdgePath] =
    overall | perUser.getOrElse(user.id, Set.empty)

  /** Apply a change to the overridden paths for `user`.
    */
  def ~(user: UserId, change: GateOverrides.Change): GateOverrides =
    this ~~ GateOverrides.Changes(perUser = Map(user.id -> change))

  /** Apply a change to the overall overridden paths.
    */
  def ~(overall: GateOverrides.Change): GateOverrides =
    this ~~ GateOverrides.Changes(overall = overall)

  def ~(gated: EdgePath, change: GateOverrides.Change): GateOverrides =
    this ~~ GateOverrides.Changes(assignment = Map(gated -> change))

  /** Apply a [[GateOverrides.Changes Changes]] object to these overrides.
    */
  def ~~(changes: GateOverrides.Changes): GateOverrides =
    def mapMerge[K](changes: Map[K, GateOverrides.Change]) =
      Endo[Map[K, Set[EdgePath]]] { current =>
        changes.foldLeft(current) { (overrides, entry) =>
          val (key, change) = entry
          overrides + (key -> overrides.get(key) ?|: change)
        }
      }
    copy(
      perUser = mapMerge(changes.perUser)(perUser),
      overall = overall |: changes.overall,
      assignment = mapMerge(changes.assignment)(assignment),
    ).simplify
  end ~~

  // remove users/assignments from the override map which have no overrides
  private def simplify: GateOverrides =
    copy(
      perUser = perUser.filterValues(_.nonEmpty),
      assignment = assignment.filterValues(_.nonEmpty),
    )
end GateOverrides

object GateOverrides:
  final val empty: GateOverrides = GateOverrides()

  type Change = SetDelta[EdgePath]

  final case class Changes(
    perUser: Map[Long, Change] = Map.empty,
    overall: Change = SetDelta.empty,
    assignment: Map[EdgePath, Change] = Map.empty,
  )

  import ArgoExtras.*
  implicit val changesCodec: CodecJson[Changes] = CodecJson.derive[Changes]
  implicit val codec: CodecJson[GateOverrides]  = CodecJson.derive[GateOverrides]
end GateOverrides
