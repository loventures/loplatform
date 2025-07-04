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

package loi.cp.reply

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

/** Reply web API. This primarily exists to serve debugging email connectivity and integration testing.
  */
@Controller(value = "replies", root = true)
@RequestMapping(path = "replies")
@Secured(Array(classOf[AdminRight]))
trait ReplyRootApi extends ApiRootComponent:

  /** Gets an email reply processed by the system.
    * @param id
    *   the reply id
    * @return
    *   the reply
    */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[ReplyReceipt]

  /** Gets email replies processed by the system.
    * @param q
    *   the request query
    * @return
    *   the resulting reply receipts
    */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[ReplyReceipt]

  /** Submits a reply email to the reply service.
    *
    * @param reply
    *   a representation of the reply email
    */
  @RequestMapping(method = Method.PUT)
  def reply(@RequestBody reply: Reply): Unit

  /** Suspend replies to an address.
    *
    * @param address
    *   the local part of the email address
    */
  @RequestMapping(path = "suspend/{address}", method = Method.PUT)
  def suspend(@PathVariable("address") address: String): Unit

  /** Returns whether SMTP email replies are enabled on this system.
    *
    * @return
    *   whether SMTP email replies are enabled
    */
  @RequestMapping(path = "configured", method = Method.PUT)
  def isConfigured: Boolean

  /** Get the reply settings for this domain.
    *
    * @return
    *   the reply settings
    */
  @RequestMapping(path = "settings", method = Method.GET)
  def getSettings: ReplyRootApi.Settings

  /** Get the reply settings for this domain.
    *
    * @return
    *   the reply settings
    */
  @RequestMapping(path = "settings", method = Method.PUT)
  def setSettings(@RequestBody settings: ReplyRootApi.Settings): Unit
end ReplyRootApi

object ReplyRootApi:

  /** Reply settings.
    *
    * @param domainName
    *   an override for the domain address of replies for this domain
    */
  case class Settings(
    domainName: Option[String]
  )
end ReplyRootApi
