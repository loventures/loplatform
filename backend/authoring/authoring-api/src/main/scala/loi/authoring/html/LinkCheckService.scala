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

package loi.authoring.html

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.util.Timer
import loi.authoring.index.SearchPath
import loi.authoring.workspace.AttachedReadWorkspace

@Service
trait LinkCheckService:
  def hyperlinkCheck[A](ws: AttachedReadWorkspace, timer: Timer)(
    accept: (Link, LinkStatus) => A
  ): Iterable[A]

final case class Link(href: String, text: String, path: SearchPath)

final case class LinkStatus(code: Int, message: String, redirect: Option[String]):

  /** Is this link status okay with respect to a request URL. */
  def isOkay(url: String): Boolean =
    LinkStatus.isOkay(code) || LinkStatus.isRedirect(code) && redirect.exists(LinkStatus.isOkayRedirect(url, _))

object LinkStatus:

  private def isOkay(sc: Int) = sc == 200

  private def isRedirect(sc: Int) = (sc >= 300) && (sc < 400)

  /** This considers security upgrade redirects, or from www.foo.com to foo.com and vice-versa to be okay. It also
    * considers redirects from a URL ending in / to a child page to be okay; this because many / URLs redirect to
    * /Home.aspx which is a less good URL.
    */
  private def isOkayRedirect(from: String, to: String): Boolean =
    val normalisedFrom = normalise(from)
    val normalisedTo   = normalise(to)
    (normalisedTo == normalisedFrom) || (normalisedFrom.endsWith("/") && normalisedTo.startsWith(normalisedFrom))

  /** Normalise a URL for lax comparison; lower case, remove protocol, remove leading `www.` or numbered subdomain like
    * `qb1.` or `www-1.`, remove any trailing search and add a trailing `/` if it is a bare hostname.
    */
  private def normalise(url: String) =
    url.toLowerCase
      .replaceAll("^https?://(?:(?:www|[-_a-z]+[0-9]+)\\.)?", "") // ignore changes to protocol/www.
      .replaceAll("#.*", "")                                      // don't consider # part
      .replaceAll("^[^/]*$", "$0/") // foo.com -> foo.com/
end LinkStatus
