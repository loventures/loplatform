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

package loi.cp.message

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured

@Controller(value = "messages", root = true)
@RequestMapping(path = "messages")
@Secured // automatically restricted to current user
trait MessageRootApi extends ApiRootComponent:

  /** Query your messages. */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[Message]

  /** Get a message. */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Message]

  /** Count your messages. This is technically redundant, equivalent to prefilter + zero limit, but is maybe useful? */
  @RequestMapping(path = "count", method = Method.GET)
  def count(q: ApiQuery): Long // TODO: KILLME?

  /** Post a new message. */
  @RequestMapping(path = "send", method = Method.POST)
  def send(@RequestBody message: NewMessage): Message

  /** Reply to a message. */
  @RequestMapping(path = "reply/{id}", method = Method.POST)
  def reply(@PathVariable("id") id: Long, @RequestBody message: NewMessage): Message

  // TODO: Querying threads instead of messages
end MessageRootApi
