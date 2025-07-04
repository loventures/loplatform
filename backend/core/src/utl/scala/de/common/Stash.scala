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

import cats.Foldable
import cats.effect.Outcome.Errored
import cats.effect.syntax.all.*
import cats.effect.{Async, IO, Sync}
import cats.instances.list.*
import cats.instances.set.*
import cats.syntax.all.*
import de.common.Klient.*
import fs2.io.file.{Files, Path}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.http4s.{EntityDecoder, EntityEncoder, Headers, MediaRange, Uri}
import scaloi.=∂>
import scaloi.syntax.any.*
import scaloi.syntax.option.*

import java.io.File

object Stash:
  private final val logger = org.log4s.getLogger

  final val Bfr        = "bfr"
  private final val De = "DE"

  // old Stash [[Foo Bar]](url)
  // new Stash ![Foo Bar](url)
  final val ImageRE = """!\[([^]]*)]\(([^)]*)\)""".r

  def getPR[F[_]: Async](id: Long): Klient[F, PullRequest] =
    val uri = prUri(Bfr, id)
    for
      http <- ask[F]
      json <- liftF(http.client.expect[Json](http.get(uri)))
      pr   <- liftF(json.as_![PullRequest](errMsg(uri)).map(pr => pr.copy(json = json.some)).liftTo[F])
    yield pr

  def getPRs[F[_]: Async](count: Int): Klient[F, List[PullRequest]] =
    val uri = prsUri(Bfr)
    for
      jsons <- getPaged(uri, count)
      prs   <- jsons.traverse(json =>
                 json.as_![PullRequest](errMsg(uri, count)).map(pr => pr.copy(json = json.some)).liftTo[Klient[F, *]]
               )
    yield prs

  def getCommits[F[_]: Async](pr: Long, count: Int): Klient[F, List[Commit]] =
    val uri = commitsUri(Bfr, pr)
    for
      jsons <- getPaged(uri, count)
      prs   <- liftF(
                 jsons
                   .traverse(json => json.as_![Commit](errMsg(uri, count)).map(pr => pr.copy(json = json.some)).liftTo[F])
               )
    yield prs

  def getActivities[F[_]: Async](pr: Long, count: Int): Klient[F, List[Activity]] =
    val uri = activitiesUri(Bfr, pr)
    for
      jsons <- getPaged(uri, count)
      prs   <- liftF(jsons.traverse(json => json.as_![Activity](errMsg(uri, count)).liftTo[F]))
    yield prs

  def createPr[F[_]: Async](title: String, fromRef: Ref, toRef: Ref): Klient[F, PullRequest] =
    val uri  = createPrUri(Bfr)
    val post = PullRequestPostDto(title, fromRef, toRef).asJson

    for
      http <- ask[F]
      json <- liftF(logError(http.client.expect[Json](http.post(uri).withEntity(post)), uri))
      pr   <- liftF(json.as_![PullRequest](errMsg(uri)).liftTo[F])
    yield pr

  def logError[F[_]: Sync, A](fa: F[A], uri: Uri): F[A] = fa.guaranteeCase {
    case Errored(e) =>
      Sync[F].delay(logger.warn(s"Error getting $uri ${e.getMessage}"))
    case _          =>
      Sync[F].unit
  }

  def mergePr[F[_]: Async](pr: PullRequest): Klient[F, PullRequest] =
    val uri = mergePrUri(Bfr, pr.id, pr.version)

    for
      http <- ask[F]
      json <- liftF(logError(http.client.expect[Json](http.post(uri)), uri))
      pr   <- liftF(json.as_![PullRequest](errMsg(uri)).liftTo[F])
    yield pr

  def changePrSettings[F[_]: Async](settings: PrSettings): Klient[F, PrSettings] =
    val uri = prSettingsUri(Bfr)
    for
      http <- ask[F]
      json <- liftF(logError(http.client.expect[Json](http.post(uri).withEntity(settings.asJson)), uri))
      pr   <- liftF(json.as_![PrSettings](errMsg(uri)).liftTo[F])
    yield pr

  def downloadAttachment[F[_]: Async](href: String): Klient[F, (File, Headers)] =
    val uri = Uri.unsafeFromString(href)
    logger info s"GET $uri"
    for
      file    <- liftF(Sync[F].delay(File.createTempFile("stash", "bin") <| { file =>
                   file.deleteOnExit()
                 }))
      http    <- Klient.ask[F]
      path     = Path.fromNioPath(file.toPath)
      decoder  = EntityDecoder.decodeBy[F, Headers](MediaRange.`*/*`) { msg =>
                   implicit val files: Files[F] = Files.forAsync[F]
                   EntityDecoder.binFile(path).decode(msg, strict = true).as(msg.headers)
                 }
      headers <- liftF(http.client.expect[Headers](http.get(uri))(using decoder))
    yield file -> headers
    end for
  end downloadAttachment

  def linkAttachments(desc: String): String =
    desc
      .replaceAll("attachment:411/", s"${stashUri}rest/api/1.0/projects/$De/repos/$Bfr/attachments/")
      .replace("%2F", "/")
      .replace("+", "%20")

  def scrapeTicketKeys(pr: PullRequest, commits: List[Commit], comments: List[String]): Set[String] =
    val (add, remove) = comments.foldLeft((Set.empty[String], Set.empty[String])) {
      case ((a, r), Command.Link(ticket))   => (a ++ ticket.split("\\s+"), r)
      case ((a, r), Command.Unlink(ticket)) => (a, r ++ ticket.split("\\s+"))
      case (o, _)                           => o
    }
    Foldable[List].fold(commits.flatMap(cs => cs.json.map(scrapeKeys)) ++ pr.json.map(scrapeKeys)) ++ add -- remove

  def scrapeKeys(json: Json): Set[String] =
    TicketRe.findAllMatchIn(json.noSpaces).map(_.matched).toSet

  final val TicketRe = s"${`Aha!`.TicketRe.regex}|${Jira.TicketRe.regex}".r

  // deletes seem to just delete, edits seem to just be recorded as added
  def extractComments: Activity =∂> String = {
    case a if a.action == "COMMENTED" && a.commentAction.contains("ADDED") => a.comment.get.text
  }

  private def getPaged[F[_]: Async](uri: Uri, count: Int): Klient[F, List[Json]] =
    def loop(start: Int, jsons: List[Json]): Klient[F, List[Json]] =
      val quri: Uri = uri.+?("start", start).+?("limit", count - jsons.size)
      logger info s"GET $quri"
      for
        http     <- ask[F]
        json     <- liftF(
                      http.client
                        .expect[Json](http.get(quri))
                        .onError({ case e => logger.warn(s"Error getting $quri"); Sync[F].raiseError(e) })
                    )
        result    = jsons ++ json.asObject.get("values").get.asArray.get
        newJsons <- (if (result.size >= count) || json.asObject.get("isLastPage").exists(_.asBoolean.isTrue) then
                       liftF(Sync[F].point(result))
                     else loop(json.asObject.get("nextPageStart").get.asNumber.get.toInt.get, result))
      yield newJsons
      end for
    end loop
    loop(0, Nil)
  end getPaged

  def commentsUri(slug: String, pr: Long): Uri =
    repoUri(slug, s"pull-requests/$pr/comments")

  private def commitsUri(slug: String, pr: Long): Uri =
    repoUri(slug, s"pull-requests/$pr/commits")

  private def activitiesUri(slug: String, pr: Long): Uri =
    repoUri(slug, s"pull-requests/$pr/activities")

  private def prUri(slug: String, pr: Long): Uri =
    repoUri(slug, s"pull-requests/$pr").+?("withProperties", true).+?("withAttributes", true).+?("state", "MERGED")

  private def mergePrUri(slug: String, pr: Long, version: Long): Uri =
    repoUri(slug, s"pull-requests/$pr/merge?version=$version")

  private def prSettingsUri(slug: String): Uri =
    repoUri(slug, "settings/pull-requests")

  private def createPrUri(slug: String): Uri =
    repoUri(slug, "pull-requests")

  private def prsUri(slug: String): Uri =
    repoUri(slug, "pull-requests").+?("withProperties", true).+?("withAttributes", true).+?("state", "MERGED")

  private def repoUri(slug: String, suffix: String): Uri =
    stashUri.withPath(Uri.Path.unsafeFromString(s"/rest/api/1.0/projects/$De/repos/$slug/$suffix"))

  def stashUri: Uri =
    uri"https://stash.example.org/"

  private def errMsg(uri: Uri): String             = s"failed to decode $uri"
  private def errMsg(uri: Uri, count: Int): String = s"${errMsg(uri)} (count $count)"

  final case class NewComment(text: String)
  object NewComment:
    implicit val newCommentEntityEncoder: EntityEncoder[IO, NewComment] = jsonEncoderOf[IO, NewComment]

  final case class Commit(id: String, message: String, json: Option[Json])

  final case class PullRequestPostDto(
    title: String,
    fromRef: Ref,
    toRef: Ref,
  )

  final case class PullRequest(
    id: Long,
    title: String,
    description: Option[String],
    version: Long,
    fromRef: Ref,
    toRef: Ref,
    approvers: Option[List[String]],
    links: Links,
    json: Option[Json]
  )
  final case class Ref(id: String)
  final case class Links(self: List[Link])
  final case class Link(href: String)
  final case class Activity(id: Long, action: String, commentAction: Option[String], comment: Option[Comment])
  final case class Comment(id: Long, text: String)
  final case class User(id: Long, name: String)
  final case class Reviewer(user: User, approved: Boolean)
  final case class PrSettings(requiredSuccessfulBuilds: Long, requiredApprovers: Long)
end Stash
