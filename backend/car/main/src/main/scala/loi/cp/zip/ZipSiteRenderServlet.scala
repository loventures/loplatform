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

package loi.cp.zip

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.AttachmentFacade
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.admin.right.{EnforceAdminRight, MediaAdminRight}
import scalaz.syntax.std.option.*
import scaloi.misc.Extractor

@Component
@ServletBinding(path = ZipSiteRenderServlet.Binding)
@EnforceAdminRight(classOf[MediaAdminRight])
class ZipSiteRenderServlet(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  cs: ComponentService
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import ErrorResponse.*
  import ServletDispatcher.*
  import ZipSiteRenderServlet.*

  protected def handler = { case RequestMatcher(Method.GET, Path(siteId, attId, pi), _, _) =>
    for
      site <- siteId.component_?[ZipSiteImpl] \/> notFound
      att  <- site.getRevision(attId) \/> notFound
    yield
      site.serve(att.facade[AttachmentFacade], pi, false)
      NoResponse
  }
end ZipSiteRenderServlet

object ZipSiteRenderServlet:
  final val Binding = "/sys/zip"

  object Path:
    private val Prefixed = Extractor `dropPrefix` Binding

    def apply(site: Long, att: Long, pi: String): String =
      s"$Binding/$site/$att/${pi.stripPrefix("/")}"

    def unapply(arg: String): Option[(Long, Long, String)] = arg match
      case Prefixed(rest) =>
        rest.tail.split('/') match
          case Array(site, att)      =>
            try Some((site.toLong, att.toLong, "index.html"))
            catch case _: NumberFormatException => None
          case Array(site, att, pi*) =>
            try Some((site.toLong, att.toLong, pi.mkString("/", "/", "")))
            catch case _: NumberFormatException => None
          case _                     => None
      case _              => None
  end Path
end ZipSiteRenderServlet
