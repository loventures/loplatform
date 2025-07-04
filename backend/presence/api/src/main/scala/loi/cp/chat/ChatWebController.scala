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

package loi.cp.chat

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*

import scalaz.\/

/** Chat API.
  */
@Controller(value = "chats", root = true)
@RequestMapping(path = "chats")
trait ChatWebController extends ApiRootComponent:
  import ChatWebController.*

  /** Open a chat room.
    * @param chatType
    *   the chat room type
    * @return
    *   the chat room or an error
    */
  @RequestMapping(method = Method.POST)
  def openChat(@RequestBody chatType: ChatType): ErrorResponse \/ ChatRoom

  /** Get a chat room. */
  @RequestMapping(path = "{id}", method = Method.GET)
  def getChat(@PathVariable("id") id: Long): ErrorResponse \/ ChatRoom

  /** Get chat room messages. */
  @RequestMapping(path = "{id}/messages", method = Method.GET)
  def getMessages(@PathVariable("id") id: Long, query: ApiQuery): ErrorResponse \/ ApiQueryResults[ChatMessage]

  /** Send a chat message. */
  @RequestMapping(path = "{id}/messages", method = Method.POST)
  def sendMessage(@PathVariable("id") id: Long, @RequestBody chat: Chat): ErrorResponse \/ Unit
end ChatWebController

/** Chat web controller companion.
  */
object ChatWebController:

  /** A simple chat message.
    *
    * @param message
    *   the chat message
    * @param typing
    *   whether the user is currently typing
    */
  case class Chat(message: Option[String], typing: Option[Boolean])
  // At some point we might want to separate the Chat typing signaling from payload delivery

  /** Model of a chat room.
    * @param id
    *   the room identifier
    * @param chatType
    *   the type of chat root
    */
  case class ChatRoom(id: Long, chatType: ChatType)

  /** Chat room type.
    */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
  @JsonSubTypes(
    Array(
      new Type(name = "user", value = classOf[UserChat]),
      new Type(name = "context", value = classOf[ContextChat]),
      new Type(name = "branch", value = classOf[BranchChat])
    )
  )
  @JsonSerialize
  sealed trait ChatType

  /** A chat with a single user. */
  case class UserChat(handle: String) extends ChatType

  /** A chat in a course/context. */
  case class ContextChat(context: Long) extends ChatType

  /** A chat in a branch. */
  case class BranchChat(branch: Long) extends ChatType
end ChatWebController
