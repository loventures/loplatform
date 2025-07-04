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

package loi.cp.reply

import java.util.Date

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.reply.ReplyFinder
import com.learningobjects.cpxp.service.script.ScriptService
import com.typesafe.config.Config
import javax.mail.Message.RecipientType
import javax.mail.internet.{InternetAddress, MimeMessage}
import loi.cp.email.{MarshalEmailSupport, MessageHeaders}
import loi.cp.localmail.Localmail
import scaloi.syntax.any.*

import scala.compat.java8.OptionConverters.*

@Component
class ReplyRootApiImpl(val componentInstance: ComponentInstance)(
  rs: ReplyService,
  qs: QueryService
)(implicit ss: ScriptService, componentEnvironment: ComponentEnvironment)
    extends ReplyRootApi
    with ComponentImplementation:
  import MarshalEmailSupport.*
  import ReplyRootApiImpl.*

  override def get(id: Long): Option[ReplyReceipt] =
    get(ApiQuery.byId(id)).asOption

  override def get(q: ApiQuery): ApiQueryResults[ReplyReceipt] =
    ApiQueries.query[ReplyReceipt](qs.queryRoot(ReplyFinder.ITEM_TYPE_REPLY), defaultOrder(q))

  override def reply(localmail: Reply): Unit =
    rs.processReply(toMimeMessage(localmail), None)(Current.getTime)

  override def suspend(address: String): Unit =
    rs.lookupEmail(address) foreach { email =>
      email.setNoReply(true)
    }

  override def isConfigured: Boolean =
    rs.isConfigured

  private def toMimeMessage(reply: Reply): MimeMessage =
    mimeMessage(reply.messageId) <| { email =>
      email.setFrom(toInternetAddress(reply.from))
      email.addRecipient(RecipientType.TO, toInternetAddress(reply.to))
      reply.inReplyTo foreach { irt =>
        email.addHeader(MessageHeaders.InReplyTo, irt)
      }
      email.setSubject(reply.subject)
      email.setSentDate(new Date())
      email.setContent(contentPart(reply.body, true, reply.attachmentUploads*))
      email.saveChanges()
    }

  private def toInternetAddress(email: Localmail.Address): InternetAddress =
    new InternetAddress(email.address, email.name)

  override def getSettings: ReplyRootApi.Settings = replySettings

  override def setSettings(settings: ReplyRootApi.Settings): Unit =
    ss.setJsonConfiguration(Current.getDomain, classOf[ReplyRootApiImpl].getName, settings)
end ReplyRootApiImpl

object ReplyRootApiImpl:
  def replyDomain(implicit componentEnvironment: ComponentEnvironment, config: Config): Option[String] =
    replySettings.domainName.orElse(ReplyConfiguration.get.map(_.domainName))

  def replySettings(implicit componentEnvironment: ComponentEnvironment): ReplyRootApi.Settings =
    componentEnvironment
      .getJsonConfiguration(classOf[ReplyRootApiImpl].getName, classOf[ReplyRootApi.Settings])
      .asScala
      .getOrElse(ReplyRootApi.Settings(None))

  /** If the caller doesn't specify an order then order by date descending. */
  def defaultOrder(q: ApiQuery): ApiQuery =
    if q.getOrders.isEmpty then new ApiQuery.Builder(q).addOrder("date", OrderDirection.DESC).build
    else q
end ReplyRootApiImpl
