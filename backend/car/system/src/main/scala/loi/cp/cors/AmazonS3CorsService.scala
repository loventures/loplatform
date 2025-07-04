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

package loi.cp.cors

import com.amazonaws.regions.Regions
import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.service.attachment.AttachmentService
import com.learningobjects.cpxp.service.domain.DomainFacade
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import scalaz.syntax.std.boolean.*
import software.amazon.awssdk.services.s3.model.{CORSConfiguration, CORSRule, PutBucketCorsRequest}

import scala.jdk.CollectionConverters.*

/** Support for managing the S3 CORS configuration. */
object AmazonS3CorsService:
  private final val logger = org.log4s.getLogger

  /** Update the S3 CORS configuration to reflect the current hostnames associated with this system.
    * @param domain
    *   domain in case this is being done during initialization
    * @param ows
    *   the overlord web service
    * @param as
    *   the attachment service
    */
  def updateS3CorsConfiguration(domain: Option[DomainFacade] = None)(implicit
    ows: OverlordWebService,
    as: AttachmentService
  ): Unit =
    val provider = as.getDefaultProvider
    if provider.isS3 && !provider.identity.endsWith("EXAMPLE") && allDomainUrls.nonEmpty then
      logger info s"Updating S3 CORS configuration: ${provider.container} - ${Regions.getCurrentRegion}}"
      provider.s3Client.putBucketCors(
        PutBucketCorsRequest
          .builder()
          .bucket(provider.container)
          .corsConfiguration(corsConfiguration(domain))
          .build()
      )

  /** Return the S3 CORS configuration for this system.
    * @param ows
    *   the overlord web service
    * @return
    *   the S3 CORS configuration
    */
  private def corsConfiguration(
    domain: Option[DomainFacade]
  )(implicit ows: OverlordWebService): CORSConfiguration =
    val extraHostNames = domain.map(domainUrls).getOrElse(Nil)
    CORSConfiguration
      .builder()
      .corsRules(
        CORSRule
          .builder()
          .allowedMethods("GET", "PUT")
          .allowedOrigins((allDomainUrls ++ cdnUrls ++ extraHostNames).distinct.asJava)
          .allowedHeaders(X_ApmId, X_CSRF, ContentType, DoNotTrack)
          .exposeHeaders(X_ApmAppData)
          .build()
      )
      .build()
  end corsConfiguration

  /** APM header included in the OPTIONS request. */
  private final val X_ApmId = "x-apm-id"

  /** CSRF header included lawlessly in the OPTIONS request. */
  private final val X_CSRF = "x-csrf"

  /** included when doing a direct PUT of a blob to S3 */
  private val ContentType = "content-type"

  /** included when do not track */
  private val DoNotTrack = "dnt"

  /** APM Response Header */
  private val X_ApmAppData = "X-APM-App-Data"

  /** Get the CDN URLs for this system.
    * @return
    *   the CDN URLs
    */
  private def cdnUrls: Seq[String] =
    Option(BaseServiceMeta.getServiceMeta.getStaticHost)
      .fold[Seq[String]](Nil) { host =>
        Seq(s"http://$host", s"https://$host")
      }

  /** Get all domain URLs for this system.
    * @param ows
    *   the overlord web service
    * @return
    *   all domain URLs
    */
  private def allDomainUrls(implicit ows: OverlordWebService): Seq[String] =
    ows.getAllDomains.asScala.toSeq flatMap domainUrls

  /** Get the URLs for a domain.
    * @param domain
    *   the domain
    * @return
    *   the URLs for the domain
    */
  private def domainUrls(domain: DomainFacade): Seq[String] =
    val protocol = domain.getSecurityLevel.getIsSecure.fold("https", "http")
    domain.getHostNames.asScala.toSeq map { hostname =>
      s"$protocol://$hostname"
    }
end AmazonS3CorsService
