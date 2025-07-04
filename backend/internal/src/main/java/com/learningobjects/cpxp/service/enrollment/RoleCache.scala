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

package com.learningobjects.cpxp.service.enrollment

import java.lang
import java.util.Date

import com.learningobjects.cpxp.service.query.QueryCache
import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry}

import scala.concurrent.duration.*
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scaloi.syntax.date.*

class RoleCache(queryCache: QueryCache)
    extends BucketGenerationalCache[(lang.Long, lang.Long), List[lang.Long], RoleEntry](
      itemAware = true,
      replicated = false,
      timeout = 15.minutes
    ):
  queryCache.addInvalidationListener(this)

object RoleEntry:
  def apply(user: lang.Long, context: lang.Long, enrollments: List[EnrollmentFacade], now: Date): RoleEntry =
    import EnrollmentConstants.*
    val invalidationKeys = Set(
      s"$INVALIDATION_KEY_PREFIX_GROUP_MEMBERSHIP$context",
      s"$INVALIDATION_KEY_PREFIX_USER_MEMBERSHIP$user"
    )
    val roles            = enrollments
      .filter(e => !e.getDisabled && !e.getStartTime.after(now) && e.getStopTime.after(now))
      .flatMap(e => Option(e.getRoleId))
      .distinct
    val expires          = enrollments
      .filterNot(_.getDisabled)
      .flatMap(e => List(e.getStartTime, e.getStopTime))
      .filterNot(_.before(now))
      .minimum
    new RoleEntry(user -> context, roles, invalidationKeys, expires)
  end apply
end RoleEntry

class RoleEntry(
  key: (lang.Long, lang.Long),
  roles: List[lang.Long],
  invalidationKeys: Set[String],
  expires: Option[Date]
) extends Entry[(lang.Long, lang.Long), List[lang.Long]](key, roles, invalidationKeys):
  override def isStale: Boolean = expires.exists(System.currentTimeMillis >= _.getTime)
