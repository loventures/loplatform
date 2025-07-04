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

package loi.cp.localmail

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.{AttachmentFacade, AttachmentWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.service.script.ScriptService
import com.learningobjects.cpxp.util.DateUtils
import loi.cp.attachment.{AttachmentId, AttachmentInfo}
import scalaz.\/
import scalaz.std.string.*
import scalaz.syntax.std.map.*
import scalaz.syntax.std.option.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import scala.compat.java8.OptionConverters.*

@Component
@SuppressWarnings(Array("unused"))
class LocalmailWebControllerImpl(val componentInstance: ComponentInstance)(
  attachmentWebService: AttachmentWebService,
  componentEnvironment: ComponentEnvironment,
  mws: MimeWebService,
  sm: ServiceMeta,
  ss: ScriptService,
)(implicit
  fs: FacadeService,
) extends LocalmailWebController
    with ComponentImplementation:
  import LocalmailWebController.*
  import LocalmailWebControllerImpl.*

  override def getAllLocalmails: ErrorResponse \/ ArgoBody[Map[String, List[Localmail]]] =
    for _ <- checkEnabled
    yield ArgoBody {
      folder
        .findLocalmails(DateUtils.delta(-MaxAge))
        .map(facade2Localmail)
        .groupBy(_.to.address)
        .mapKeys(_ stripSuffix AtLocalmail)
    }

  override def getLocalmails(email: String): ErrorResponse \/ ArgoBody[List[Localmail]] =
    for _ <- checkEnabled
    yield ArgoBody(getLocalmails0(email))

  override def getLocalmail(email: String, id: Long): ErrorResponse \/ ArgoBody[Localmail] =
    for
      _         <- checkEnabled
      localmail <- getLocalmail0(email, id)
    yield ArgoBody(localmail)

  override def getAttachment(email: String, id: Long, att: Long): ErrorResponse \/ FileResponse[?] =
    for
      _          <- checkEnabled
      localmail  <- getLocalmailFacade0(email, id)
      attachment <- localmail.getAttachments.find(_.getId.longValue == att) \/> ErrorResponse.notFound
    yield FileResponse(attachmentWebService.getAttachmentBlob(attachment.getId))

  override def deleteLocalmail(email: String, id: Long): ErrorResponse \/ Unit =
    for localmail <- getLocalmailFacade0(email, id)
    yield localmail.delete()

  override def getSettings: LocalmailWebController.Settings =
    componentEnvironment
      .getJsonConfiguration(classOf[LocalmailWebControllerImpl].getName, classOf[LocalmailWebController.Settings])
      .asScala
      .getOrElse(LocalmailWebController.Settings(false))

  override def setSettings(settings: Settings): Unit =
    ss.setJsonConfiguration(Current.getDomain, classOf[LocalmailWebControllerImpl].getName, settings)

  // the ...0 versions don't do enabled checking nor jsonification

  private def getLocalmail0(email: String, id: Long): ErrorResponse \/ Localmail =
    getLocalmails0(email).find(_.id == id) \/> ErrorResponse.notFound

  private def getLocalmails0(email: String): List[Localmail] =
    getLocalmailFacades0(email).map(facade2Localmail)

  private def getLocalmailFacades0(email: String): List[LocalmailFacade] =
    folder.findLocalmailsByToAddress(email + AtLocalmail, DateUtils.delta(-MaxAge))

  private def getLocalmailFacade0(email: String, id: Long): ErrorResponse \/ LocalmailFacade =
    getLocalmailFacades0(email).find(_.getId.longValue == id) \/> ErrorResponse.notFound

  private def checkEnabled: ErrorResponse \/ Unit =
    (getSettings.enabled || !sm.isProdLike) \/> ErrorResponse.forbidden("Localmail not enabled")

  private def facade2Localmail(lmf: LocalmailFacade): Localmail =
    Localmail(
      id = lmf.getId,
      from = Localmail.Address(lmf.getFromAddress, lmf.getFromName),
      to = Localmail.Address(lmf.getToAddress, Option(lmf.getToName) getOrElse ""),
      subject = lmf.getSubject,
      messageId = lmf.getMessageId,
      inReplyTo = OptionNZ(lmf.getInReplyTo),
      body = lmf.getBody,
      date = lmf.getDate.toInstant,
      attachments = lmf.getAttachments.map(facade2Attachment),
    )

  private def facade2Attachment(af: AttachmentFacade): AttachmentInfo =
    AttachmentInfo(
      id = AttachmentId(af.getId),
      fileName = af.getFileName,
      size = af.getSize,
      mimeType = Option(mws.getMimeType(af.getFileName)) getOrElse "application/octet-stream",
      createDate = af.getCreated.toInstant,
    )
end LocalmailWebControllerImpl

object LocalmailWebControllerImpl:
  private val MaxAge: Long = 1000L * 60 * 5

  private[localmail] def folder(implicit fs: FacadeService): LocalmailParentFacade =
    Current.getDomain.facade[LocalmailRootFacade].getFolderByType(LocalmailRootFacade.LocalmailFolderType)
