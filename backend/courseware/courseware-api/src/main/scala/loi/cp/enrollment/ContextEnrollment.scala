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

package loi.cp.enrollment

import java.time.Instant

import loi.cp.context.ContextId
import loi.cp.right.Right

/** A service object representing an enrollment in either domain or section. An enrollment grants a role to a user for a
  * specified period of time. A context grants rights to holders of particular roles.
  *
  * @param id
  *   the persistence id of the enrollment
  * @param context
  *   the id fo the context (domain or section) the enrollment is for
  * @param user
  *   the user this enrollment belongs to
  * @param role
  *   the role the user has in the context
  * @param disabled
  *   whether this enrollment is disabled
  * @param startDate
  *   the time the enrollment starts be active, if constrained by a start date
  * @param endDate
  *   the time the enrollment stops being active, if constrained by an end date
  */
case class ContextEnrollment(
  id: Long,
  context: ContextId,
  user: Long,
  role: Role,
  disabled: Boolean,
  startDate: Option[Instant],
  endDate: Option[Instant],
  dataSource: Option[String] = None
):

  /** Whether this enrollment is, or will be or was, active at a given date.
    *
    * @param when
    *   the date to check this enrollment against
    * @return
    *   whether this enrollment is active and valid for the given time
    */
  def isActive(when: Instant): Boolean =
    val inEnrollmentPeriod: Boolean =
      startDate.forall(start => when.isAfter(start)) && endDate.forall(end => when.isBefore(end))
    !disabled && inEnrollmentPeriod
end ContextEnrollment

/** An enrollment paired with what rights you are granted in that context (domain or section).
  *
  * @param enrollment
  *   the enrollment in the context
  * @param rightsSet
  *   the rights granted when the enrollment is active
  */
abstract class ContextRights(enrollment: ContextEnrollment, rightsSet: RightsSet):

  /** Returns whether you do, or will or did, have the given right at the given time.
    *
    * @param when
    *   the date to check this enrollment against
    * @param right
    *   the target right
    * @return
    *   whether the enrollment is active at the given time and has the given right at the given time
    */
  def hasRight(when: Instant, right: Class[? <: Right]): Boolean =
    enrollment.isActive(when) && rightsSet.hasRight(right)

  /** Returns whether you do, or will or did, have any of the given rights at the given time.
    *
    * @param when
    *   the date to check this enrollment against
    * @param rights
    *   the target rights
    * @return
    *   whether the enrollment is active at the given time and has at least one given right at the given time
    */
  def hasAnyRight(when: Instant, rights: Class[? <: Right]*): Boolean =
    enrollment.isActive(when) && rightsSet.hasAnyRight(rights*)
end ContextRights

case class DomainRights(enrollment: ContextEnrollment, rightsSet: RightsSet)
    extends ContextRights(enrollment, rightsSet)

case class SectionRights(enrollment: ContextEnrollment, rightsSet: RightsSet)
    extends ContextRights(enrollment, rightsSet)

object ContextRights:
  implicit class ContextRightsCollectionOps(val rights: Seq[ContextRights]) extends AnyVal:
    def hasRight(when: Instant, right: Class[? <: Right]): Boolean =
      rights.exists(_.hasRight(when, right))

    def hasAnyRight(when: Instant, right: Class[? <: Right]*): Boolean =
      rights.exists(_.hasAnyRight(when, right*))
