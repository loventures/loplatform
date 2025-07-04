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

package loi.cp.status

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.util.{ClientConfig, HttpUtils}
import com.typesafe.config.Config
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntity, HttpResponse}
import scalaz.syntax.std.boolean.*
import scaloi.syntax.AnyOps.*

import scala.concurrent.{ExecutionContext, Future, blocking}

/** Client API to the cluster status root. Used to interrogate member status among the cluster over HTTP.
  */
@Service
class ClusterStatusClient(
  sm: ServiceMeta,
  mapper: ObjectMapper,
  config: Config
):
  import ClusterStatusClient.*

  /** Get the cluster status of a cluster member. */
  def getClusterStatus(host: String)(implicit ec: ExecutionContext): Future[ClusterStatus] =
    Future {
      val request  = new HttpGet(s"http://$host:${sm.getPort}$BaseUrl/status")
      // x-domain to system to allow inter-cluster-member access
      request.addHeader(CurrentFilter.HTTP_HEADER_X_DOMAIN_ID, CurrentFilter.DOMAIN_ID_SYSTEM)
      logger info s"Member status request $request"
      val response = blocking {
        http(config).execute(request)
      }
      response match
        case SuccessfulResponse(entity) =>
          mapper.readValue(entity.getContent, classOf[ClusterStatus]) <| { status =>
            logger info s"Member status: $status"
          }
        case OtherResponse(sc, entity)  =>
          EntityUtils.consumeQuietly(entity)
          throw new Exception(s"Member status response $sc from $host")
    }
end ClusterStatusClient

/** Cluster status client companion. */
object ClusterStatusClient:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** The base SRS URL. */
  private val BaseUrl = "/api/v2/clusterStatus"

  private var httpClient: HttpClient = null

  /** A dedicated HTTP client that bypasses the configured proxy. */
  private def http(config: Config) = synchronized {
    if httpClient eq null then
      httpClient = HttpUtils.HttpClientBuilder
        .newMonitoredClient("ClusterStatus")
        .setUseProxy(false)
        .build(ClientConfig.fromConfig(config))
    httpClient
  }

  /** An extractor for a successful HTTP response. */
  object SuccessfulResponse:

    /** Extract the successful entity from a HTTP response. */
    def unapply(response: HttpResponse): Option[HttpEntity] =
      (response.getStatusLine.getStatusCode == 200).option(response.getEntity)

  /** An extractor for an other-than-successful HTTP response. */
  object OtherResponse:

    /** Extract the status code and entity from a HTTP response. */
    def unapply(response: HttpResponse): Option[(Int, HttpEntity)] =
      (response.getStatusLine.getStatusCode != 200).option(response.getStatusLine.getStatusCode -> response.getEntity)
end ClusterStatusClient
