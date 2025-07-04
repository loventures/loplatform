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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.chat.ChatConstants.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{Comparison, Direction}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.project.ProjectService
import loi.cp.content.ContentAccessService
import loi.cp.presence.{PresenceService, SceneActor}
import loi.cp.web.HandleService
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.ʈry.*

import java.util.Date
import scala.jdk.CollectionConverters.*

/** Implementation of the chat root API.
  *
  * NOTES: If we wanted to flag who has a particular chat room open, we could model that using presence-in-scene for
  * scene id = chat room. Similarly we could scope busy signals to that scene to allow typing signals without excess
  * overhead.
  *
  * @param componentInstance
  *   the component instance
  */
@Component
class ChatWebControllerImpl(val componentInstance: ComponentInstance)(
  domain: DomainDTO,
  now: Date,
  ps: PresenceService,
  projectService: ProjectService,
  contentAccessService: ContentAccessService,
  user: UserDTO,
)(implicit hs: HandleService, fs: FacadeService)
    extends ChatWebController
    with ComponentImplementation:

  import ChatWebController.*
  import ChatWebControllerImpl.*

  /** Open a chat room. Returns an existing chat room if it exists, or else creates a new one.
    * @param chatType
    *   the chat room type
    * @return
    *   the chat room or an error
    */
  override def openChat(chatType: ChatType): ErrorResponse \/ ChatRoom =
    for roomId <- chatRoomId(chatType)
    yield ChatRoom(chatDomain.getOrCreateChatRoom(roomId).getId, chatType)

  /** Convert a chat room type into the corresponding chat room identifier. This is a convenient, albeit imperfect way
    * of finding chat rooms.
    * @param chatType
    *   the chat room type
    * @return
    *   the chat room identifier
    */
  private def chatRoomId(chatType: ChatType): ErrorResponse \/ String = chatType match
    case UserChat(handle)     =>
      for uid <- hs.unmask(handle) \/> ErrorResponse.badRequest
      yield List[Long](user.getId, uid).sorted.mkString("user:", ":", "")
    case ContextChat(context) =>
      for _ <- contentAccessService.getCourseAsLearner(context, user) \/>| ErrorResponse.unauthorized
      yield s"context:$context"
    case BranchChat(branch)   =>
      for _ <- projectService.loadBronch(branch) \/> ErrorResponse.unauthorized
      yield s"branch:$branch"

  /** Get a chat room by PK.
    * @param id
    *   the chat room PK
    * @return
    *   the chat room, or an error
    */
  override def getChat(id: Long): ErrorResponse \/ ChatRoom =
    for room <- chatRoom(id)
    yield ChatRoom(id, chatType(room))

  /** Convert a chat room identifier to a chat room type. This returns a value that the front-end can easily intepret.
    * @param room
    *   the chat room
    * @return
    *   the chat room type
    */
  private def chatType(room: ChatRoomFacade): ChatType = room.getRoomId match
    case ContextChatRoomId(id)  => ContextChat(id)
    case UserChatRoomId(u0, u1) => UserChat(hs.mask((Set(u0, u1) - myself).head))
    case BranchChatRoomId(id)   => BranchChat(id)

  /** Get the history of a chat room.
    * @param id
    *   the chat room PK
    * @param query
    *   filtering parameters
    * @return
    *   the chat messages, or an error
    */
  override def getMessages(id: Long, query: ApiQuery): ErrorResponse \/ ApiQueryResults[ChatMessage] =
    for room <- chatRoom(id)
    yield
      // Not bothering with caching under the assumption that it's rarely useful.
      val messages = chatDomain.queryChatMessages
        .addCondition(DATA_TYPE_CHAT_MESSAGE_ROOM, Comparison.eq, room)
        .setOrder(DATA_TYPE_CHAT_MESSAGE_TIMESTAMP, Direction.DESC)
        .setLimit(query.getPage.getLimitOr(DefaultHistory) max MaxHistory)
        .setFirstResult(query.getPage.getOffset)
        .getFacades[ChatMessageFacade]
        .map(toChatMessage)
      // I don't want to offer a totalCount. There should be a different
      // query results container for when we want infinite scroll-like behaviour
      new ApiQueryResults(messages.asJava, null, null)

  /** Send a message to a chat room.
    *
    * @param id
    *   the chat room
    * @param chat
    *   the chat message
    * @return
    *   success or an error
    */
  override def sendMessage(id: Long, chat: Chat): ErrorResponse \/ Unit =
    for room <- chatRoom(id)
    yield
      val msgId = chat.message map { message =>
        logger info s"Chat to room $id: $message"
        val msg = chatDomain addChatMessage { chat =>
          chat.setRoomId(id)
          chat.setSenderId(user.getId)
          chat.setTimestamp(now)
          chat.setBody(message)
        }
        msg.getId.longValue
      }
      deliverMessage(room, ChatMessage(msgId, hs.mask(user), id, Some(now), chat.message, chat.typing))

  /** Deliver a chat message to the members of a chat room.
    *
    * @param room
    *   the chat room
    * @param msg
    *   the chat message
    */
  private def deliverMessage(room: ChatRoomFacade, msg: ChatMessage): Unit = room.getRoomId match
    case UserChatRoomId(user0, user1) =>
      ps.deliverToUsers(msg)(user0, user1)
    case ContextChatRoomId(course)    =>
      ps.deliverToScene(msg)(SceneActor.InContext(course))
    case BranchChatRoomId(branch)     =>
      ps.deliverToScene(msg)(SceneActor.InBranch(branch, None))

  /** Look up a chat room by PK if you have access.
    *
    * @param id
    *   the chat room id
    * @return
    *   the chat room facade, or an error if not found or you do not have access
    */
  private def chatRoom(id: Long): ErrorResponse \/ ChatRoomFacade =
    for
      room <- chatDomain.getChatRoom(id) \/> ErrorResponse.notFound
      _    <- canAccess(room) \/> ErrorResponse.unauthorized
    yield room

  /** Test whether the current user can access a chat room. */
  private def canAccess(room: ChatRoomFacade): Boolean = room.getRoomId match
    case UserChatRoomId(u0, u1)     => (u0 == myself) || (u1 == myself)
    case ContextChatRoomId(context) => contentAccessService.getCourseAsLearner(context, user).isSuccess
    case BranchChatRoomId(branch)   => projectService.loadBronch(branch).isDefined

  /** Map a chat message facade to a chat message POJO. */
  def toChatMessage(msg: ChatMessageFacade): ChatMessage =
    ChatMessage(
      Some(msg.getId),
      hs.mask(msg.getSenderId),
      msg.getRoomId,
      Some(msg.getTimestamp),
      Some(msg.getBody),
      None
    )

  /** Get the chat domain facade. */
  private def chatDomain = domain.facade[ChatDomainFacade]

  /** Get the current user's PK. */
  private final def myself = user.getId.longValue
end ChatWebControllerImpl

object ChatWebControllerImpl:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** Default number of historic messages to return. */
  private final val DefaultHistory = 20

  /** Maximum number of historic messages to return. */
  private final val MaxHistory = 100

  /** Pattern matching on context room identifiers. */
  private object ContextChatRoomId:
    private final val CourseRegex = """context:(\d+)""".r

    /** Match a context room identifier to the context id. */
    def unapply(roomId: String): Option[Long] = roomId match
      case CourseRegex(id) => Some(id.toLong)
      case _               => None

  /** Pattern matching on user to user chat room identifiers. */
  private object UserChatRoomId:
    private final val DirectRegex = """user:(\d+):(\d+)""".r

    /** Match a user room identifier to the pair of user ids. */
    def unapply(roomId: String): Option[(Long, Long)] = roomId match
      case DirectRegex(u0, u1) => Some(u0.toLong -> u1.toLong)
      case _                   => None

  /** Pattern matching on branch room identifiers. */
  private object BranchChatRoomId:
    private final val BranchRegex = """branch:(\d+)""".r

    /** Match a context room identifier to the context id. */
    def unapply(roomId: String): Option[Long] = roomId match
      case BranchRegex(id) => Some(id.toLong)
      case _               => None
end ChatWebControllerImpl
