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

package loi.db

import com.learningobjects.cpxp.util.TestProject
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory
import com.zaxxer.hikari.util.DriverDataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import de.mon.MonitorProxy
import jakarta.persistence.spi.{ClassTransformer, PersistenceUnitInfo, PersistenceUnitTransactionType}
import jakarta.persistence.{SharedCacheMode, ValidationMode}
import org.hibernate.jpa.HibernatePersistenceProvider
import scaloi.syntax.AnyOps.*

import java.net.{URI, URL}
import java.util
import java.util.Properties
import javax.sql.DataSource
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** PersistenceUnit information built using typesafe config. Supports configuring a non-jta datasource and property map
  * to be passed to he underlying provider. Example configuration:
  * {{{
  *   example {
  * properties {
  * hibernate.dialect = "com.learningobjects.cpxp.hibernate.CpxpPostgreSQLDialect"
  * hibernate.archive.autodetection = ""
  * hibernate.hbm2ddl.auto = "update"
  * hibernate.order_updates = "true"
  * hibernate.generate_statistics = "true"
  * hibernate.boot.allow_jdbc_metadata_access = "false"
  * hibernate.cache.use_query_cache = "false"
  * hibernate.cache.use_second_level_cache = "false"
  * }
  * datasource {
  * user = "ug"
  * pass = "ugpass123"
  * driver = "org.postgresql.Driver"
  * }
  * }
  * }}}
  * @author
  *   zpowers
  */
class ConfigPersistenceUnitInfo(config: Config, ds: DataSource, props: Properties) extends PersistenceUnitInfo:

  override def getPersistenceUnitRootUrl: URL = null

  @scala.annotation.nowarn
  override def getTransactionType: PersistenceUnitTransactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL

  override def getManagedClassNames: util.List[String] = new util.ArrayList[String]()

  override def getPersistenceProviderClassName: String = classOf[HibernatePersistenceProvider].getName

  override def getSharedCacheMode: SharedCacheMode = SharedCacheMode.ENABLE_SELECTIVE

  override def getPersistenceXMLSchemaVersion: String = "1.0"

  override def getNewTempClassLoader: ClassLoader = getClassLoader

  override def getClassLoader: ClassLoader = this.getClass.getClassLoader

  override def getJtaDataSource: DataSource = null

  override def getMappingFileNames: util.List[String] = new util.ArrayList[String]()

  override def getPersistenceUnitName: String = config.getString("name")

  override def excludeUnlistedClasses(): Boolean = false

  override def addTransformer(transformer: ClassTransformer): Unit = ()

  override def getJarFileUrls: util.List[URL] = new util.ArrayList[URL]()

  override def getProperties: Properties = props

  override def getValidationMode: ValidationMode = ValidationMode.NONE

  override def getNonJtaDataSource: DataSource = ds

  override def getScopeAnnotationName: String = null

  override def getQualifierAnnotationNames: util.List[String] = new util.ArrayList[String]()
end ConfigPersistenceUnitInfo

object ConfigPersistenceUnitInfo:

  /** A string which, should it appear in a datasource URL, will be swapped out for the name of the project currently
    * under test, if one is known to be.
    *
    * This allows us parallelly to run dbtests without worrying about cross-test interference effects.
    */
  final val TestProjectPlaceholder = "@PROJECT@"

  def fromConfig(config: Config): ConfigPersistenceUnitInfo =
    new ConfigPersistenceUnitInfo(config, dataSource(config), properties(config))

  def dataSource(config: Config): DataSource =
    val url      =
      val cfgUrl = config.getString("datasource.url")
      Option(TestProject.name) map (_.toLowerCase) match // pg lowercases db names
        case Some(name) if cfgUrl `contains` TestProjectPlaceholder =>
          val actualUrl = cfgUrl.replace(TestProjectPlaceholder, s"_$name")
          // mm, jdbc "urls" aren't really urls, you can't have two schemata...
          ensureDb(config, new URI(actualUrl.stripPrefix("jdbc:")).getPath.stripPrefix("/"))
          actualUrl
        case _                                                      => cfgUrl.replace(TestProjectPlaceholder, "")
    val user     = config.getString("datasource.user")
    val pass     = config.getString("datasource.pass")
    val driver   = config.getString("datasource.driver")
    val poolSize = config.getInt("hikari.poolSize")
    val register = config.getBoolean("hikari.register")
    val name     = config.getString("name")
    val base     = if config.getBoolean("hikari.enabled") then
      val hconf = new HikariConfig()
      hconf.setJdbcUrl(url)
      hconf.setUsername(user)
      hconf.setPassword(pass)
      hconf.setDriverClassName(driver)
      hconf.setPoolName(name)
      hconf.setMaximumPoolSize(poolSize)
      hconf.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory)
      hconf.setRegisterMbeans(register)
      hconf.setPoolName(name)
      new HikariDataSource(hconf)
    else new DriverDataSource(url, driver, new Properties(), user, pass)
    if config.getBoolean("datasource.monitoring") then MonitorProxy.proxy(base)
    else base
  end dataSource

  def ensureDb(config: Config, name: String): Unit = if !knownDbs(name) then
    // i.e. jdbc:postgres://localhost:5432/aboveground
    val dfltUrl =
      config.getString("datasource.url").replace(TestProjectPlaceholder, "")
    val dfltCfg = ConfigFactory
      .parseString(s"""datasource { url = "$dfltUrl" \n monitoring = false }""")
      .withFallback(config)
    // i.e., ug
    val owner   = config.getString("datasource.user")
    val ds      = dataSource(dfltCfg)
    Using.resource(ds.getConnection()) { cxn =>
      Using.resource(cxn.createStatement()) { stmt =>
        // horrid, postgres, no "CREATE DATABASE IF NOT EXIST"
        val exists = Using.resource(stmt.executeQuery(s"SELECT 1 FROM pg_database WHERE datname = '$name'")) { count =>
          count.next() && (count.getInt(1) > 0)
        }
        if !exists then stmt.execute(s"""CREATE DATABASE $name OWNER $owner;""")
      }
    }
    knownDbs += name

  private val knownDbs: mutable.Set[String] = mutable.Set.empty

  private def properties(config: Config): Properties =
    new Properties() <| { props =>
      config
        .getConfig("properties")
        .entrySet()
        .asScala
        .foreach(entry => props.put(entry.getKey, entry.getValue.unwrapped))
    }
end ConfigPersistenceUnitInfo
