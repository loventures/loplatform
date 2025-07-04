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

package de.common

import cats.effect.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.syntax.all.*
import cats.effect.{Async, Sync}
import cats.syntax.all.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.http4s.circe.*
import org.http4s.client.UnexpectedStatus
import org.http4s.{Response, Status, Uri}

object Jira:
  private final val logger = org.log4s.getLogger

  final val TicketRe = "[A-Z][A-Z0-9]{2,8}-[0-9]{1,6}".r

  def url[F[_]](jiraUri: Uri)(key: String): String =
    s"${jiraUri}browse/$key"

  // Kind Reader:
  // If you wind up here, debugging a sporadic 401 unauthorized from Jira, double
  // check that you are authenticating as an internal Jira user and not as a user
  // backed by an external directory. The repeated authentication triggered by
  // this app causes JumpCloud to go sad and randomly report auth failures.
  def getTicket[F[_]: Async](key: String): Klient[F, Option[Ticket]] =
    for
      http   <- Klient.ask[F]
      uri     = ticketUri(http.jiraUri)(key)
      _      <- Klient.delay(logger info s"GET $uri")
      ticket <- Klient.liftF(expect404(getTicket[F](http, uri), uri))
      _      <- Klient.delay(logger info s"$uri: 200 ${ticket.map(_.key)}")
    yield ticket

  def getTicket[F[_]: Async](http: Http[F], ticketUri: Uri): F[Ticket] =
    http.client
      .expect[Ticket](http.get(ticketUri))(using jsonOf[F, Ticket])

  def expect404[F[_]: Sync, A](fa: F[A], uri: Uri): F[Option[A]] =
    fa.map(_.some)
      .recoverWith({ case UnexpectedStatus(Status.NotFound, _, _) =>
        Sync[F].delay(logger info s"$uri: 404") *> Sync[F].pure(None)
      })
      .onError({ case e =>
        Sync[F].delay(logger.warn(s"Error getting $uri"))
      })

  def requireTicket[F[_]: Async](key: String): Klient[F, Ticket] =
    getTicket(key).flatMap(_.liftTo[Klient[F, *]](TicketNotFound(key): Throwable))

  def postComment[F[_]: Async](key: String, body: String): Klient[F, Comment] =
    val comment                                     = Comment(id = None, body = body)
    def doPost(http: Http[F], uri: Uri): F[Comment] =
      http.client.expect[Comment](http.post(uri).withEntity(comment.asJson))(using jsonOf[F, Comment])
    for
      http    <- Klient.ask[F]
      uri      = commentUri(http.jiraUri)(key)
      comment <- Klient.liftF(logAction(doPost(http, uri), s"adding comment($uri)"))
    yield comment

  def transition[F[_]: Sync](key: String, tid: String, resolution: Option[String]): Klient[F, Unit] =
    val transition                      = Transition(TransitionId(tid), resolution.map(r => "resolution" -> JiraName(r)).toMap)
    def doPost(http: Http[F], uri: Uri) =
      http.client
        .run(http.post(uri).withEntity(transition.asJson))
        .use(
          logResponse(_, s"POST $uri ${transition.asJson.noSpaces}")
        )
    for
      http <- Klient.ask[F]
      uri   = transitionsUri(http.jiraUri)(key)
      _    <- Klient.liftF(doPost(http, uri))
    yield ()
  end transition

  def setDescription[F[_]: Sync](key: String, description: String): Klient[F, Unit] =
    updateTicket(key, "description" -> description)

  def setExternalParent[F[_]: Sync](key: String, parent: String): Klient[F, Unit] =
    updateTicket(key, "customfield_10204" -> parent)

  def setAssignee[F[_]: Sync](key: String, ass: String): Klient[F, Unit] =
    val ticket                         = TicketAssign(ass)
    def doPut(http: Http[F], uri: Uri) =
      logAction(
        failStatus(http.client.status(http.put(uri).withEntity(ticket.asJson)))(_.code != 204),
        s"assigning $uri"
      )
    for
      http <- Klient.ask[F]
      uri   = assigneeUri(http.jiraUri)(key)
      _    <- Klient.liftF(doPut(http, uri))
    yield ()
  end setAssignee

  private def updateTicket[F[_]: Sync](key: String, fields: (String, String)*): Klient[F, Unit] =
    val ticket                         = TicketUpdate(fields.toMap)
    println(ticket.asJson.spaces2)
    def doPut(http: Http[F], uri: Uri) =
      logAction(
        failStatus(http.client.status(http.put(uri).withEntity(ticket.asJson)))(_.code != 204),
        s"updating $uri"
      )
    for
      http <- Klient.ask[F]
      uri   = ticketUri(http.jiraUri)(key)
      _    <- Klient.liftF(doPut(http, uri))
    yield ()
  end updateTicket

  def unwatch[F[_]: Sync](key: String): Klient[F, Unit] =
    for
      http <- Klient.ask[F]
      uri   = watchersUri(http.jiraUri)(key).+?("username", http.user)
      _    <- Klient.liftF(logAction(failStatus(http.client.status(http.delete(uri)))(_.code != 204), s"unwatching $uri"))
    yield ()

  private def logAction[F[_]: Sync, A](action: F[A], name: String): F[A] =
    action guaranteeCase {
      case Succeeded(_) =>
        Sync[F].delay(logger info s"$name: 200)")
      case Errored(th)  =>
        Sync[F].delay(logger.warn(s"Error $name")) *>
          Sync[F].delay(logger.warn(th.getMessage))
      case Canceled()   =>
        Sync[F].unit
    }

  private def logResponse[F[_]: Sync](response: Response[F], actionName: String): F[Unit] =
    if response.status.isSuccess then Sync[F].delay(logger info s"$actionName: ${response.status.code}")
    else
      Sync[F].delay(logger.warn(s"Error $actionName\n")) *>
        response.bodyText.compile.string.flatMap(s => Sync[F].raiseError(new Exception("Transition failure: " + s)))

  private def failStatus[F[_]: Sync](response: F[Status])(f: Status => Boolean): F[Unit] =
    response.flatMap({ status =>
      if f(status) then Sync[F].raiseError(new Exception(s"Oh noes: $status"))
      else Sync[F].unit
    })

  private def ticketUri(jiraUri: Uri)(key: String): Uri =
    jiraUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/2/issue/$key"))

  private def commentUri(jiraUri: Uri)(key: String): Uri =
    jiraUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/2/issue/$key/comment"))

  private def transitionsUri(jiraUri: Uri)(key: String): Uri =
    jiraUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/2/issue/$key/transitions"))

  private def watchersUri(jiraUri: Uri)(key: String): Uri =
    jiraUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/2/issue/$key/watchers"))

  private def assigneeUri(jiraUri: Uri)(key: String): Uri =
    jiraUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/2/issue/$key/assignee"))

  final case class Ticket(
    id: Long,
    key: String,
    fields: Fields
  )
  given Decoder[Ticket]                                   = Decoder.derived[Ticket]

  final case class Fields(
    issuetype: IssueType,
    status: JiraName,
    description: Option[String],
    summary: String,
    issuelinks: List[IssueLink]
  )
  given Decoder[Fields] = Decoder.derived[Fields]

  final case class IssueLink(
    `type`: JiraName,
    outwardIssue: Option[TicketRef],
    inwardIssue: Option[TicketRef]
  )
  given Decoder[IssueLink] = Decoder.derived[IssueLink]

  final case class TicketRef(
    id: Long,
    key: String,
    fields: TicketRefFields
  )
  given Decoder[TicketRef] = Decoder.derived[TicketRef]

  final case class TicketRefFields(
    issuetype: IssueType,
    summary: String,
    `type`: Option[JiraName],
//    outwardIssue: Option[TicketRef],
    //   inwardIssue: Option[TicketRef]
  )
  given Decoder[TicketRefFields] = Decoder.derived[TicketRefFields]

  final case class Comment(
    id: Option[String],
    body: String
  )
  given Encoder[Comment] = Encoder.derived[Comment]
  given Decoder[Comment] = Decoder.derived[Comment]

  final case class Transition(
    transition: TransitionId,
    fields: Map[String, JiraName],
  )
  given Encoder[Transition] = Encoder.derived[Transition]

  final case class IssueType(name: String)
  given Decoder[IssueType] = Decoder.derived[IssueType]

  final case class TransitionId(id: String)
  given Encoder[TransitionId] = Encoder.derived[TransitionId]

  final case class TicketUpdate(fields: Map[String, String])
  given Encoder[TicketUpdate] = Encoder.derived[TicketUpdate]

  final case class TicketAssign(name: String)
  given Encoder[TicketAssign] = Encoder.derived[TicketAssign]

  final case class JiraName(name: String)
  given Encoder[JiraName] = Encoder.derived[JiraName]
  given Decoder[JiraName] = Decoder.derived[JiraName]

  private final case class TicketNotFound(key: String) extends Exception(s"$key not found")
end Jira
