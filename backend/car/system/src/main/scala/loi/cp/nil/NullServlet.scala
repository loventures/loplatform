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

package loi.cp.nil

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.ServletDispatcher.RequestHandler
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import scalaz.syntax.either.*

/** This servlet just logs and ignores all request. The purpose is to give a valid action to forms that use javascript
  * submission such that when machines poke at the forms we don't get large exception traces logged. Case in point,
  * automated comment spam.
  */
@Component
@ServletBinding(path = "/dev/null", system = true)
class NullServlet(val componentInstance: ComponentInstance)
    extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:

  override def handler: RequestHandler = { case _ =>
    NoContentResponse.right
  }
end NullServlet
