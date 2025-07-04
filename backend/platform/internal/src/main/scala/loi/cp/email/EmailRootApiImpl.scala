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

package loi.cp.email

import java.nio.charset.StandardCharsets
import javax.mail.internet.MimeMessage
import javax.mail.Message as MailMessage

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.{TextResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.email.{EmailFinder, EmailService}
import com.learningobjects.cpxp.service.query.QueryService
import loi.cp.reply.ReplyService

@Component
class EmailRootApiImpl(
  val componentInstance: ComponentInstance,
  qs: QueryService,
  es: EmailService,
  rs: ReplyService
) extends EmailRootApi
    with ComponentImplementation:
  import EmailRootApiImpl.*

  override def get(id: Long): Option[Email] =
    get(ApiQuery.byId(id)).asOption

  override def get(q: ApiQuery): ApiQueryResults[Email] =
    ApiQueries.query[Email](qs.queryRoot(EmailFinder.ITEM_TYPE_EMAIL), defaultOrder(q))

  override def test(address: String): WebResponse =
    def testEmail(email: MimeMessage): Unit =
      email.setFrom(MarshalEmailSupport.replyAddress("Test Email", 1L, 2L, rs.replyDomain))
      email.addRecipient(MailMessage.RecipientType.TO, MarshalEmailSupport.toAddress("Test Recipient", address))
      email.setSubject("Test Subject", StandardCharsets.UTF_8.name)
      email.setContent(MarshalEmailSupport.contentPart("<p>Test body.</p>", asHtml = true))
    es.sendEmail(MarshalEmailSupport.domainMessageId(1L), testEmail)
    TextResponse.plain("OK")
end EmailRootApiImpl

object EmailRootApiImpl:

  /** If the caller doesn't specify an order then order by id descending. */
  def defaultOrder(q: ApiQuery): ApiQuery =
    if q.getOrders.isEmpty then new ApiQuery.Builder(q).addOrder("id", OrderDirection.DESC).build
    else q
