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

import argonaut.*
import argonaut.Argonaut.*
import cats.effect.Async
import cats.syntax.option.*
import de.common.{Klient, Stash}
import org.http4s.Uri
import scaloi.json.ArgoExtras

import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

private[changelog] object Slack:

  import React.{deticket, ticketUrl}
  import de.common.Jira.Ticket
  import de.common.Stash.PullRequest

  def render[F[_]: Async](
    prs: Map[Group, List[PullRequest]],
    issues: Map[Long, List[Ticket]],
    images: Map[String, URL],
    name: String,
  ): Klient[F, String] =
    for
      jiraUri <- Klient.ask[F].map(_.jiraUri)
      notes    = releaseNotes(prs, issues, images, jiraUri)
    yield
      val date      = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss'Z'").format(new Date)
      val title     = s"Release Notes for $name, build date $date"
      val blocks    = Header(title) :: notes
      val truncated =
        if blocks.length <= BlockLimit then blocks
        else blocks.take(BlockLimit - 1) ::: mdBlock("* Changelog Truncated *") :: Nil
      Message(title, truncated).asJson.nospaces

  private def releaseNotes(
    prs: Map[Group, List[PullRequest]],
    issues: Map[Long, List[Ticket]],
    images: Map[String, URL],
    jiraUri: Uri
  ): List[Block] =
    for
      group <- Group.values.toList
      prl   <- prs.get(group).toList
      pr    <- prl
      block <- renderPR(pr, issues(pr.id), images, jiraUri)
    yield block

  private def renderPR(
    pr: PullRequest,
    tickets: List[Ticket],
    images: Map[String, URL],
    jiraUri: Uri,
  ): List[Block] =
    val titleInfo   = s"*#${pr.id} — ${crapdown(deticket(pr.title))}*"
    val ticketInfo  = tickets map { ticket =>
      s"• <${ticketUrl(jiraUri)(ticket.key)}|${ticket.key}>: ${crapdown(ticket.fields.summary)}"
    }
    val intro       = Section(Text.markdown((titleInfo :: ticketInfo).mkString("\n")))
    val description = pr.description.map(blockify(_, images)).orEmpty
    intro :: description
  end renderPR

  /** Split a paragraph of Stash markdown into a list of Slack code blocks, images and text blocks. */
  def blockify(paragraph: String, images: Map[String, URL]): List[Block] =
    // First split the text into code blocks and non-code blocks
    def splitCodeBlocks(s: String): List[Block] =
      val ticTicTic = s.indexOf("```")
      val tacTacTac = s.indexOf("```", ticTicTic + 3)
      if ticTicTic < 0 then splitImages(s)
      else
        val text = s.substring(0, ticTicTic).trim
        if tacTacTac < 0 then
          val code = s.substring(ticTicTic) + "```"
          splitImages(text) ::: mdBlock(code, "...```") :: Nil
        else
          val code = s.substring(ticTicTic, tacTacTac + 3)
          splitImages(text) ::: mdBlock(code, "...```") :: splitCodeBlocks(s.substring(tacTacTac + 3))
    end splitCodeBlocks

    // Then split the non-code blocks into text and images
    def splitImages(s: CharSequence): List[Block] =
      Stash.ImageRE.findFirstMatchIn(s) match
        case None        => splitText(s)
        case Some(image) =>
          val imageUrl = Stash.linkAttachments(image.group(2)).replace("+", "%20")
          val s3Url    = images(imageUrl).toExternalForm
          splitText(image.before) ::: Image(s3Url, image.group(1)) :: splitImages(image.after)

    // Then split the paragraphs into distinct markdown blocks. In principle we could include
    // multiple paragraphs in a single text block and try to balance the 50 / 3000 limits, but
    // address that if it comes up.
    def splitText(str: CharSequence): List[Block] =
      "\n\n".r.split(str).toList.map(_.trim).filter(_.nonEmpty).map(txt => mdBlock(BoldRE.replaceAllIn(txt, "$1")))

    splitCodeBlocks(paragraph)
  end blockify

  private def mdBlock(text: String, suffix: String = "..."): Block =
    val crap = crapdown(text)
    Section(
      Text.markdown(
        if crap.length < TextLimit then crap else crap.substring(0, TextLimit - suffix.length) + suffix
      )
    )

  private final val BlockLimit = 50   // Slack #blocks limit
  private final val TextLimit  = 3000 // Slack block size limit

  // stash **bold**, vs slack *bold*
  private final val BoldRE = """\*(\*.*?\*)\*""".r

  /** Slack markdown is not real markdown. */
  private def crapdown(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  sealed trait Block:
    val `type`: String

  final case class Text(`type`: String, text: String)

  object Text:
    def plain(text: String): Text = Text("plain_text", text)

    def markdown(text: String): Text = Text("mrkdwn", text)

  implicit def codecText: CodecJson[Text] = casecodec2(Text.apply, ArgoExtras.unapply)("type", "text")

  final case class Header(`type`: String, text: Text) extends Block

  object Header:
    def apply(text: String): Header = Header("header", Text.plain(text))

  implicit def codecHeader: CodecJson[Header] = casecodec2(Header.apply, ArgoExtras.unapply)("type", "text")

  final case class Image(`type`: String, image_url: String, alt_text: String) extends Block

  object Image:
    def apply(url: String, alt: String): Image = Image("image", url, alt)

  implicit def codecImage: CodecJson[Image] =
    casecodec3(Image.apply, ArgoExtras.unapply)("type", "image_url", "alt_text")

  final case class Section(`type`: String, text: Text) extends Block

  object Section:
    def apply(text: Text): Section = Section("section", text)

  implicit def codecSection: CodecJson[Section] = casecodec2(Section.apply, ArgoExtras.unapply)("type", "text")

  implicit def blockEncoder: EncodeJson[Block] = EncodeJson {
    case header: Header   => header.asJson
    case image: Image     => image.asJson
    case section: Section => section.asJson
  }

  final case class Message(text: String, blocks: List[Block])

  implicit def encodeMessage: EncodeJson[Message] =
    EncodeJson(message => Json.obj("text" -> message.text.asJson, "blocks" -> message.blocks.asJson))
end Slack
