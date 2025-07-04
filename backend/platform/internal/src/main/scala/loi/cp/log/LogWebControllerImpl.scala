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

package loi.cp.log

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ArgoBody
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import de.tomcat.juli.LogMeta

import scala.util.Try

@Component
class LogWebControllerImpl(val componentInstance: ComponentInstance)
    extends LogWebController
    with ComponentImplementation:
  import LogWebController.*
  import LogWebControllerImpl.*

  override def log(argoLogEntry: ArgoBody[LogEntry]): Try[Unit] =
    for entry <- argoLogEntry.decode_!
    yield
      // that is, with shape given by meta
      def metamorphically[A](it: => A): A = entry.payload match
        case None     => it
        case Some(pl) => LogMeta.let("payload" -> pl)(it)
      metamorphically(logger.info(entry.message))
end LogWebControllerImpl

object LogWebControllerImpl:
  private final val logger = org.log4s.getLogger(classOf[LogWebController])
