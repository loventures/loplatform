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

package loi.cp.shutdown

import org.apache.pekko.actor.ActorSystem
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method.{GET, POST}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, HtmlWriter}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.shutdown.ShutdownActor
import loi.cp.overlord.OverlordRight
import loi.cp.right.RightService
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*

import scala.concurrent.duration.*

/** Provides support for shutting the cluster down.
  */
@Component
@ServletBinding(path = ShutdownServlet.ControlShutdown)
class ShutdownServlet(val componentInstance: ComponentInstance)(implicit
  actorSystem: ActorSystem,
  rs: RightService,
  user: () => UserDTO
) extends ServletComponent
    with ServletDispatcher
    with ComponentImplementation:
  import ServletDispatcher.*
  import ShutdownServlet.*

  /** Check overlord access. */
  override protected def checkAccess: WebResponse \/ Unit =
    rs.getUserHasRight(classOf[OverlordRight]) either (()) or ErrorResponse.notFound

  override def handler: RequestHandler = {
    case RequestMatcher(GET, ControlShutdown, _, _) =>
      HtmlResponse(this, "shutdown.html").right

    case RequestMatcher(POST, ControlShutdown, _, _) =>
      logger.error(s"Cluster shutdown initiated by ${user.userName}")
      // I ought to have a two-phase commit protocol in places of hoping that 2 seconds is long enough to get the message out.
      ShutdownActor.broadcastShutdownRequest(2.seconds)
      TextResponse.json(true.toString).right

    case RequestMatcher(GET, ControlShutdownPoll, _, _) =>
      // Just wait for Tomcat to shutdown
      HtmlResponse((_: HtmlWriter) => Thread.sleep(30.seconds.toMillis)).right
  }
end ShutdownServlet

object ShutdownServlet:
  private final val logger = org.log4s.getLogger

  /** The URL this binds to. */
  final val ControlShutdown = "/control/shutdown"

  /** Poll for shutdown. */
  final val ControlShutdownPoll = s"$ControlShutdown/poll"
