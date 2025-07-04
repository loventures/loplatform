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

import java.time.Instant

/** Data that the frontend needs to display gating information to the learner.
  */
final case class ContentGateDto(
  enabled: Boolean,
  activityGatingPolicy: Option[ContentGateDto.ActivityGateInfo],
  temporalGatingPolicy: Option[ContentGateDto.TemporalGateInfo],
  rightsGatingPolicy: Option[ContentGateDto.RightsGatingInfo],
)

object ContentGateDto:

  /** Data about a time-based gate.
    *
    * @param lockDate
    *   the date at which the gated content becomes available.
    */
  final case class TemporalGateInfo(
    lockDate: Instant,
  )

  /** Data about an activity-based gate.
    *
    * @param gates
    *   the individual activity gates applied to this content. If there are multiple, ''all'' must pass before the gate
    *   is considered open.
    */
  final case class ActivityGateInfo(
    gates: List[SingleActivityGate]
  )

  /** Data about a single activity-based gate.
    *
    * Such a gate unlocks when the learner achieves a score of [[threshold]] or above on the linked assignment.
    *
    * @param assignmentId
    *   the identifier for the assignment which gates this content. It is a `String` for as long as this API can at
    *   least theoretically be
    * @param threshold
    *   the minimum score that must be achieved to open this gate
    */
  final case class SingleActivityGate(
    assignmentId: String,
    threshold: Double,
    disabled: Boolean,
  )

  /** Data about a rights-based gate.
    *
    * Such a gate locks based on rights that the user has in the course.
    *
    * @param policyType
    *   the type of policy that afflicts the current user.
    */
  final case class RightsGatingInfo(
    policyType: PolicyType,
  )
end ContentGateDto
