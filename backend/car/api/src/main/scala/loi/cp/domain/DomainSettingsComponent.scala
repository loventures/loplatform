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

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.ComponentDecorator
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.controller.domain.DomainAppearance
import com.learningobjects.cpxp.validation.groups.Writable
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.attachment.AttachmentComponent

import javax.validation.groups.Default

/** This trait accretes just the information necessary for an administrator to update domain settings. This is not
  * intended for general usage.
  */
@Schema("domainSettings")
@Secured(Array(classOf[AdminRight]))
trait DomainSettingsComponent extends ComponentDecorator:

  /** Update the domain settings.
    * @param settings
    *   the new domain settings
    */
  @RequestMapping(method = Method.PUT)
  def update(@RequestBody settings: DomainSettingsComponent): DomainSettingsComponent

  /** Full domain name.
    */
  @JsonProperty
  def getName: String

  /** A short description of the domain.
    */
  @JsonProperty
  def getDescription: Option[String]

  /** Short domain name, used where space is tight. U
    */
  @JsonProperty
  def getShortName: String

  /** Default language.
    */
  @JsonProperty
  def getDefaultLanguage: String

  /** Default time zone.
    */
  @JsonProperty
  def getDefaultTimeZone: String

  /** Google analytics UA number.
    */
  @JsonProperty
  def getGoogleAnalyticsAccount: Option[String]

  /** Get style variables.
    */
  @JsonProperty
  def getStyleVariables: DomainAppearance

  /** Current robots.txt file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "robots", method = Method.GET)
  def getRobots: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded robots.txt file.
    */
  @JsonView(Array(classOf[Writable]))
  def getRobotsUpload: Option[String]

  /** Current sitemap.xml file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "sitemap", method = Method.GET)
  def getSitemap: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded sitemap.xml file.
    */
  @JsonView(Array(classOf[Writable]))
  def getSitemapUpload: Option[String]

  /** Current favicon file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "icon", method = Method.GET)
  def getIcon: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded favicon file.
    */
  @JsonView(Array(classOf[Writable]))
  def getIconUpload: Option[String]

  /** Current logo file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "logo", method = Method.GET)
  def getLogo: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded logo file.
    */
  @JsonView(Array(classOf[Writable]))
  def getLogoUpload: Option[String]

  /** Current logo2 file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "logo2", method = Method.GET)
  def getLogo2: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded logo2 file.
    */
  @JsonView(Array(classOf[Writable]))
  def getLogo2Upload: Option[String]

  /** Current image file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "image", method = Method.GET)
  def getImage: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded image file.
    */
  @JsonView(Array(classOf[Writable]))
  def getImageUpload: Option[String]

  /** Current CSS file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "css", method = Method.GET)
  def getCss: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded CSS file. This may be a single CSS file or a zip containing domain.css.
    */
  @JsonView(Array(classOf[Writable]))
  def getCssUpload: Option[String]

  /** Current MIME types file.
    */
  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "mimeTypes", method = Method.GET)
  def getMimeTypes: Option[AttachmentComponent]

  /** On submit, the GUID of an uploaded MIME types file.
    */
  @JsonView(Array(classOf[Writable]))
  def getMimeTypesUpload: Option[String]

  /** The email which should be used for support
    */
  @JsonProperty
  def getSupportEmail: Option[String]
end DomainSettingsComponent
