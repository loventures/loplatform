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

package loi.cp.localmail

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import scalaz.\/

@Service
@Controller(value = "localmail", root = true)
@Secured(allowAnonymous = true)
@RequestMapping(path = "localmail")
trait LocalmailWebController extends ApiRootComponent:

  @RequestMapping(method = Method.GET)
  def getAllLocalmails: ErrorResponse \/ ArgoBody[Map[String, List[Localmail]]]

  @RequestMapping(path = "{email}", method = Method.GET)
  def getLocalmails(
    @PathVariable("email") email: String,
  ): ErrorResponse \/ ArgoBody[List[Localmail]]

  @RequestMapping(path = "{email}/{id}", method = Method.GET)
  def getLocalmail(
    @PathVariable("email") email: String,
    @PathVariable("id") id: Long,
  ): ErrorResponse \/ ArgoBody[Localmail]

  @RequestMapping(path = "{email}/{id}/attachments/{attachment}", method = Method.GET)
  def getAttachment(
    @PathVariable("email") email: String,
    @PathVariable("id") id: Long,
    @PathVariable("attachment") attachment: Long,
  ): ErrorResponse \/ FileResponse[?]

  @RequestMapping(path = "{email}/{id}", method = Method.DELETE)
  def deleteLocalmail(
    @PathVariable("email") email: String,
    @PathVariable("id") id: Long
  ): ErrorResponse \/ Unit

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "settings", method = Method.GET)
  def getSettings: LocalmailWebController.Settings

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "settings", method = Method.PUT)
  def setSettings(@RequestBody settings: LocalmailWebController.Settings): Unit
end LocalmailWebController

object LocalmailWebController:
  val Domain      = "localmail"
  val AtLocalmail = s"@$Domain"

  case class Settings(
    enabled: Boolean
  )
