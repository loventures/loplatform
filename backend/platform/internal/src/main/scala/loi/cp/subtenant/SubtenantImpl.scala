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

package loi.cp.subtenant

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.upload.Uploads
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.subtenant.SubtenantFacade
import loi.cp.attachment.AttachmentComponent
import com.learningobjects.cpxp.scala.cpxp.Component.*
import loi.cp.subtenant.Subtenant.SubtenantDTO

@Component
class SubtenantImpl(val componentInstance: ComponentInstance, facade: SubtenantFacade, aws: AttachmentWebService)(
  implicit cs: ComponentService
) extends Subtenant
    with ComponentImplementation:

  @PostCreate
  private def init(subtenant: SubtenantDTO): Unit =
    update(subtenant)

  override def getId = componentInstance.getId

  override def getTenantId: String = facade.getTenantId

  override def getName: String = facade.getName

  override def getShortName: String = facade.getShortName

  override def getLogo: Option[AttachmentComponent] =
    Option(facade.getLogo).map(_.component[AttachmentComponent])

  override def delete(): Unit =
    facade.delete()

  override def getLogoUpload: Option[String] = ???

  override def setName(name: String): Unit = facade.setName(name)

  override def setTenantId(tenantId: String): Unit = facade.setTenantId(tenantId)

  override def update(subtenant: SubtenantDTO): Unit =
    facade.setTenantId(subtenant.tenantId)
    facade.setName(subtenant.name)
    facade.setShortName(subtenant.shortName)
    val logoGuid = subtenant.logoUpload
    logoGuid.foreach(guid =>
      if guid == "remove" then facade.setLogo(null)
      else if guid != "" then
        val logoId = aws.createAttachment(getId, Uploads.retrieveUpload(guid))
        facade.setLogo(logoId)
    )
    facade.pollute()
  end update
end SubtenantImpl
