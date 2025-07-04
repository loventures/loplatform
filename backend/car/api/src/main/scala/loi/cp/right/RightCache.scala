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

package loi.cp.right

import com.learningobjects.cpxp.service.query.QueryCache
import com.learningobjects.cpxp.util.cache.BucketGenerationalCache
import org.apache.commons.lang3.tuple.Pair

import java.{lang as jl, util as ju}
import scala.concurrent.duration.*

class RightCache(queryCache: QueryCache)
    extends BucketGenerationalCache[Pair[jl.Long, jl.Long], ju.Set[Class[? <: Right]], RightEntry](
      itemAware = true,
      replicated = false,
      timeout = 5.minutes
    ):
  // This is nine times monstrous but the right cache listens for query cache invalidations
  // around enrollments in order to know when to invalidate cached rights.
  queryCache.addInvalidationListener(this)
