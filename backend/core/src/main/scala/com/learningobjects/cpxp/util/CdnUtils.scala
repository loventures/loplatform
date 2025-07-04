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

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.upgrade.ClusterSupport
import jakarta.servlet.http.HttpServletRequest
import scaloi.syntax.AnyOps.*

import java.lang.management.ManagementFactory
import javax.annotation.{Nonnull, Nullable}

/** Utility methods for working with the CDN.
  */
object CdnUtils extends ManagedObject:

  /** Returns a route to a given path on the application via the CDN, if the CDN is valid in the current request
    * context. Otherwise returns the path unchanged.
    *
    * @param path
    *   an absolute path; e.g. /api/v2/foo/bar
    * @param request
    *   the current HTTP request, or null
    * @param domain
    *   the current domain, or null
    * @param serviceMeta
    *   the current service meta
    *
    * @return
    *   a route to that path via the CDN, or else the path unchanged
    */
  def cdnUrl(path: String)(implicit
    @Nullable request: HttpServletRequest,
    @Nullable domain: DomainDTO,
    @Nonnull serviceMeta: ServiceMeta
  ): String =
    cdnPrefix.fold(path) { prefix =>
      prefix.concat(path)
    }

  /** Returns the URL prefix to the CDN if the CDN is valid in the current request context. This typically looks like
    * https://<cdn>/cdn/<host>.
    *
    * @param request
    *   the current HTTP request, or null
    * @param domain
    *   the current domain, or null
    * @param serviceMeta
    *   the current service meta
    *
    * @return
    *   the cdn prefix, if valid
    */
  def cdnPrefix(implicit
    @Nullable request: HttpServletRequest,
    @Nullable domain: DomainDTO,
    @Nonnull serviceMeta: ServiceMeta
  ): Option[String] =
    for
      request    <- Option(request)
      serverName <- Option(request.getServerName)
      requestHost = serverName.toLowerCase
      domain     <- Option(domain)
      if requestHost == domain.hostName
      staticHost <- Option(serviceMeta.getStaticHost)
      protocol    = if request.isSecure then "https://" else "http://"
      hostPart    = requestHost stripSuffix serviceMeta.getStaticSuffix
    yield s"$protocol$staticHost/cdn/$hostPart"

  /** The CDN suffix. Changes when new versions are deployed, to force CloudFront to refresh assets from the server.
    */
  def cdnSuffix(implicit
    serviceMeta: ServiceMeta,
    entityContext: EntityContext
  ): String =
    val key =
      if serviceMeta.isLocal then
        ManagementFactory.getRuntimeMXBean.getStartTime / 1000 // local dev use jvm start time as cdn key
      else
        serviceMeta.getRevision.take(
          8
        ) + serviceMeta.getBuildNumber                         // hack on build number until the carchives get their own cdn suffix
    val version =
      if entityContext != null && entityContext.getEntityManager.isOpen then
        ClusterSupport.clusterSystemInfo.getCdnVersion <| (version => cdnVersion = version)
      else cdnVersion
    s"${key}_$version"
  end cdnSuffix

  /** Update the CDN version. This causes the suffix to change, and will therefore force the CDN to see new versions of
    * assets. Use if the CDN has cached wrong data.
    */
  def incrementCdnVersion(): Unit =
    val sysInfo = ClusterSupport.clusterSystemInfo
    sysInfo.setCdnVersion(sysInfo.getCdnVersion + 1)

  /** The HTTP servlet request attribute that is set to true on a request being executed on behalf of the CDN.
    */
  final val CdnRequestAttribute = "cdn"

  /** The CDN version.
    *
    * Authoritatively stored in SystemInfo, but cached here to remove the dependency on a hibernate session for
    * rendering CDN urls.
    *
    * This looks like a gigantic race condition, sure, but almost all threads will just be setting it to the same value
    * anyhow.
    */
  private final var cdnVersion = 0
end CdnUtils
