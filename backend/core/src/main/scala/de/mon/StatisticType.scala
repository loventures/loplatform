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

package de.mon

import enumeratum.{Enum, EnumEntry}

/** Enumeration of basic statistic types.
  */
sealed trait StatisticType extends EnumEntry

/** Statistic type companion. */
object StatisticType extends Enum[StatisticType]:

  /** Get the enumeration values. */
  override val values = findValues

  /** A row lock statement. */
  case object Lock extends StatisticType

  /** A select statement. */
  case object Select extends StatisticType

  /** A CTE select statement (unparseable). */
  case object Cte extends StatisticType

  /** An update statement. */
  case object Update extends StatisticType

  /** An insert statement. */
  case object Insert extends StatisticType

  /** A delete statement. */
  case object Delete extends StatisticType

  /** A DDL statement. */
  case object Ddl extends StatisticType

  /** An unknown SQL statement. */
  case object UnknownSQL extends StatisticType

  /** An eviction L2 cache operation. */
  case object Evict extends StatisticType

  /** A data table access. */
  case object Data extends StatisticType

  /** A JSON serialization to the database or l2 cache. */
  case object SaveJson extends StatisticType

  /** A JSON deserialization from the database or l2 cache. */
  case object LoadJson extends StatisticType

  /** A component instantiation. */
  case object Component extends StatisticType

  /** An application cache invalidation (is not a SQL statement). */
  case object AppCacheInvalidate extends StatisticType

  /** An ehcache invalidation (is not a SQL statement). */
  case object LoCacheInvalidate extends StatisticType

  /** A cache hit. */
  case object LoCacheHit extends StatisticType
end StatisticType
