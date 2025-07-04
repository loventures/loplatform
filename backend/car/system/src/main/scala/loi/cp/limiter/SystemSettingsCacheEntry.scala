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

package loi.cp.limiter

import com.learningobjects.cpxp.util.cache.Entry

/** A class to cache system-wide settings which are stored in the Overlord's Domain object.
  *
  * @param settings
  *   the system setting
  * @param key
  *   the key to cache these settings on
  */
final class SystemSettingsCacheEntry(key: String, settings: Set[String])
    extends Entry[String, Set[String]](key, settings)
