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

package loi.cp.web.converter

import java.lang.reflect.{ParameterizedType, Type}
import java.nio.charset.StandardCharsets
import java.util as ju

import argonaut.{Json, Parse}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.{Component, RequestBody}
import com.learningobjects.cpxp.component.web.converter.{ConvertOptions, HttpMessageConverter}
import com.learningobjects.cpxp.component.web.{ArgoBody, WebRequest}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.util.{HttpUtils, ManagedUtils}
import com.learningobjects.de.web.MediaType
import de.tomcat.juli.LogMeta
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.util.Using

@Component
class ArgoBodyMessageConverter(val componentInstance: ComponentInstance)(
  sm: ServiceMeta,
) extends HttpMessageConverter[ArgoBody[?]]
    with ComponentImplementation:
  import ArgoBodyMessageConverter.*

  override def getSupportedMediaTypes: ju.List[MediaType] = SupportedMediaTypes

  override def canRead(tpe: Type, mediaType: MediaType): Boolean =
    MediaType.APPLICATION_JSON.includes(mediaType) && tpe.isInstanceOf[ParameterizedType] &&
      (classOf[ArgoBody[?]] == tpe.asInstanceOf[ParameterizedType].getRawType)

  override def read(requestBody: RequestBody, request: WebRequest, targetType: Type): ArgoBody[?] =
    val length = request.getRawRequest.getContentLength
    val json   = Parse.parse(request.getRawRequest.body)
    if requestBody.log then
      if length < MaxLoggableSize then
        json.fold(
          s => logger.info(s"Error parsing JSON: $s"),
          j => LogMeta.let("json" -> Bowdlerize(j))(logger.info(s"Parsing JSON for: ${request.getPath}"))
        )
      else logger.info(s"Not logging JSON for submission of size $length bytes")
    new ArgoBody(json)
  end read

  override def canWrite(value: AnyRef, mediaType: MediaType): Boolean =
    MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) && value.isInstanceOf[ArgoBody[?]]

  override def write(body: ArgoBody[?], op: ConvertOptions, req: HttpServletRequest, rsp: HttpServletResponse): Unit =
    ManagedUtils.end() // omg
    rsp.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
    body.json foreach { json =>
      Using.resource(HttpUtils.getOutputStream(req, rsp)) { o =>
        o.write(stringulate(json).getBytes(StandardCharsets.UTF_8))
      }
    }

  private def stringulate(j: Json) = if sm.isLocal then j.spaces2 else j.nospaces
end ArgoBodyMessageConverter

object ArgoBodyMessageConverter:
  private final val logger              = org.log4s.getLogger
  private final val MaxLoggableSize     = 65536
  private final val SupportedMediaTypes = ju.Collections.singletonList(MediaType.APPLICATION_JSON)
