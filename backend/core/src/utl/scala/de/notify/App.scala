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

package de.notify

import cats.effect.*
import cats.syntax.all.*
import de.common.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import org.http4s.Uri
import org.http4s.circe.*
import org.http4s.implicits.*
import scalaz.syntax.std.option.*
import scopt.OptionParser

/** Endpoint to notify user of build success, build failure, or a selenide test result. Notification by slack.
  *
  * core/utl:runMain de.notify.App -m success -e sjafri@learningobjects.com
  */
object App extends IOApp:

  private val forwardto = Map(
    "jdunsworth@learningobjects"      -> "jeremy@lo.ventures",
    "merlin@learningobjects.com"      -> "merlin@lo.ventures",
    "mfirtion@learningobjects.com"    -> "matt@lo.ventures",
    "nvanaartsen@learningobjects.com" -> "nick@lo.ventures",
    "pryan@learningbojects.com"       -> "patrick@lo.ventures",
    "sjordan@learningobjects.com"     -> "stephen@lo.ventures"
  )

  override def run(args: List[String]): IO[ExitCode] =
    notify[IO](args).map(_ => ExitCode.Success)

  def notify[F[_]: Async](args: List[String]): F[Unit] =
    for
      params     <- parser.parse(args, NotifyParams()).liftTo[F](OptUnspecified("malformed"))
      slackToken <- getenv[F]("SLACK_TOKEN")
      slackhttp   = SlackHttp(slackuri, slackToken, SlackHttp.defaultClient[F])
      _          <- slackhttp.use(notifyBySlack(forwardto.getOrElse(params.recipient, params.recipient), params.message)(_))
    yield println("diffused spores")

  def notifyBySlack[F[_]: Async](recipient: String, message: String)(slackHttp: SlackHttp[F]): F[Unit] =
    for
      channelIdOpt <- findChannel(recipient)(slackHttp)
      _            <- channelIdOpt.cata(slackUser(_, message)(slackHttp), Sync[F].delay(println(s"Failed")))
    yield ()

  def findChannel[F[_]: Async](recipient: String)(slackHttp: SlackHttp[F]): F[Option[String]] =
    val path = if recipient `contains` "@" then "/api/users.list" else "/api/conversations.list"
    slackHttp.client
      .expect[Json](slackHttp.post(slackHttp.slackuri.withPath(Uri.Path.unsafeFromString(path))))
      .flatMap(json =>
        if recipient `contains` "@" then
          json
            .as_![SlackUserList](s"I no work ${json.spaces4}")
            .liftTo[F]
            .map(_.members.find(_.profile.email.contains(recipient)).map(_.id))
        else
          json
            .as_![SlackChannelList](s"I no work ${json.spaces4}")
            .liftTo[F]
            .map(_.channels.find(_.name.equals(recipient)).map(_.id))
      )
  end findChannel

  def slackUser[F[_]: Async](channelId: String, message: String)(slackHttp: SlackHttp[F]): F[Unit] =
    slackHttp.client
      .expect[Json](
        slackHttp
          .post(slackHttp.slackuri.withPath(Uri.Path.unsafeFromString("/api/chat.postMessage")))
          .withEntity({
            SlackMessage(channelId, message, as_user = true).asJson
          })
      )
      .void

  final case class SlackMessage(
    channel: String,
    text: String,
    as_user: Boolean
  )

  implicit val messageEncoder: Encoder[SlackMessage] = deriveEncoder[SlackMessage]

  final case class NotifyParams(
    recipient: String = "",
    message: String = "",
    result: Option[String] = None
  )

  final case class Profile(email: Option[String])

  implicit val profileDecoder: Decoder[Profile] = deriveDecoder

  final case class Member(id: String, profile: Profile)

  final case class Channel(id: String, name: String)

  implicit val memberDecoder: Decoder[Member] = deriveDecoder

  implicit val channelDecoder: Decoder[Channel] = deriveDecoder

  final case class SlackUserList(members: List[Member])

  final case class SlackChannelList(channels: List[Channel])

  implicit val slackUserListDecoder: Decoder[SlackUserList] = deriveDecoder

  implicit val slackChannelListDecoder: Decoder[SlackChannelList] = deriveDecoder

  private final val parser = new OptionParser[NotifyParams]("Notifier"):
    head("notify", "0.1-SNAPSHOT", "Notifies users of build status. Success and failure.")
    opt[String]('e', "recipient").required() text "Which user or channel to notify" action { (recipient, params) =>
      params.copy(recipient = recipient)
    }
    opt[String]('m', "message").required() text "Message to send 🔨" action { (message, params) =>
      params.copy(message = message)
    }
    opt[String]('r', "result") text "Any extra results you want appended to the notification" action {
      (method, params) =>
        params.copy(result = Some(method))
    }

  private def slackuri: Uri = uri"https://slack.com"
end App
