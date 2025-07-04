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

package loi.cp.devops

import java.nio.charset.StandardCharsets
import java.util.logging.{Level, Logger}

import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.util.HttpUtils
import com.typesafe.config.Config
import org.apache.http.HttpStatus
import org.apache.http.client.methods.{HttpEntityEnclosingRequestBase, HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils
import scalaz.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/** Minimal API to devops API functions.
  */
object DevOps:
  private final val log    = Logger.getLogger(DevOps.getClass.getName)
  private final val mapper = JacksonUtils.getMapper

  /** context.xml parameters. */
  private final val ExecuteApiUrl = "apiUrl"
  private final val ExecuteApiKey = "apiKey"

  type GoodString = String; type BadString = String
  private def requiredConfig(key: String)(implicit config: Config): BadString \/ GoodString =
    if isConfigured then \/-(config.getConfig("loi.cp.devops").getString(key))
    else -\/(s"loi.cp.devops is unconfigured.")

  /** Returns whether devops API integration is configured for this system. */
  def isConfigured(implicit config: Config): Boolean = config.getConfig("loi.cp.devops").getBoolean("configured")

  /** Requests that a CNAME be generated pointing at the current cluster. Returns a change identifier, on success, or
    * else an error message.
    */
  def requestCName(hostName: String)(implicit config: Config): BadString \/ GoodString =
    for
      url      <- requiredConfig(ExecuteApiUrl)
      key      <- requiredConfig(ExecuteApiKey)
      request   = jsonRequest(url, "cname", key, DnsRequest(hostName))
      response <- execute[String](request)
    yield response

  /** Polls for completion of a CNAME request based on a previously-returned change identifier. Returns completion
    * status or else an error message.
    */
  def pollCName(changeId: String)(implicit config: Config): BadString \/ Boolean =
    for
      url      <- requiredConfig(ExecuteApiUrl)
      key      <- requiredConfig(ExecuteApiKey)
      request   = getRequest(url, "cname", key, "changeId" -> changeId)
      response <- execute[String](request)
      confirm  <- validateStatus(response)
    yield confirm

  /** Validates a poll status */
  private def validateStatus(status: String): BadString \/ Boolean =
    status match
      case "PENDING" => false.right[String]
      case "INSYNC"  => true.right[String]
      case s         => s"Invalid status: $s".left[Boolean]

  /** Executes a HTTP request with appropriate logging, parsing a successful result from JSON as the implicit type T, or
    * else returning an error message.
    */
  private def execute[T](request: HttpUriRequest)(implicit tt: ClassTag[T]): BadString \/ T =
    try
      val client   = HttpUtils.getHttpClient
      log.config(s"Request: ${request.getMethod} ${request.getURI}")
      request match
        case entity: HttpEntityEnclosingRequestBase =>
          val body =
            EntityUtils.toString(entity.getEntity, StandardCharsets.UTF_8)
          log.config(s"Body: $body")
        case _                                      =>
      // request.getAllHeaders foreach { h => log.info(s"Header: $h") }
      val response = client.execute(request)
      log.config(s"Status: ${response.getStatusLine}")
      response.getAllHeaders foreach { h =>
        log.config(s"Header: $h")
      }
      Option(response.getEntity).fold("No response".left[T]) { entity =>
        val contentType = ContentType.getOrDefault(entity)
        val body        = EntityUtils.toString(entity, StandardCharsets.UTF_8)
        log.config(s"Response: $contentType: ${truncate(body)}")
        val sc          = response.getStatusLine.getStatusCode
        if sc != HttpStatus.SC_OK then
          val json =
            if isJson(contentType) then mapper.readTree(body)
            else mapper.getNodeFactory.nullNode
          Option(json.get("errorMessage"))
            .orElse(Option(json.get("message")))
            .fold(s"HTTP status code: $sc")(_.asText)
            .left[T]
        else if !isJson(contentType) then s"Non-JSON response: $contentType".left[T]
        else
          mapper
            .readValue(body, tt.runtimeClass.asInstanceOf[Class[T]])
            .right[String]
        end if
      }
    catch
      case NonFatal(e) =>
        log.log(Level.WARNING, "HTTP exception", e)
        "HTTP exception".left[T]

  /** Truncates a string to 1280 characters with embedded ellipsis upon overflow. */
  private def truncate(body: String): GoodString =
    (body.length < 1280) ? body | (body.substring(0, 1260) + "..." + body
      .substring(body.length - 17))

  /** Given a base URL, API fragment, key and JSON body, produces a corresponding POST request. */
  private def jsonRequest(url: String, api: String, key: String, body: Any): HttpUriRequest =
    val request = new HttpPost(url.concat(api))
    request.setEntity(jsonEntity(body))
    request.addHeader("X-API-Key", key)
    request

  /** Given a base URL, API fragment, key and parameter list, produces a corresponding GET request. */
  private def getRequest(url: String, api: String, key: String, parameters: (String, String)*): HttpUriRequest =
    val builder = parameters.foldLeft(new URIBuilder(url.concat(api))) { case (b, (k, v)) =>
      b.addParameter(k, v)
    }
    val request = new HttpGet(builder.build)
    request.addHeader("X-API-Key", key)
    request

  /** Turns a POJO into a JSON entity. */
  private def jsonEntity(a: Any): StringEntity =
    new StringEntity(mapper.writeValueAsString(a), APPLICATION_JSON_SANS_CHARSET)

  /** Tests whether a content type is JSON, ignoring charset. */
  private def isJson(contentType: ContentType): Boolean =
    ContentType.APPLICATION_JSON.getMimeType == contentType.getMimeType

  // AWS Lambda fails spuriously with the standard application/json; charset=UTF-8
  private val APPLICATION_JSON_SANS_CHARSET =
    ContentType.create(ContentType.APPLICATION_JSON.getMimeType)

  /** A DNS request. */
  case class DnsRequest(name: String)
end DevOps
