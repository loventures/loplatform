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

package loi.cp.system

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.filter.CurrentFilter
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.misc.Extractor

import java.util.Locale

@ServletBinding(path = LocaleServlet.Path, transact = false)
@Component
class LocaleServlet(val componentInstance: ComponentInstance)
    extends ServletComponent
    with ComponentImplementation
    with ServletDispatcher:
  import LocaleServlet.*
  import ServletDispatcher.*

  protected def handler: RequestHandler = {
    case RequestMatcher(Method.GET, QuickSetPath(locale), _, _) =>
      autopost(Path, Map(Parameter -> locale)).right
    case RequestMatcher(Method.GET, Path, _, _)                 =>
      HtmlResponse(this, "locale.html").right
    case RequestMatcher(Method.POST, Path, req, rsp)            =>
      for
        newLocaleStr <- Option(req.getParameter(Parameter)) \/> ErrorResponse.badRequest
        newLocale     = Locale.forLanguageTag(newLocaleStr)
        _            <- (newLocale != Locale.ROOT) either (()) or ErrorResponse.validationError(Parameter, newLocaleStr)(
                          "Bad locale."
                        )
      yield
        req.getSession.setAttribute(CurrentFilter.SESSION_ATTRIBUTE_LOCALE, newLocale)
        NoContentResponse
  }
end LocaleServlet

object LocaleServlet:
  final val Path      = "/sys/locale"
  final val Parameter = "locale"

  val QuickSetPath = Extractor `dropPrefix` (Path + "/")
