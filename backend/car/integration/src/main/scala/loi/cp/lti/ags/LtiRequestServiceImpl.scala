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

package loi.cp.lti.ags

import argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.HttpUtils
import loi.cp.bus.*
import loi.cp.integration.LtiSystemComponent
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.imsglobal.lti.launch.LtiOauthSigner
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.either.*
import scalaz.syntax.std.option.*
import scaloi.syntax.option.*

import java.io.IOException
import java.net.URI
import scala.reflect.*

@Service
class LtiRequestServiceImpl(
  domain: => DomainDTO
) extends LtiRequestService:

  private val logger = org.log4s.getLogger

  override def sendLtiServicePostWithoutResp[Body: EncodeJson](
    url: String,
    system: LtiSystemComponent[?],
    contentType: String,
    entity: Body,
    untx: YieldTx
  ): DeliveryFailure \/ Unit =
    val body = implicitly[EncodeJson[Body]].apply(entity).toString()
    for
      req             <- packageRequest(url, contentType, body, system)
      (sc, text, tpe) <- untx(executeRequest(req, body))
      _               <- validateResponse(req, sc, text, tpe)
    yield ()
  end sendLtiServicePostWithoutResp

  /** Helper that sends an LTI service post. Throws [[RuntimeException]] on non-200 response.
    */
  override def sendLtiServicePost[Body: EncodeJson, Resp: DecodeJson: ClassTag](
    url: String,
    system: LtiSystemComponent[?],
    contentType: String,
    entity: Body,
    untx: YieldTx
  ): DeliveryFailure \/ Resp =
    val body = implicitly[EncodeJson[Body]].apply(entity).toString()
    for
      req             <- packageRequest(url, contentType, body, system)
      (sc, text, tpe) <- untx(executeRequest(req, body))
      respBodyStr     <- validateResponse(req, sc, text, tpe)
      respBody        <- validateResponseBody(body, url, sc, respBodyStr, tpe)
      jsonResp        <- parseJson(respBody)
      responseBody    <-
        implicitly[DecodeJson[Resp]]
          .decodeJson(jsonResp)
          .toEither
          .toDisjunction
          .leftMap(e => PermanentFailure(s"Error parsing: $text into type: ${classTag[Resp].runtimeClass.getName}: $e"))
    yield responseBody
    end for
  end sendLtiServicePost

  override def sendLtiServiceDelete(
    url: String,
    system: LtiSystemComponent[?],
    untx: YieldTx
  ): DeliveryFailure \/ Unit =
    for
      req             <- signRequest(url, system)(new HttpDelete(url))
      (sc, text, tpe) <- untx(executeRequest(req, ""))
      _               <- validateResponse(req, sc, text, tpe)
    yield ()

  private def parseJson(body: String): DeliveryFailure \/ Json =
    Parse.parse(body).toDisjunction.leftMap(PermanentFailure.apply)

  private def validateResponse[A <: HttpUriRequest](
    req: A,
    statusCode: Integer,
    responseBody: Option[String],
    contentType: String
  ): DeliveryFailure \/ Option[String] =
    (statusCode >= 200 && statusCode < 300) either responseBody or {
      logger.info(s"Status code was not successful ($statusCode), received body from ${req.getURI}: $responseBody")
      TransientFailure(
        FailureInformation(Request(req), Left(Response(responseBody, contentType, statusCode)))
      )
    }

  private def validateResponseBody(
    requestBody: String,
    url: String,
    statusCode: Integer,
    respBody: Option[String],
    contentType: String
  ): DeliveryFailure \/ String =
    respBody toRightDisjunction {
      logger.info(s"Lti service response body was empty (status $statusCode, url: $url)")
      TransientFailure(
        FailureInformation(Request(url, requestBody, "POST"), Left(Response(None, contentType, statusCode)))
      )
    }

  private def executeRequest(req: HttpUriRequest, body: String): DeliveryFailure \/ (Int, Option[String], String) =
    try
      logger.info(s"Executing LTI request: ${req.getURI} $body ${req.getMethod}")
      val resp = HttpUtils.getHttpClient.execute(req)

      try
        val maybeRespBody = Option(EntityUtils.toString(resp.getEntity))
          .catap(
            respBody => logger.info(s"Got response from LTI service: $respBody"),
            logger.info("Got empty response from LTI service")
          )
        processResponse(resp, maybeRespBody)
      catch
        case e: IllegalArgumentException =>
          logger.error(e)(s"Exception thrown while executing LTI request")
          (
            resp.getStatusLine.getStatusCode,
            Option.empty[String],
            Option(resp.getFirstHeader("Content-Type")).map(_.getValue).getOrElse("")
          ).right
      finally EntityUtils.consume(resp.getEntity)
      end try
    catch
      case ioe: IOException =>
        TransientFailure(FailureInformation(Request(req.getURI.toString, body, req.getMethod), ioe)).left
      case e: Exception     =>
        TransientFailure(e).left

  private def packageRequest(
    url: String,
    contentType: String,
    body: String,
    system: LtiSystemComponent[?]
  ): DeliveryFailure \/ HttpPost = signRequest(url, system) {
    val req = new HttpPost(url)
    req.setHeader("Content-Type", contentType)
    req.setEntity(new StringEntity(body))
    req
  }

  private def signRequest[A <: HttpRequestBase](url: String, system: LtiSystemComponent[?])(
    req: => A
  ): DeliveryFailure \/ A = \/.attempt {
    // hack to allow our integration tests to pass a cross-domain id header in URL user info
    Option(req.getURI.getUserInfo).filter(_ == domain.domainId) foreach { xDomain =>
      req.setURI(URI.create(url.replace(xDomain + "@", "")))
      req.setHeader(CurrentFilter.HTTP_HEADER_X_DOMAIN_ID, xDomain)
    }

    val signer = new LtiOauthSigner
    signer.sign(req, system.getSystemId, system.getKey).asInstanceOf[A]
  }(PermanentFailure.apply)

  private def processResponse(resp: CloseableHttpResponse, body: Option[String]) =
    (resp.getStatusLine.getStatusCode, body, Option(resp.getFirstHeader("Content-Type")).map(_.getValue).getOrElse(""))
      .right[DeliveryFailure]
end LtiRequestServiceImpl
