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

package loi.cp.password

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentEnvironment}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Finder.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.scala.util.I18nMessage
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.token.{TokenFinder, TokenType}
import com.learningobjects.cpxp.service.user.UserFacade
import com.learningobjects.cpxp.util.{FormattingUtils, GuidUtil, PersistenceIdFactory}
import loi.cp.password.ChangePasswordReceipt.{EmailAddressNotFound, MessagingError}
import loi.cp.user.UserComponent
import org.hibernate.Session
import scaloi.misc.TimeSource
import scaloi.syntax.DateOps.*

import javax.mail.MessagingException
import scala.compat.java8.OptionConverters.*
import scala.concurrent.duration.*

@Service
class UserPasswordServiceImpl(
  emailService: EmailService,
  componentEnvironment: ComponentEnvironment,
  domain: => DomainDTO
)(implicit
  fs: FacadeService,
  is: ItemService,
  qs: QueryService,
  ids: PersistenceIdFactory,
  urls: CurrentUrlService,
  session: () => Session,
  now: TimeSource,
) extends UserPasswordService:

  override def resetPassword(
    user: UserComponent,
    initiate: Boolean,
    redirect: String => String
  ): ChangePasswordReceipt =
    if initiate then initiatePassword(user: UserComponent, redirect)
    else recoverPassword(user: UserComponent, redirect)

  override def generateChangePasswordToken(user: UserComponent): String =
    createToken(user, TokenType.Recover).tid

  private def recoverPassword(user: UserComponent, redirect: String => String): ChangePasswordReceipt =
    resetPassword("email_recover", TokenType.Recover)(user, redirect)

  private def initiatePassword(user: UserComponent, redirect: String => String): ChangePasswordReceipt =
    resetPassword("email_invite", TokenType.Register)(user, redirect)

  private def resetPassword(
    messageType: String,
    tokenType: TokenType
  )(user: UserComponent, redirect: String => String): ChangePasswordReceipt =
    val token = createToken(user, tokenType)
    sendEmail(user, token, redirect, messageType)

  private def createToken(user: UserComponent, tokenType: TokenType): TokenFinder =
    // delete existing tokens
    user.queryChildren[TokenFinder].getFinders[TokenFinder] foreach is.delete
    user.invalidateQueries()
    // add new token
    create[TokenFinder](user.item) { token =>
      token.accepted = false
      token.tid = GuidUtil.longGuid
      token.ttype = tokenType
      token.expires = now.date + tokenType.getValidity.days
    }
  end createToken

  private def sendEmail(
    user: UserComponent,
    token: TokenFinder,
    redirect: String => String,
    messageType: String
  ): ChangePasswordReceipt =
    val url        = domainUrl(redirect, token)
    val userFacade = user.getId.facade[UserFacade]

    val templateVariables = Map(
      "domain" -> domain,
      "user"   -> userFacade,
      "url"    -> url
    )
    val subject           = getEmailMsg(messageType, "subject", templateVariables)
    val body              = getEmailMsg(messageType, "body_html", templateVariables)

    val senderName  = recoverRootConfig.flatMap(_.senderName)
    val senderEmail = recoverRootConfig.flatMap(_.senderEmail)

    try
      emailService.sendTextEmail(
        senderEmail.orNull,
        senderName.orNull,
        user.getEmailAddress,
        FormattingUtils.userStr(userFacade),
        subject,
        body,
        true
      )
      ChangePasswordReceipt(user, None)
    catch
      case _: InvalidRequestException => ChangePasswordReceipt(user, Some(EmailAddressNotFound))
      case e: MessagingException      => ChangePasswordReceipt(user, Some(MessagingError(e)))
    end try
  end sendEmail

  private def domainUrl(redirect: String => String, token: TokenFinder): String =
    urls.getUrl(s"${redirect(token.tid)}")

  private def getEmailMsg(messageType: String, part: String, templateVariables: Map[String, Any]) =
    I18nMessage.key(s"${messageType}_$part").i18n(templateVariables.toList)(using recoverRootCD)

  private def recoverRootConfig =
    componentEnvironment
      .getJsonConfiguration(classOf[RecoverPasswordRootApiImpl].getName, classOf[RecoverPasswordRootConfiguration])
      .asScala

  private def recoverRootCD: ComponentDescriptor =
    componentEnvironment.getComponent(classOf[RecoverPasswordRootApiImpl].getName)
end UserPasswordServiceImpl
