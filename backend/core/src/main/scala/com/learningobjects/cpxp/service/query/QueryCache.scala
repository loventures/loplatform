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

package com.learningobjects.cpxp.service.query

import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Cache}

import scala.collection.mutable
import scala.concurrent.duration.*

class QueryCache
    extends BucketGenerationalCache[String, QueryResults, QueryEntry](
      itemAware = true,
      replicated = true,
      timeout = 5.minutes
    ):
  private val listeners = mutable.Buffer.empty[Cache[?, ?, ?]]

  override def invalidate(inv: String, propagate: Boolean): Unit = synchronized {
    super.invalidate(inv, propagate)
    listeners foreach { _.invalidate(inv, propagate = false) }
  }

  def addInvalidationListener(cache: Cache[?, ?, ?]): Unit = synchronized {
    listeners.append(cache)
  }
end QueryCache
