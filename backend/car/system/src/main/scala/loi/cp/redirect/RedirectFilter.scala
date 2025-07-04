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

package loi.cp.redirect

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.cpxp.util.HttpUtils
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import scaloi.syntax.AnyOps.*
import scaloi.syntax.OptionOps.*

@Component
@FilterBinding(priority = 1000)
class RedirectFilter(val componentInstance: ComponentInstance, env: ComponentEnvironment)(implicit
  domain: () => DomainDTO,
  fs: FacadeService,
  qs: QueryService
) extends FilterComponent
    with ComponentImplementation:
  import RedirectFilter.*
  import RedirectRootApiImpl.*

  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    def absolutize(href: String): String = href.transformWhen(_.startsWith("/"))(HttpUtils.getUrl(request, _))
    val path                             = request.getRequestURI.toLowerCase // /path/to
    val redirects                        = redirectMap.redirects
    Option(request.getQueryString)
      .flatMap(qs => redirects.get(s"$path?$qs")) // match by path plus query string
      .orElse(redirects.get(path)) // or just path
      .tap(redirect => HttpUtils.sendRedirect(response, absolutize(redirect))) // then redirect
      .isEmpty // and continue processing if no redirect
  end filter

  private def redirectMap: RedirectMap =
    Option(redirectFolder).fold(RedirectMap.empty) { folder =>
      val generation = Option(folder.getGeneration).fold(0L)(_.longValue)
      Option(env.getAttribute(classOf[RedirectMap]))
        .filter(_.generation == generation) getOrElse {
        logger.info(s"Building redirect map ${env.getIdentifier}")
        val redirects = folder.queryRedirects
          .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.ne, true)
          .getComponents[Redirect]
          .map(_.getRedirects)
          .fold(Map.empty)(_ ++ _)
        RedirectMap(generation, redirects) <| {
          env.setAttribute(classOf[RedirectMap], _)
        }
      }
    }
end RedirectFilter

object RedirectFilter:
  private final val logger = org.log4s.getLogger

  case class RedirectMap(generation: Long, redirects: Map[String, String])

  object RedirectMap:
    val empty = RedirectMap(0L, Map.empty)
