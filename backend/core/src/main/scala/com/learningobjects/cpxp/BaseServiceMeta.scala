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

package com.learningobjects.cpxp

import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{DescribeTagsRequest, Filter}
import com.amazonaws.util.EC2MetadataUtils
import com.learningobjects.cpxp.BaseServiceMeta.*
import com.learningobjects.cpxp.scala.cpxp.Summon.summon
import com.learningobjects.cpxp.service.upgrade.{SystemInfo, UpgradeService}
import com.typesafe.config.Config
import org.apache.commons.lang3.StringUtils

import java.net.{InetAddress, NetworkInterface, URI}
import java.util.jar.{Attributes, Manifest}
import _root_.scala.compiletime.uninitialized
import _root_.scala.jdk.CollectionConverters.*
import _root_.scala.util.Try

class BaseServiceMeta extends ServiceMeta:

  final val _manifest: Manifest = readManifest()

  private def readManifest(): Manifest =
    try
      val clazz        = classOf[BaseServiceMeta]
      val className    = clazz.getSimpleName + ".class"
      val classPath    = clazz.getResource(className)
      val manifestPath = URI.create(classPath.toExternalForm.replaceAll("!.*", "!/META-INF/MANIFEST.MF"))
      val in           = manifestPath.toURL.openStream()
      try
        new Manifest(in)
      finally
        in.close()
    catch case _: Throwable => new Manifest()

  override def getVersion: String =
    val version = _manifest.getMainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
    if version == null then "AppleTragedy"
    else version

  override def getBuild: String =
    val build = _manifest.getMainAttributes.getValue("Implementation-Build")
    if build == null then "0"
    else build

  override def getBranch: String =
    val branch = _manifest.getMainAttributes.getValue("Implementation-Branch")
    if branch == null then "0"
    else branch

  override def getRevision: String =
    val revision = _manifest.getMainAttributes.getValue("Implementation-Revision")
    if revision == null then "0"
    else revision

  override def getRevisionLink: String =
    val revision = _manifest.getMainAttributes.getValue("Implementation-RevisionLink")
    if revision == null then "https://cdn0.tnwcdn.com/wp-content/blogs.dir/1/files/2015/11/torvaldrant.png"
    else revision

  override def getBuildDate: String =
    val buildDate = _manifest.getMainAttributes.getValue("Implementation-BuildDate")
    if buildDate == null then "0"
    else buildDate

  override def getBuildNumber: String =
    val buildNumber = _manifest.getMainAttributes.getValue("Implementation-BuildNumber")
    if buildNumber == null then "0"
    else buildNumber

  override def getJvm: String =
    System.getProperty("java.vm.version")

  override def getJava: String =
    System.getProperty("java.version")

  private var _localName: String      = uninitialized
  private var _networkLocal: String   = uninitialized
  private var _networkCentral: String = uninitialized
  private var _das: Boolean           = uninitialized
  private var _networkPort: Int       = uninitialized
  private var _cluster: String        = uninitialized
  private var _clusterType: String    = uninitialized
  private var _node: String           = uninitialized
  private var _staticHost: String     = uninitialized
  private var _staticSuffix: String   = uninitialized
  private var _ponyHerd: String       = uninitialized

  private var _pekkoSingleton: Boolean = uninitialized

  override def pekkoSingleton: Boolean =
    _pekkoSingleton

  def doConfigure(config: Config): Unit =
    val networkConfig = config.getConfig("com.learningobjects.cpxp.network")
    val staticConfig  = config.getConfig("com.learningobjects.cpxp.static")
    _networkLocal = validateLocalhost(networkConfig.getString("localhost"))
    _localName = Try(InetAddress.getLocalHost.getHostName).getOrElse("")

    val ssl        = config.getBoolean("de.tomcat.ssl")
    if ssl then _networkPort = config.getInt("de.tomcat.sslPort")
    else _networkPort = config.getInt("de.tomcat.port")
    _cluster = networkConfig.getString("cluster.name")
    _clusterType = networkConfig.getString("cluster.type")
    _node = networkConfig.getString("node.name")
    val statichost = staticConfig.getString("host")
    if StringUtils.isBlank(statichost) then _staticHost = null
    else _staticHost = statichost
    _staticSuffix = "." + staticConfig.getString("suffix")
    _pekkoSingleton = networkConfig.getBoolean("cluster.singleton")
    _ponyHerd = if isLocal then "local" else getPonyHerdTagValue

    doAcquireCentralHost()
    doHeartbeat()
  end doConfigure

  private def doAcquireCentralHost(): Unit =
    // TODO: TODO: TODO: If I acquire central host from null then that implies system
    // restart so I should run bootstrap.
    val upgradeService = summon[UpgradeService]
    val centralHost    = upgradeService.acquireCentralHost(_networkLocal)
    if _networkLocal.equals(centralHost) then
      if !_networkLocal.equals(_networkCentral) then logger.warn(s"Acquired central host status, ${_networkLocal}")
      // TODO: BUG: If I become the central host while live, no singleton
      // scheduled tasks will be run...
      _networkCentral = _networkLocal
      _das = true
    else
      if _das then logger.warn(s"Lost central host status, ${_networkLocal}")
      _networkCentral = centralHost
      _das = false
    end if
  end doAcquireCentralHost

  // TODO: break all this stuff out into somewhere it belongs

  private def doHeartbeat(): Unit =
    val upgradeService = summon[UpgradeService]
    upgradeService.heartbeat(_networkLocal, _node)

  def doReleaseCentralHost(): Unit =
    val upgradeService = summon[UpgradeService]
    upgradeService.releaseCentralHost(_networkLocal)
    _das = false

  override def isDas: Boolean = _das

  override def getLocalName: String = _localName

  override def getLocalHost: String = _networkLocal

  override def getCentralHost: String = _networkCentral

  override def getPort: Int = _networkPort

  override def getCluster: String = _cluster

  override def getClusterType: String = _clusterType

  override def isLocal: Boolean = // local laptop, else AWS
    CLUSTER_TYPE_LOCAL.equals(_clusterType)

  override def isProduction: Boolean =
    CLUSTER_TYPE_PRODUCTION.equals(_clusterType)

  override def isProdLike: Boolean =
    CLUSTER_TYPE_PRODUCTION.equals(_clusterType) ||
      CLUSTER_TYPE_PATCH.equals(_clusterType) ||
      CLUSTER_TYPE_CERTIFICATION.equals(_clusterType)

  override def getStaticHost: String = _staticHost

  override def getStaticSuffix: String = _staticSuffix

  override def getNode: String = _node

  override def getPonyHerd: String = _ponyHerd

  private def getPonyHerdTagValue: String =
    val tagValue = Try {
      val ec2          = Ec2Client.create()
      val instanceList = new java.util.LinkedList[String]()

      instanceList.add(EC2MetadataUtils.getInstanceId)

      val request = DescribeTagsRequest
        .builder()
        .filters(
          Filter.builder().name("key").values("ponyHerd").build(),
          Filter.builder().name("resource-id").values(instanceList).build()
        )
        .build()

      val results = ec2.describeTags(request)

      if results != null then results.tags.asScala.headOption.map(_.value)
      else None
    }

    tagValue.toOption.flatten.getOrElse("unknown")
  end getPonyHerdTagValue
end BaseServiceMeta

object BaseServiceMeta:
  private val logger = org.log4s.getLogger

  lazy val serviceMeta = new BaseServiceMeta()

  def getServiceMeta: BaseServiceMeta = serviceMeta

  def configure(loiConfig: Config): Unit           =
    serviceMeta.doConfigure(loiConfig)
  /* Ensure that a configured localhost remains valid. */
  def validateLocalhost(localHost: String): String =

    var validLocalHost: String = if StringUtils.isNotEmpty(localHost) then
      val addrList     = new java.util.HashSet[String]()
      val ifcs         = NetworkInterface.getNetworkInterfaces
      while ifcs.hasMoreElements do
        val ifc = ifcs.nextElement()
        if ifc.isUp then
          val addrs = ifc.getInetAddresses
          while addrs.hasMoreElements do
            val addr = addrs.nextElement()
            addrList.add(addr.getHostAddress)
      val localAddress = InetAddress.getByName(localHost).getHostAddress
      if !addrList.contains(localAddress) then
        logger.warn(s"Ignoring invalid Network/localHost, $localHost, $localAddress, $addrList")
        null
      else localHost
    else null

    if StringUtils.isEmpty(validLocalHost) then // may have been nulled by invalidity
      InetAddress.getLocalHost.getHostName
    else validLocalHost
  end validateLocalhost

  def acquireCentralHost(): Unit =
    serviceMeta.doAcquireCentralHost()

  // TODO: break all this stuff out into somewhere it belongs

  def heartbeat(): Unit =
    serviceMeta.doHeartbeat()

  def findRecentHosts(): java.util.List[SystemInfo] =
    val upgradeService = summon[UpgradeService]
    upgradeService.findRecentHosts()

  def releaseCentralHost(): Unit =
    serviceMeta.doReleaseCentralHost()
end BaseServiceMeta
