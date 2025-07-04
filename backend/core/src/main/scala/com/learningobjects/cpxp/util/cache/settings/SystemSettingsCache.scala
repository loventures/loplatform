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

package com.learningobjects.cpxp.util.cache.settings

import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry}

import scala.concurrent.duration.*

/** A class to cache system-wide settings which are stored in the Overlord's Domain object. Assumes settings are a list
  * of strings (a single string entry is cached a a list of one). The key should be the Data Type name from the
  * OverlordDomainConstants file.
  *
  * NOTE: We could set up a cool Scala union type to represent either a list of strings or a single string, but that has
  * been deferred until we actually have a use case using a single string. - rantonucci
  */
final class SystemSettingsCache
    extends BucketGenerationalCache[String, Set[String], Entry[String, Set[String]]](
      itemAware = true,
      replicated = false,
      timeout = 1.hour
    )
