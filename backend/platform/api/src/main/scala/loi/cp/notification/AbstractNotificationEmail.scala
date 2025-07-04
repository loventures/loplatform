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

package loi.cp.notification

import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentService, ComponentSupport}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.scala.util.Misc.ErrorMessage
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.context.CourseContextComponent
import loi.cp.email.MarshalEmailSupport.*
import loi.cp.email.*
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*

import javax.mail.internet.{MimeMessage, MimeMultipart}
import scala.reflect.ClassTag
import scala.xml.{Elem, Node, Unparsed}

/** Abstract superclass for notification emails.
  */
abstract class AbstractNotificationEmail[A <: Notification: ClassTag](implicit cs: ComponentService)
    extends AbstractEmail[A]:
  import AbstractNotificationEmail.*

  override val bodyClass: Class[?] = classOf[Nothing]

  /** Build an email for the associated notification. */
  override def buildEmail(email: MimeMessage): Unit =
    implicit val cd = componentInstance.getComponent
    val bindings    = getBindings
    initEmail(email, from, subject.i18n(bindings), notification.getTime)
    val content     = contentPart(html(bindings).toString, asHtml = true)
    addAttachments(content)
    email.setContent(content)

  protected def addAttachments(part: MimeMultipart): Unit = ()

  /** Render the html. Could use lohtml if we want more fidelity in our formatting. */
  private def html(bindings: Map[String, Any])(implicit cd: ComponentDescriptor): Elem =
    <html>
      <body>
        {parts(bindings)}
        <footer>
          <hr />
          {footer.i18n(bindings).u}
        </footer>
      </body>
    </html>

  /** Replies to most notification emails are unsupported. */
  override def processReply(email: MimeMessage): ErrorMessage \/ Option[Long] =
    ReplyUnsupported.left

  /** Get the bindings to use when rendering the email. */
  protected def getBindings: Map[String, Any] =
    notification.bindings(using domain = domain, user = getUser.toDTO, cs = cs)

  /** Get the parts of the body of the email. */
  protected def parts(bindings: Map[String, Any])(implicit cd: ComponentDescriptor): Seq[Node] =
    Seq(intro, body, outro) map { p =>
      <p>{p.i18n(bindings).u}</p>
    }

  /** Get the email sender name. */
  protected def from: String = context.fold(domain.name)(_.getName)

  /** Get the email subject. */
  protected def subject: I18nMessage = I18nMessage(s"NOTIFICATIONS_EMAIL_SUBJECT_$schema", null)

  /** Get the email intro. */
  protected def intro: I18nMessage = context.isDefined.fold(EmailIntroInCourse, EmailIntroNoCourse)

  /** Get the email body. */
  protected def body: I18nMessage = I18nMessage(s"NOTIFICATIONS_EMAIL_BODY_${schema}_html", null)

  /** Get the email outro. */
  protected def outro: I18nMessage = EmailOutro

  /** Get the email footer. */
  protected def footer: I18nMessage = EmailFooter

  protected val domain: DomainDTO

  protected implicit val fs: FacadeService

  protected val ns: NotificationService

  protected lazy val schema: String = ComponentSupport.getSchemaName(notification)

  protected lazy val notification: A = getEntity.flatMap(ns.notification).flatMap(implicitly[ClassTag[A]].unapply).get

  protected lazy val context: Option[CourseContextComponent] =
    notification.getContext.map(_.component[CourseContextComponent])
end AbstractNotificationEmail

object AbstractNotificationEmail:

  implicit class UnparsedStringOps(val s: String) extends AnyVal:
    def u: Unparsed = Unparsed(s)

  val EmailIntroNoCourse =
    I18nMessage("NOTIFICATIONS_EMAIL_INTRO_html", "Hi {user.givenName}, you have received a new notification:")

  val EmailIntroInCourse = I18nMessage(
    "NOTIFICATIONS_EMAIL_INTRO_IN_COURSE_html",
    "Hi {user.givenName}, you have received a new notification in {course.name}:"
  )

  val EmailOutro = I18nMessage(
    "NOTIFICATIONS_EMAIL_OUTRO_html",
    "Please log in to view this on your notifications page. If you no longer wish to receive email notifications, please log in and change your notifications settings."
  )

  val EmailFooter =
    I18nMessage("EMAIL_FOOTER_html", """Sent from <a href="https://{domain.hostName}/">{domain.name}</a>.""")

  val ReplyUnsupported = I18nMessage("NOTIFICATIONS_EMAIL_NOREPLY", "Replies to this email are not supported.")
end AbstractNotificationEmail
