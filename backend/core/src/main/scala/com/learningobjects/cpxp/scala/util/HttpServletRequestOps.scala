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

package com.learningobjects.cpxp.scala.util

import com.learningobjects.cpxp.util.HttpUtils
import com.learningobjects.de.web.MediaType
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.io.IOUtils
import scalaz.std.string.*
import scaloi.syntax.OptionOps.*

import java.net.URI
import scala.jdk.CollectionConverters.*

/** Enhancements on Tomcat HTTP requests. */
final class HttpServletRequestOps(private val self: HttpServletRequest) extends AnyVal:

  /** All accepted media types for this request */
  def acceptedMediaTypes: Seq[MediaType] =
    Option(self.getHeader("Accept"))
      .map(header => MediaType.sortByQualityValue(MediaType.parseMediaTypes(header)).asScala.toSeq)
      .getOrElse(Seq(MediaType.ALL))

  /** Whether this request accepts the given media type */
  def accepts(mtype: MediaType): Boolean =
    acceptedMediaTypes exists (_ `includes` mtype)

  /** Whether this request prefers to accept `mType` over `toType` */
  def prefers(mType: MediaType): Prefers = new Prefers(mType, acceptedMediaTypes)

  /** Whether this request is one of the safe methods (GET, HEAD, OPTIONS) */
  def isSafe: Boolean = HttpUtils.isSafe(self)

  /** Get a fully-qualified url from an absolute path. */
  def fqUrl(path: String): String = HttpUtils.getUrl(self, path)

  /** Get a fully-qualified URI from an absolute path. */
  def fqUri(path: String): URI = new URI(fqUrl(path))

  /** Get an optional parameter. */
  def param(name: String): Option[String] = Option(self.getParameter(name))

  /** Get an optional nonzero parameter. */
  def paramNZ(name: String): Option[String] = OptionNZ(self.getParameter(name))

  /** Get an optional header. */
  def header(name: String): Option[String] = Option(self.getHeader(name))

  /** Get the body of the request as a string. */
  def body: String = IOUtils.toString(self.getReader)

  /** Get a cookie value. */
  def cookie(name: String): Option[String] = Option(HttpUtils.getCookieValue(self, name))
end HttpServletRequestOps

final class Prefers(mType: MediaType, acceptedMediaTypes: Seq[MediaType]):
  def to(toType: MediaType): Boolean =
    val reverseAccepted = acceptedMediaTypes.reverse
    reverseAccepted.indexOf(mType) > reverseAccepted.indexOf(toType)

trait ToHttpServletRequestOps:
  import language.implicitConversions
  implicit def toHttpServletRequestOps(self: HttpServletRequest): HttpServletRequestOps =
    new HttpServletRequestOps(self)

object HttpServletRequestOps extends ToHttpServletRequestOps
