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

package loi.cp.web

import com.fasterxml.jackson.databind.node.{JsonNodeFactory, MissingNode, ObjectNode}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.HttpResponse
import com.learningobjects.cpxp.scala.json.JacksonCodecs
import com.learningobjects.cpxp.service.exception.{HttpApiException, RestErrorType, RestExceptionInterface}
import com.learningobjects.cpxp.util.GuidUtil
import de.tomcat.juli.LogMeta
import loi.apm.Apm
import loi.cp.web.converter.Bowdlerize
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.HttpStatus

import scala.jdk.CollectionConverters.*

/** Default resolver to resolve exceptions to HTTP responses.
  */
@Service
class DefaultExceptionResolver(
  serviceMeta: ServiceMeta
) extends ExceptionResolver:

  private val log = org.log4s.getLogger

  private def isClientError(statusCode: Int) = statusCode >= 400 && statusCode < 500

  private def isOkError(statusCode: Int) = statusCode >= 200 && statusCode < 300

  override def resolveException(throwable: Throwable): HttpResponse =

    ExceptionUtils
      .getThrowableList(throwable)
      .asScala
      .collectFirst({
        case t: HttpResponseException  => (t, t)
        case t: RestExceptionInterface => (reiResponse(t), t)
        case t: HttpApiException       => (haeResponse(t), t)
        case t: HttpResponse           => t -> throwable
      })
      .map({
        case (resp, t: RestExceptionInterface) if resp.statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY =>
          val argoJson = JacksonCodecs.jsonNodeEnc(t.getJson)
          LogMeta.let("responseBody" -> Bowdlerize(argoJson)) {
            log.info(t.getMessage)
          }
          resp
        case (resp, t) if resp.statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY                         =>
          log.info(t)("unprocessable entity")
          resp
        case (resp, t) if isClientError(resp.statusCode) || isOkError(resp.statusCode)                  =>
          log.info(t)("client error")
          resp
        case (resp, t)                                                                                  =>
          log.error(t)(s"API Error")
          Apm.noticeError(t)
          resp
      }) getOrElse {
      // then `throwable` can't be handled by this resolver
      val guid = GuidUtil.errorGuid()
      log.error(throwable)(s"API Exception; guid=$guid")
      Apm.noticeError(throwable)
      val msg  = if serviceMeta.isProdLike then MissingNode.getInstance() else getJsonStackTrace(throwable)
      HttpResponseEntity.from(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        new ErrorResponseBody(RestErrorType.UNEXPECTED_ERROR, msg, guid)
      )
    }

  private def reiResponse(rei: RestExceptionInterface): HttpResponse =
    val body =
      if rei.getJson == null || rei.getErrorType == null then
        // this means no body should be returned.. but this triggers ApiDispatcherServlet
        // to return 406..
        None
      else Some(new ErrorResponseBody(rei.getErrorType, rei.getJson))

    HttpResponseEntity.from(rei.getHttpStatusCode, body.orNull)

  private def haeResponse(hae: HttpApiException): HttpResponse =
    HttpResponseEntity.from(hae.getStatusCode, hae)

  private def getJsonStackTrace(throwable: Throwable): ObjectNode =
    val err = JsonNodeFactory.instance.objectNode()
    err.put("message", throwable.getMessage)

    val trace = JsonNodeFactory.instance.arrayNode()
    ExceptionUtils.getStackFrames(throwable).foreach(trace.add)
    err.set[ObjectNode]("trace", trace)

    err
end DefaultExceptionResolver
