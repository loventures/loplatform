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

package loi.cp.language

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.bootstrap.Bootstrap

@Component
class LanguageBootstrap(
  val componentInstance: ComponentInstance,
  implicit val fs: FacadeService
) extends ComponentImplementation:
  @Bootstrap("core.language.create")
  def createLanguage(i18n: BootstrapLanguage): Unit =
    val language =
      LanguageRoot.languageFolder.addLanguage[LanguageComponent](i18n)
    language.upload(i18n.getUpload)
end LanguageBootstrap

trait BootstrapLanguage extends LanguageComponent:
  @JsonProperty
  def getUpload: UploadInfo
