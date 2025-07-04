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

package com.learningobjects.cpxp.component.web

import com.google.common.net.HttpHeaders
import com.learningobjects.cpxp.BaseWebContext
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{BaseHtmlWriter, ComponentSupport}
import com.learningobjects.cpxp.filter.SendFileFilter
import com.learningobjects.cpxp.scala.json.JacksonCodecs
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.ServiceException
import com.learningobjects.cpxp.util.{HttpUtils, MimeUtils}
import com.learningobjects.de.web.MediaType
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.apm.Apm
import org.apache.commons.io.IOUtils

import scala.compat.java8.OptionConverters.*
import scala.util.Using

object WebResponseOps:
  def send(w: WebResponse, request: HttpServletRequest, response: HttpServletResponse): Unit =

    if w.statusCode > 0 then response.setStatus(w.statusCode)

    w.headers foreach { case (name, value) =>
      response.setHeader(name, value)
    }

    w match
      case NoResponse =>

      case NoContentResponse =>
        HttpUtils.setExpired(response)
        response.setContentType(MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8)

      case ErrorResponse(statusCode, _, body) =>
        HttpUtils.setExpired(response)
        if isInteractive(request) then
          (statusCode, body) match
            case (HttpServletResponse.SC_NOT_FOUND, _)                                                             =>
              ComponentSupport.getFn("errors", "notFound").invoke()
            case (HttpServletResponse.SC_FORBIDDEN | HttpServletResponse.SC_UNAUTHORIZED, bodyOpt: Option[AnyRef]) =>
              ComponentSupport
                .getFn("errors", "forbidden")
                .invoke(bodyOpt.collect { case ex: ServiceException =>
                  ex
                }.asJava) // this does forbidden or unauthorized as appropriate
            case (HttpServletResponse.SC_BAD_REQUEST, Some(ex: ServiceException))                                  =>
              Apm.noticeError(ex)
              ComponentSupport.getFn("errors", "badRequest").invoke(ex)
            case _                                                                                                 => // currently no other errors reported
              response.sendError(statusCode)
        else if body.isEmpty then response.sendError(statusCode)
        else
          response.setStatus(statusCode)
          response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8)

          if statusCode == 422 then
            // there is nothing in the codebase that I am proud of
            val json     = ComponentUtils.toJsonNode(body)
            val argoJson = JacksonCodecs.jsonNodeEnc(json)
            // TODO Bowdlerize (you can't from here)
            // TODO use a common method on DefaultExceptionResolver (you can't from here)
            LogMeta.let("responseBody" -> argoJson) {
              log.info("ErrorResponse")
            }

          Using.resource(HttpUtils.getWriter(request, response)) { out =>
            ComponentUtils.getObjectMapper.writeValue(out, body)
          }
        end if

      case dr @ DirectResponse(_, _) =>
        HttpUtils.setExpired(response)
        dr.respond(response)

      case RedirectResponse(url, _, _) =>
        if w.statusCode != HttpServletResponse.SC_MOVED_PERMANENTLY then HttpUtils.setExpired(response)
        // TODO: remove this extra header and make the creator of RedirectResponse handle the url appropriately
        response
          .setHeader(HttpHeaders.LOCATION, HttpUtils.getUrl(request, url))

      case DispatchResponse(url, _, _) =>
        HttpUtils.setExpired(response)
        request.getRequestDispatcher(url).forward(request, response)

      case HtmlResponse(html, _, _) =>
        HttpUtils.setExpired(response)
        response.setContentType(MimeUtils.MIME_TYPE_TEXT_HTML + MimeUtils.CHARSET_SUFFIX_UTF_8)
        val out = new BaseHtmlWriter(HttpUtils.getWriter(request, response))
        BaseWebContext.getContext.setHtmlWriter(out) // This will go away with the death of context htmlwriter
        try
          out.write(html)
          out.close()
        finally BaseWebContext.getContext.setHtmlWriter(null)

      case TextResponse(text, mediaType, _, _) =>
        if !w.headers.contains(HttpHeaders.CACHE_CONTROL) then HttpUtils.setExpired(response)
        response.setContentType(mediaType.toString)
        Using.resource(HttpUtils.getWriter(request, response)) { out =>
          out.write(text)
        }

      case FileResponse(info, _, _) =>
        request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, info)

      case StreamResponse(is, _, _) =>
        HttpUtils.setExpired(response)
        IOUtils.copy(is, response.getOutputStream)

      case EntityResponse(entity, statusCode, headers) =>
        HttpUtils.setExpired(response)
        response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8)
        Using.resource(HttpUtils.getWriter(request, response)) { out =>
          JacksonUtils.getMapper.writeValue(out, entity)
        }

      case r @ ArgoResponse(e, statusCode, headers) =>
        HttpUtils.setExpired(response)
        response.setContentType(MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8)
        Using.resource(HttpUtils.getWriter(request, response)) { out =>
          out.write(r.encoder.encode(e).nospaces)
        }
    end match
  end send

  def isInteractive(request: HttpServletRequest) =
    isWebBrowser(request) && !isXHR(request) && !prefersJson(request)

  private def isWebBrowser(request: HttpServletRequest): Boolean =
    request.header(HttpHeaders.USER_AGENT).exists(BrowserUA.findFirstIn(_).isDefined)

  private def isXHR(request: HttpServletRequest): Boolean =
    request.header(HttpHeaders.X_REQUESTED_WITH) contains XMLHttpRequest

  private def prefersJson(request: HttpServletRequest): Boolean =
    request `prefers` MediaType.APPLICATION_JSON `to` MediaType.TEXT_HTML

  private val BrowserUA = "MSIE|Chrome|Mozilla|Opera".r

  private val XMLHttpRequest = "XMLHttpRequest"

  private val log = org.log4s.getLogger
end WebResponseOps
