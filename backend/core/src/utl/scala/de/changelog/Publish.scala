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

package de.changelog

import cats.effect.Async
import cats.syntax.traverse.*
import de.common.{Klient, Stash}
import org.http4s.Uri
import scalaz.std.string.*
import scaloi.syntax.option.*

import java.net.URL

private[changelog] object Publish:

  import de.common.Stash.PullRequest

  def publishImages[F[_]: Async](
    prs: List[PullRequest],
    branch: String,
    name: String,
    s3: S3,
  ): Klient[F, Map[String, URL]] =
    for
      jiraUri   <- Klient.ask[F].map(_.jiraUri)
      images     = prs.flatMap(stashAttachments(_, jiraUri))
      published <- images.traverse(publishImage(_, branch, name, s3))
    yield published.toMap

  private def stashAttachments(pr: PullRequest, jiraUri: Uri): List[String] =
    Stash.ImageRE.findAllMatchIn(pr.description.orZ).toList map { image =>
      Stash.linkAttachments(image.group(2)).replace("+", "%20")
    }

  private def publishImage[F[_]: Async](
    imageUrl: String,
    branch: String,
    name: String,
    s3: S3,
  ): Klient[F, (String, URL)] =
    for
      (file, headers) <- Stash.downloadAttachment(imageUrl)
      url             <- Klient.liftF(s3.publish(file, headers, branch, name))
    yield imageUrl -> url
end Publish
