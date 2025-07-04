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

package loi.cp.usage

import com.learningobjects.cpxp.scala.util.MkHeader
import scalaz.Monoid
import scaloi.syntax.FiniteDurationOps.*
import scalaz.syntax.semigroup.*
import scala.concurrent.duration.*

/** Captures domain usage information.
  */
case class DomainUsageInfo(
  users: Long,
  activeUsers: Long,
  weeklyActiveUsers: Long,
  monthlyActiveUsers: Long,
  quarterlyActiveUsers: Long,
  logins: Long,
  enrollments: Long,
  weeklyEnrollments: Long,
  sessionDuration: SessionStatistics
)

/** Domain usage information companion.
  */
object DomainUsageInfo:

  /** Monoid evidence for domain usage information. */
  implicit val monoid: Monoid[DomainUsageInfo] = Monoid.instance(
    (a, b) =>
      DomainUsageInfo(
        a.users + b.users,
        a.activeUsers + b.activeUsers,
        a.weeklyActiveUsers + b.weeklyActiveUsers,
        a.monthlyActiveUsers + b.monthlyActiveUsers,
        a.quarterlyActiveUsers + b.quarterlyActiveUsers,
        a.logins + b.logins,
        a.enrollments + b.enrollments,
        a.weeklyEnrollments + b.weeklyEnrollments,
        a.sessionDuration |+| b.sessionDuration,
      ),
    DomainUsageInfo(0, 0, 0, 0, 0, 0, 0, 0, Monoid[SessionStatistics].zero)
  )

  /** Column headers. */
  val Headers: List[String] = MkHeader[DomainUsageInfo]
end DomainUsageInfo

/** Session statistics
  */
case class SessionStatistics(
  count: Int,
  duration: Long
):
  override def toString: String = if count == 0 then "-" else (duration / count).millis.toHumanString

object SessionStatistics:

  /** Monoid evidence for domain usage information. */
  implicit val monoid: Monoid[SessionStatistics] = Monoid.instance(
    (a, b) => SessionStatistics(a.count + b.count, a.duration + b.duration),
    SessionStatistics(0, 0)
  )
