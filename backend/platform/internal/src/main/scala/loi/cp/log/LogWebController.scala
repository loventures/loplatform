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

import argonaut.{CodecJson, Json}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, Method}
import com.learningobjects.de.authorization.Secured

import scala.util.Try

@Controller(value = "logging", root = true)
trait LogWebController extends ApiRootComponent:
  import LogWebController.*

  @RequestMapping(path = "log/info", method = Method.POST, csrf = false)
  @Secured(allowAnonymous = true)
  def log(@RequestBody logMessage: ArgoBody[LogEntry]): Try[Unit]

object LogWebController:

  final case class LogEntry(
    message: String,
    payload: Option[Json],
  )

  implicit val logEntryCodec: CodecJson[LogEntry] = CodecJson.derive[LogEntry]
