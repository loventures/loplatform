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

package loi.cp.async

import com.learningobjects.cpxp.async.async.AsyncRouter
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.AsyncEventBinding
import loi.cp.sse.AbstractSSEServlet

import java.util.logging.Logger
import scala.concurrent.ExecutionContext

@Component
@AsyncEventBinding(path = "/async")
class AsyncSSEComponent(val componentInstance: ComponentInstance)(implicit
  ec: ExecutionContext
) extends AbstractSSEServlet[AsyncSSEComponent]:
  override def routers = Seq(AsyncRouter.routerBroadcastGroup.path)

  override def logger = AsyncSSEComponent.logger

object AsyncSSEComponent:
  val logger = Logger.getLogger(classOf[AsyncSSEComponent].getName)
