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

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Date

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants
import com.learningobjects.cpxp.service.query.{BaseDataProjection, Comparison, QueryService, Function as QBFunction}
import com.learningobjects.cpxp.service.session.SessionConstants
import com.learningobjects.cpxp.service.user.{UserConstants, UserWebService}
import scalaz.std.anyVal.*
import scalaz.syntax.std.option.*

/** Domain usage calculator. */
object DomainUsageInfoCalculator:

  /** Compute the usage of a domain.
    * @param domain
    *   the domain to query
    * @param now
    *   the current time
    * @param uws
    *   the user web service
    * @param qs
    *   the query service
    * @return
    *   usage information for the domain
    */
  def calculate(domain: Id, now: Instant)(implicit uws: UserWebService, qs: QueryService): DomainUsageInfo =
    val id         = domain.getId
    val userFolder = uws.getUserFolder(id)
    val zdt        = ZonedDateTime.ofInstant(now, ZoneId.systemDefault)
    val weekAgo    = Date.from(zdt.minusWeeks(1).toInstant)
    val monthAgo   = Date.from(zdt.minusMonths(1).toInstant)
    val quarterAgo = Date.from(zdt.minusMonths(3).toInstant)

    val totalUsers = userFolder.queryChildren(UserConstants.ITEM_TYPE_USER).getAggregateResult(QBFunction.COUNT)

    val totalActiveUsers =
      qs.queryAllDomains(UserConstants.ITEM_TYPE_USER_HISTORY)
        .addCondition(UserConstants.DATA_TYPE_ACCESS_TIME, Comparison.ne, null)
        .addJoinQuery(DataTypes.META_DATA_TYPE_PARENT_ID, userFolder.queryChildren(UserConstants.ITEM_TYPE_USER))
        .setCacheQuery(false)
        .getAggregateResult(QBFunction.COUNT)

    val weeklyActiveUsers :: monthlyActiveUsers :: quarterlyActiveUsers :: Nil =
      weekAgo :: monthAgo :: quarterAgo :: Nil map { whenAgo =>
        qs.queryAllDomains(UserConstants.ITEM_TYPE_USER_HISTORY)
          .addCondition(UserConstants.DATA_TYPE_ACCESS_TIME, Comparison.ge, whenAgo)
          .addJoinQuery(DataTypes.META_DATA_TYPE_PARENT_ID, userFolder.queryChildren(UserConstants.ITEM_TYPE_USER))
          .setCacheQuery(false)
          .getAggregateResult(QBFunction.COUNT)
      }: @unchecked

    val totalLogins =
      qs.queryAllDomains(UserConstants.ITEM_TYPE_USER_HISTORY)
        .addJoinQuery(DataTypes.META_DATA_TYPE_PARENT_ID, userFolder.queryChildren(UserConstants.ITEM_TYPE_USER))
        .setDataProjection(BaseDataProjection.ofAggregateData(UserConstants.DATA_TYPE_LOGIN_COUNT, QBFunction.SUM))
        .setCacheQuery(false)
        .getResult[Number]

    val totalEnrollments =
      qs.queryRoot(id, EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
        .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false)
        .getAggregateResult(QBFunction.COUNT)

    val weeklyEnrollments =
      qs.queryRoot(id, EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
        .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false)
        .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_CREATED_ON, Comparison.ge, weekAgo)
        .setCacheQuery(false)
        .getAggregateResult(QBFunction.COUNT)

    // Pull stats from a week ago so we always have at least a week's usage aggregated
    val weekAgoDate       = SessionConstants.SESSION_STATISTICS_DATE_FORMAT.format(weekAgo)
    val sessionStatistics =
      qs.queryRoot(id, SessionConstants.ITEM_TYPE_SESSION_STATISTICS)
        .addCondition(SessionConstants.DATA_TYPE_SESSION_STATISTICS_DATE, Comparison.eq, weekAgoDate)
        .setDataProjection(
          BaseDataProjection.ofData(
            SessionConstants.DATA_TYPE_SESSION_STATISTICS_COUNT,
            SessionConstants.DATA_TYPE_SESSION_STATISTICS_DURATION
          )
        )
        .setCacheQuery(false)
        .getValues[Array[?]] collectFirst { case Array(count: Number, duration: Number) =>
        SessionStatistics(count.intValue, duration.longValue)
      }

    DomainUsageInfo(
      users = totalUsers,
      activeUsers = totalActiveUsers,
      weeklyActiveUsers = weeklyActiveUsers,
      monthlyActiveUsers = monthlyActiveUsers,
      quarterlyActiveUsers = quarterlyActiveUsers,
      logins = Option(totalLogins).map(_.longValue).orZero,
      enrollments = totalEnrollments,
      weeklyEnrollments = weeklyEnrollments,
      sessionDuration = sessionStatistics.orZero
    )
  end calculate
end DomainUsageInfoCalculator
