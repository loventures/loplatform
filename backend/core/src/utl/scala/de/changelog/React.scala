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

import cats.effect.Sync
import de.common.{Jira, Klient, Stash, `Aha!`}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.http4s.Uri
import scalaz.syntax.std.option.*

import java.text.SimpleDateFormat
import java.util.Date
import scala.xml.{Group as _, *}

private[changelog] object React:
  import de.common.Jira.Ticket
  import de.common.Stash.PullRequest

  def render[F[_]: Sync](
    prs: Map[Group, List[PullRequest]],
    issues: Map[Long, List[Ticket]],
    branch: String
  ): Klient[F, Elem] =
    for jiraUri <- Klient.ask[F].map(_.jiraUri)
    yield
      val date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss'Z'").format(new Date)
      <html>
      <head>
        <title>{s"Changelog: $branch"}</title>
          <link rel="stylesheet"
                href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
                integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO"
                crossorigin="anonymous" />
          <style type="text/css">{
        """
            img { max-width: 60% }
          """
      }</style>
      </head>
      <body>
        <div class="container my-5">
          <div class="preamble"></div>
          <h3 style="font-size: 1.5rem; line-height: 1.2; margin-top: 1rem; margin-bottom: .5rem">{
        s"Changelog for $branch, build date $date"
      }</h3> {
        Group.values flatMap { group =>
          prs.get(group) map { prs =>
            <div>
                <h4 style="font-size: 1.3rem; line-height: 1.2; margin-top: 1rem; margin-bottom: .5rem">{
              group.friendlyName
            }</h4> {
              prs.map(pr => renderPR(pr, issues(pr.id), jiraUri))
            }
              </div>
          }
        }
      } </div>
      </body>
      </html>

  private def renderPR(pr: PullRequest, tickets: List[Ticket], jiraUri: Uri): Elem =
    <div style="margin-bottom: .5rem">
      <div style="border-top: 1px solid #888; padding-top: .5rem; margin-bottom: .5rem">
        <span style="font-size: 1.2rem; margin-right: .5rem">
          <span style="vertical-align: text-top; font-size: .75rem">#</span>{pr.id}
        </span>
        <a style="vertical-align: top; font-weight: 0500; font-size: 1.1rem" href={pr.links.self.head.href}>
          {deticket(pr.title)}
        </a>
      </div>
      <div style="margin-bottom: 1rem">
        <div style="margin-bottom: .5rem">
          {
      tickets.map { ticket =>
        <div>
            <span style="font-weight: 0500; font-size: .9rem; margin-right: .5rem">
              {ticket.key}
            </span>
            <a href={ticketUrl(jiraUri)(ticket.key)}>
              {ticket.fields.summary}
            </a>
          </div>
      } ++ pr.description.map(Stash.linkAttachments).map { desc =>
        <div style="color: #444; margin-bottom: .5rem" class="md">
            {renderMD(desc)}
          </div>
      }
    }
        </div>
      </div>
    </div>

  def ticketUrl(jiraUri: Uri)(key: String): String =
    if `Aha!`.TicketRe matches key then `Aha!`.url(key) else Jira.url(jiraUri)(key)

  private def renderMD(md: String): Node =
    val parser   = Parser.builder.build
    val document = parser.parse(md)
    val renderer = HtmlRenderer.builder.softbreak("<br />").build
    val html     = renderer.render(document)
    rewriteNodes(XML.loadString(s"<div>$html</div>") :: Nil, Nil).head

  // Rewrite space image urls
  private def rewriteNodes(nodes: List[Node], result: List[Node]): List[Node] = nodes match
    case (e: Elem) :: tail =>
      // Is this an image out of stash?
      e.elemAttrStartsWith("img", "src", Stash.stashUri.toString)
        .cata(
          { uri =>
            // Rewrite spaces as demanded by society
            val src = new UnprefixedAttribute("src", uri.replace("+", "%20"), Null)
            rewriteNodes(tail, (e % src) :: result)
          }, {
            // Nope, so just rewrite all its children
            val childrenᛌ = rewriteNodes(e.child.toList, Nil)
            rewriteNodes(tail, e.copy(child = childrenᛌ) :: result)
          }
        )

    case node :: tail =>
      rewriteNodes(tail, node :: result)

    case Nil =>
      result.reverse

  // strip "CBLPROD-1234: Foo bar - CBLPROD-1236, CBLPROD-1237" down to "Foo bar"
  private[changelog] def deticket(title: String): String =
    title.replaceAll(s" *[-.,:(]? *(?:${Stash.TicketRe.regex}) *[-.,:)]? *", "").capitalize
end React
