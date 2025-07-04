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

package loi.cp.cache.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQueryUtils}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.util.ManagedUtils
import com.learningobjects.cpxp.util.cache.Cache as CpxpCache
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.Queryable
import loi.cp.admin.right.AdminRight
import org.hibernate.stat.{CacheRegionStatistics, Statistics}
import org.hibernate.{SessionFactory, Cache as HCache}
import scalaz.\/
import scalaz.syntax.either.*

import scala.annotation.meta.beanGetter
import scala.beans.BeanProperty

/** Web API for peeking at and clearing the caches.
  */
@Secured(Array(classOf[AdminRight]))
@Controller(value = "cache", root = true)
@RequestMapping(path = "cache")
trait CacheWebController extends ApiRootComponent:

  /** Gets the region names in the L2 cache. The L2 cache is partitioned by regions. The region is a scope that can be
    * cleared to avoid clearing the entire L2 cache. Each JPA type has its own region (e.g. there is an item region, and
    * a region for each finder type)
    *
    * @return
    *   the region names in the L2 cache
    */
  @RequestMapping(path = "l2/regionNames", method = Method.GET)
  def getL2RegionNames: List[String]

  /** Gets all of the entries in the given L2 cache region. We reflectively serialize the cache entries with Jackson,
    * and this sometimes causes NPEs (/shrug). Keep hitting the endpoint until you don't get one. L2 cache entries are
    * scalar data not the entities themselves.
    *
    * @param regionName
    *   the L2 region name
    * @return
    *   all of the entries in the given L2 cache region
    */
  @RequestMapping(path = "l2/regions/{regionName}", method = Method.GET)
  def getL2Region(@PathVariable("regionName") regionName: String): CacheRegionStatistics

  /** Gets just the statistics for all regions. */
  @RequestMapping(path = "l2/regionStats", method = Method.GET)
  def getL2Stats(q: ApiQuery): ApiQueryResults[L2Stats]

  /** Gets just the statistics for a region. */
  @RequestMapping(path = "l2/regionStats/{regionName}", method = Method.GET)
  def getL2RegionStats(@PathVariable("regionName") regionName: String): L2Stats

  /** Clear all L2 caches. */
  @RequestMapping(path = "l2/clear", method = Method.POST)
  def clearL2Cache(): Unit

  /** Get all of the names of the app caches. These are class names of implementations of
    * [[com.learningobjects.cpxp.util.cache.Cache]].
    *
    * Note that caches are lazily created, so if one has no entries it may appear nonexistent from this endpoint.
    *
    * @return
    *   the list of caches that currently exist on this appserver.
    */
  @RequestMapping(path = "app", method = Method.GET)
  def getAppCacheNames: Seq[String]

  /** Get statistics about the app caches.
    */
  @RequestMapping(path = "appStats", method = Method.GET)
  def getAppCacheStats: Seq[AppCacheStats]

  /** Get all of the keys in the given application cache.
    *
    * @param cache
    *   the name (fqcn) of the application cache
    * @return
    *   all keys currently in that cache.
    */
  @RequestMapping(path = "app/{cache}", method = Method.GET)
  def getAppCacheKeys(@PathVariable("cache") cache: String): Option[Seq[String]]

  /** Clear an application cache
    *
    * @param cache
    *   the name (fqcn) of the application cache
    */
  @RequestMapping(path = "app/{cache}/clear", method = Method.POST)
  def clearAppCache(@PathVariable("cache") cache: String): ErrorResponse \/ Unit
end CacheWebController

@Component
class CacheWebControllerImpl(
  val componentInstance: ComponentInstance,
  caches: Seq[CpxpCache[?, ?, ?]]
) extends CacheWebController
    with ComponentImplementation:

  import CacheWebControllerImpl.*

  override def getL2RegionNames: List[String] =
    l2CacheStats.getSecondLevelCacheRegionNames.toList

  override def getL2Region(regionName: String): CacheRegionStatistics =
    l2CacheStats.getDomainDataRegionStatistics(regionName)

  override def getL2Stats(q: ApiQuery): ApiQueryResults[L2Stats] =
    ApiQueryUtils.query(getL2RegionNames.map(getL2RegionStats), ApiQueryUtils.propertyMap[L2Stats](q))

  override def getL2RegionStats(regionName: String): L2Stats = L2Stats(regionName, getL2Region(regionName))

  override def clearL2Cache(): Unit = l2Cache.evictAllRegions()

  override def getAppCacheNames: Seq[String] =
    caches.map(_.getName)

  override def getAppCacheStats: Seq[AppCacheStats] =
    caches.map(AppCacheStats.apply)

  override def getAppCacheKeys(cache: String): Option[Seq[String]] =
    caches.find(_.getName == cache).map(_.getKeys.map(_.toString).toSeq)

  override def clearAppCache(cache: String): ErrorResponse \/ Unit =
    caches.find(_.getName == cache).fold[ErrorResponse \/ Unit](ErrorResponse.notFound.left)(_.clear().right)
end CacheWebControllerImpl

object CacheWebControllerImpl:

  private def l2Cache: HCache = sessionFactory.getCache

  private def l2CacheStats: Statistics = sessionFactory.getStatistics

  private def sessionFactory: SessionFactory =
    ManagedUtils.getEntityManagerFactory.unwrap(classOf[SessionFactory])

case class L2Stats(
  @(Queryable @beanGetter) @BeanProperty region: String,
  @(Queryable @beanGetter) @BeanProperty hitCount: Long,
  @(Queryable @beanGetter) @BeanProperty missCount: Long,
  @(Queryable @beanGetter) @BeanProperty putCount: Long,
  @(Queryable @beanGetter) @BeanProperty elementCountInMemory: Long,
  @(Queryable @beanGetter) @BeanProperty elementCountOnDisk: Long,
  @(Queryable @beanGetter) @BeanProperty sizeInMemory: Long
)

object L2Stats:
  def apply(region: String, l2: CacheRegionStatistics): L2Stats =
    L2Stats(
      region,
      l2.getHitCount,
      l2.getMissCount,
      l2.getPutCount,
      l2.getElementCountInMemory,
      l2.getElementCountOnDisk,
      l2.getSizeInMemory
    )
end L2Stats

case class AppCacheStats(
  name: String,
  hitCount: Int,
  missCount: Int,
  size: Int,
  invalidationSize: (Int, Int)
)

object AppCacheStats:
  def apply(cache: CpxpCache[?, ?, ?]): AppCacheStats = AppCacheStats(
    cache.getName,
    cache.getHitCount,
    cache.getMissCount,
    cache.getSize,
    cache.getInvalidationSize
  )
