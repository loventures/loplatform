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

import cats.effect.{Async, Resource, Sync}
import cats.instances.list.*
import cats.syntax.all.*
import de.common.{Klient, Stash, getenv}
import org.apache.commons.codec.digest.DigestUtils
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*

import java.net.URL
import javax.activation.{DataHandler, FileDataSource}
import javax.mail.*
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import scala.xml.{Elem, Node, Null, UnprefixedAttribute}

private[changelog] object Email:

  /** Render external stash images as S3 urls. */
  def renderEmail(html: Node, images: Map[String, URL]): String =
    renderNodes(html :: Nil, images, Nil).head.toString

  def emailResults[F[_]: Async](
    to: String,
    subject: Option[String],
    name: String,
    html: Node,
    release: Boolean
  ): Klient[F, Unit] =
    for
      session <- Klient.liftF(openSession[F])
      email   <- generateEmail(to, subject, name, html, release, session)
      _       <- Klient.liftF(transport[F](session).use(transport => sendEmail[F](email, transport)))
    yield ()

  // TODO: Make a Resource
  private def openSession[F[_]: Sync]: F[Session] =
    for
      username <- getenv[F](UsernameEnv)
      password <- getenv[F](PasswordEnv)
    yield Session.getInstance(
      System.getProperties,
      new Authenticator:
        override def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(username, password)
    )

  private def transport[F[_]](session: Session)(implicit F: Sync[F]): Resource[F, Transport] =
    Resource.make(F.delay(session.getTransport <| { _.connect() }))(transport => F.delay(transport.close()))

  private def sendEmail[F[_]: Sync](email: MimeMessage, transport: Transport): F[Unit] = Sync[F] delay {
    Thread.currentThread.setContextClassLoader(classOf[MimeMessage].getClassLoader) // java is poo
    transport.sendMessage(email, email.getAllRecipients)
  }

  private def generateEmail[F[_]: Async](
    to: String,
    subject: Option[String],
    name: String,
    html: Node,
    release: Boolean,
    session: Session
  ): Klient[F, MimeMessage] =
    val (email, attachments) = rewriteHtml(html, release)
    attachments.toList
      .map(att => mimeAttachment[F](att._1, att._2))
      .sequence
      .map({ parts =>
        new MimeMessage(session) <| { message =>
          message.setFrom(new InternetAddress("equestria@learningobjects.com", "Learning Objects Platform"))
          message.setReplyTo(Array(new InternetAddress("noreply@learningobjects.com")))
          message.setSubject(subject.cata(subj => s"$subj - $name", name))
          message.setRecipients(Message.RecipientType.TO, to)
          message.setContent(multipart(email, parts))
        }
      })
  end generateEmail

  private def multipart(email: Node, parts: Seq[MimeBodyPart]): MimeMultipart =
    new MimeMultipart("related") <| { related =>
      related `addBodyPart` new MimeBodyPart <| { text =>
        text.setText(email.toString, "UTF-8", "html")
      }
      parts foreach related.addBodyPart
    }

  /** Rewrite external stash images as content id references, returning revised html and map from content id to stash
    * url.
    */
  private def rewriteHtml(html: Node, release: Boolean): (Node, Map[String, String]) =
    val (htmlᛌ :: _, attachments) = rewriteNodes(html :: Nil, Map.empty, Nil, release): @unchecked
    htmlᛌ -> attachments

  // makes a self-contained email with attachments
  private def rewriteNodes(
    nodes: List[Node],
    attachments: Map[String, String],
    result: List[Node],
    release: Boolean
  ): (List[Node], Map[String, String]) = nodes match
    case (e: Elem) :: tail =>
      // Is this an image out of stash?
      e.elemAttrStartsWith("img", "src", Stash.stashUri.toString)
        .cata(
          { uri =>
            // Yup so rewrite that href and add some css so it'll print okay
            val cid   = DigestUtils.md5Hex(uri)
            val src   = new UnprefixedAttribute("src", s"cid:$cid", Null)
            val style = new UnprefixedAttribute("style", "max-width: 100%", Null)
            rewriteNodes(tail, attachments + (cid -> uri), (e % src % style) :: result, release)
          }, {
            // Nope, so just rewrite all its children
            val (childrenᛌ, attachmentsᛌ) = rewriteNodes(e.child.toList, attachments, Nil, release)
            // But if it has a hyperlink to stash then strip it
            val href                      = e.elemAttrStartsWith("a", "href", Stash.stashUri.toString)
            val attributesᛌ               = if href.isDefined then e.attributes.remove("href") else e.attributes
            val elem                      = e
              .elemAttr("div", "class")
              .contains("preamble")
              .fold(
                <div>{release.fold(ReleasePreamble, PrereleasePreamble)}</div>,
                e.copy(child = childrenᛌ, attributes = attributesᛌ)
              )
            rewriteNodes(tail, attachmentsᛌ, elem :: result, release)
          }
        )

    case node :: tail =>
      rewriteNodes(tail, attachments, node :: result, release)

    case Nil =>
      result.reverse -> attachments

  // makes an email with external attachments
  private def renderNodes(
    nodes: List[Node],
    images: Map[String, URL],
    result: List[Node],
  ): List[Node] = nodes match
    case (e: Elem) :: tail =>
      // Is this an image out of stash?
      e.elemAttrStartsWith("img", "src", Stash.stashUri.toString)
        .cata(
          { uri =>
            // Yup so rewrite that href and add some css so it'll print okay
            val src   = new UnprefixedAttribute("src", images(uri).toExternalForm, Null)
            val style = new UnprefixedAttribute("style", "max-width: 100%", Null)
            renderNodes(tail, images, (e % src % style) :: result)
          }, {
            // Nope, so just rewrite all its children
            val childrenᛌ   = renderNodes(e.child.toList, images, Nil)
            // But if it has a hyperlink to stash then strip it
            val href        = e.elemAttrStartsWith("a", "href", Stash.stashUri.toString)
            val attributesᛌ = if href.isDefined then e.attributes.remove("href") else e.attributes
            val elem        = e
              .elemAttr("div", "class")
              .contains("preamble")
              .fold(
                <span></span>,
                e.copy(child = childrenᛌ, attributes = attributesᛌ)
              )
            renderNodes(tail, images, elem :: result)
          }
        )

    case node :: tail =>
      renderNodes(tail, images, node :: result)

    case Nil =>
      result.reverse

  /** Download an attachment and turn it into a MIME body part. */
  private def mimeAttachment[F[_]: Async](cid: String, url: String): Klient[F, MimeBodyPart] =
    Stash.downloadAttachment(url) map { fileWithHeaders =>
      new MimeBodyPart <| { imagePart =>
        imagePart.setDataHandler(new DataHandler(new FileDataSource(fileWithHeaders._1)))
        imagePart.setContentID(s"<$cid>")
        imagePart.setDisposition("inline")
      }
    }

  private final val UsernameEnv = "SMTP_USERNAME"
  private final val PasswordEnv = "SMTP_PASSWORD"

  private final val PrereleasePreamble =
    """Learning Objects will deploy a software update sometime in the future.
      |Changes included with this update are as follows:""".stripMargin

  private final val ReleasePreamble =
    """Learning Objects will deploy a software update during our next release window
      |(between 6:00 and 7:00 AM Eastern Time on the next business day). Changes
      |included with this update are as follows:""".stripMargin
end Email
