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

package com.learningobjects.cpxp.locache

import org.hibernate.boot.spi.SessionFactoryOptions as SFO
import org.hibernate.cache.cfg.spi.{DomainDataRegionBuildingContext as DDRBC, DomainDataRegionConfig as DDRC}
import org.hibernate.cache.spi.support.{
  DomainDataRegionTemplate,
  DomainDataStorageAccess,
  RegionFactoryTemplate,
  StorageAccess
}
import org.hibernate.cache.spi.{CacheKeysFactory, DomainDataRegion, ExtendedStatisticsSupport, RegionFactory}
import org.hibernate.engine.spi.SessionFactoryImplementor as SFI
import org.hibernate.stat.CacheRegionStatistics

import java.util

/** A replication and app-cache aware cache region factory. */
class LoCacheRegionFactory extends AbstractRegionFactory(LoCache.shutdown):
  override def prepareForUse(settings: SFO, configValues: util.Map[String, AnyRef]): Unit = ()

  override def createDomainDataStorageAccess(regionConfig: DDRC, buildingContext: DDRBC): DomainDataStorageAccess =
    LoCache.getOrCreate(regionConfig.getRegionName)

/** A simple cache region factory for use in tests. */
class SimpleCacheRegionFactory extends AbstractRegionFactory(() => ()):
  override def prepareForUse(settings: SFO, configValues: util.Map[String, AnyRef]): Unit = ()

  override def buildDomainDataRegion(regionConfig: DDRC, buildingContext: DDRBC): DomainDataRegion =
    new SimpleRegion(
      regionConfig,
      this,
      new SimpleCache,
      getImplicitCacheKeysFactory,
      buildingContext
    )
end SimpleCacheRegionFactory

class AbstractRegionFactory(destroyer: () => Unit) extends RegionFactoryTemplate:
  override def prepareForUse(settings: SFO, configValues: util.Map[String, AnyRef]): Unit = ()

  override def createQueryResultsRegionStorageAccess(regionName: String, sessionFactory: SFI): StorageAccess = ???

  override def createTimestampsRegionStorageAccess(regionName: String, sessionFactory: SFI): StorageAccess = ???

  override def releaseFromUse(): Unit = destroyer()

// region type for the SimpleCache
class SimpleRegion(
  regionConfig: DDRC,
  regionFactory: RegionFactory,
  cache: SimpleCache,
  defaultKeysFactory: CacheKeysFactory,
  buildingContext: DDRBC
) extends DomainDataRegionTemplate(regionConfig, regionFactory, cache, defaultKeysFactory, buildingContext)
    with ExtendedStatisticsSupport:

  override def getElementCountInMemory: Long = cache.size()

  override val getElementCountOnDisk: Long = 0

  override def getSizeInMemory: Long = CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN
end SimpleRegion
