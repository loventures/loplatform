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

package com.learningobjects.cpxp.util

import org.hibernate.stat.{CacheRegionStatistics, Statistics}

import scala.language.implicitConversions
import scala.reflect.{ClassTag, classTag}

class HibernateStatisticsOps(val stats: Statistics) extends AnyVal:

  // Note well that @Cache(READ_ONLY) entities and collections put even if the cache already contains the key
  def putHitMiss: (Long, Long, Long) = (
    stats.getSecondLevelCachePutCount,
    stats.getSecondLevelCacheHitCount,
    stats.getSecondLevelCacheMissCount
  )

  def sizeHitMiss: (Long, Long, Long) = (
    size,
    stats.getSecondLevelCacheHitCount,
    stats.getSecondLevelCacheMissCount
  )

  def size: Long = stats.getSecondLevelCacheRegionNames.foldLeft(0L) { case (acc, name) =>
    acc + stats.getCacheRegionStatistics(name).getElementCountInMemory
  }

  def region[A: ClassTag]: CacheRegionStatistics =
    val regionName = classTag[A].runtimeClass.getName
    stats.getCacheRegionStatistics(regionName)

  def region[A: ClassTag](collectionName: String): CacheRegionStatistics =
    val regionName = classTag[A].runtimeClass.getName + "." + collectionName
    stats.getCacheRegionStatistics(regionName)
end HibernateStatisticsOps

class CacheRegionStatisticsOps(val regionStats: CacheRegionStatistics) extends AnyVal:

  // Note well that @Cache(READ_ONLY) entities and collections put even if the cache already contains the key
  def putHitMiss: (Long, Long, Long) =
    (
      regionStats.getPutCount,
      regionStats.getHitCount,
      regionStats.getMissCount
    )

  def sizeHitMiss: (Long, Long, Long) =
    (
      size,
      regionStats.getHitCount,
      regionStats.getMissCount
    )

  def size: Long = regionStats.getElementCountInMemory
end CacheRegionStatisticsOps

object HibernateStatisticsOps:
  implicit def statisticsOps(s: Statistics): HibernateStatisticsOps                   = new HibernateStatisticsOps(s)
  implicit def regionStatisticOps(s: CacheRegionStatistics): CacheRegionStatisticsOps = new CacheRegionStatisticsOps(s)
