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

package loi.cp.domain

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentInterface,
  ComponentService
}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFacade}
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.bootstrap.Bootstrap
import scalaz.std.option.*
import scaloi.syntax.cobind.*

@Component
class DomainSettingsBootstrap(val componentInstance: ComponentInstance)(implicit
  cs: ComponentService,
  domain: DomainDTO,
  fs: FacadeService
) extends ComponentInterface
    with ComponentImplementation:

  @Bootstrap("core.domain.settings")
  def bootstrapDomainSettings(json: JsonDomainSettings): Unit =
    val settings = domain.component[DomainSettings]

    json.shortName foreach domain.facade[DomainFacade].setShortName
    json.name foreach domain.facade[DomainFacade].setName
    json.styleVariables foreach { style =>
      settings.getStyleVariables
        .setColors(style.primaryColor, style.secondaryColor, style.accentColor)
    }
    json.icon `coflatForeach` settings.updateIcon
    json.image `coflatForeach` settings.updateImage
    json.logo `coflatForeach` settings.updateLogo
    json.logo2 `coflatForeach` settings.updateLogo2
    json.css `coflatForeach` settings.updateCss
    json.robots `coflatForeach` settings.updateRobots
    json.sitemap `coflatForeach` settings.updateSitemap
    json.mimeTypes `coflatForeach` settings.updateMimeTypes
  end bootstrapDomainSettings
end DomainSettingsBootstrap

case class JsonDomainSettings(
  shortName: Option[String],
  name: Option[String],
  styleVariables: Option[JsonStyle],
  icon: Option[UploadInfo],
  image: Option[UploadInfo],
  logo: Option[UploadInfo],
  logo2: Option[UploadInfo],
  css: Option[UploadInfo],
  robots: Option[UploadInfo],
  sitemap: Option[UploadInfo],
  mimeTypes: Option[UploadInfo]
)

case class JsonStyle(primaryColor: String, secondaryColor: String, accentColor: String)
