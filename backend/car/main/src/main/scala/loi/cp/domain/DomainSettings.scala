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

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.domain.DomainAppearance
import com.learningobjects.cpxp.controller.upload.{UploadInfo, Uploads}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.{AttachmentFacade, AttachmentParentFacade, AttachmentWebService}
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainFacade, DomainWebService}
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.language.LanguageService
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.service.name.NameService
import com.learningobjects.cpxp.util.ClassUtils
import loi.cp.attachment.AttachmentComponent

import scala.compat.java8.OptionConverters.*
import scala.util.control.NonFatal

/* Missing from former settings screens:
 *
 * * Appearance
 *   . Theme
 *   . Dark Color
 *   . Medium Color
 *   . Light Color
 * * Settings
 *   . Login Required
 *   . User URL Format
 *   . Group URL Format
 *   . Guest Policy
 *   . Terms of Service
 *   . Domain Help Label / URL
 */

@Component
class DomainSettings(
  val componentInstance: ComponentInstance,
  self: DomainFacade,
  ns: NameService,
  aws: AttachmentWebService,
  dws: DomainWebService,
  ls: LanguageService,
  da: DomainAppearance,
  implicit val fs: FacadeService,
  implicit val mws: MimeWebService
)(implicit cs: ComponentService)
    extends DomainSettingsComponent
    with ComponentImplementation:

  import DomainSettings.*

  override def getName: String = self.getName

  override def getShortName: String = self.getShortName

  override def getDescription: Option[String] = Option(self.getDescription)

  override def getDefaultLanguage: String =
    ClassUtils.parseLocale(self.getLocale).toLanguageTag

  override def getDefaultTimeZone: String = self.getTimeZone

  override def getGoogleAnalyticsAccount: Option[String] =
    Option(self.getGoogleAnalyticsAccount).filterNot(_.isEmpty)

  override def getStyleVariables: DomainAppearance = da

  override def getRobots: Option[AttachmentComponent] =
    Option(ns.getItem("/robots.txt")).map(_.component[AttachmentComponent])

  override def getRobotsUpload: Option[String] = ???

  override def getSitemap: Option[AttachmentComponent] =
    Option(ns.getItem("/sitemap.xml")).map(_.component[AttachmentComponent])

  override def getSitemapUpload: Option[String] = ???

  override def getIcon: Option[AttachmentComponent] =
    Option(self.getFavicon).map(_.component[AttachmentComponent])

  override def getIconUpload: Option[String] = ???

  override def getLogo: Option[AttachmentComponent] =
    Option(self.getLogo).map(_.component[AttachmentComponent])

  override def getLogoUpload: Option[String] = ???

  override def getLogo2: Option[AttachmentComponent] =
    Option(self.getLogo2).map(_.component[AttachmentComponent])

  override def getLogo2Upload: Option[String] = ???

  override def getImage: Option[AttachmentComponent] =
    Option(self.getImage).map(_.component[AttachmentComponent])

  override def getImageUpload: Option[String] = ???

  override def getCss: Option[AttachmentComponent] =
    Option(self.getCssFile).map(_.component[AttachmentComponent])

  override def getCssUpload: Option[String] = ???

  override def getMimeTypes: Option[AttachmentComponent] =
    Option(mws.getMimeTypes).map(_.component[AttachmentComponent])

  override def getMimeTypesUpload: Option[String] = ???

  override def getSupportEmail: Option[String] = self.getSupportEmail.asScala

  override def update(settings: DomainSettingsComponent): DomainSettingsComponent =
    validateUploads(settings)

    self.setName(settings.getName)
    self.setShortName(settings.getShortName)
    self.setTimeZone(settings.getDefaultTimeZone)
    self.setGoogleAnalyticsAccount(settings.getGoogleAnalyticsAccount.filterNot(_.isEmpty).orNull)
    self.setDescription(settings.getDescription.filterNot(_.isEmpty).orNull)
    self.setSupportEmail(settings.getSupportEmail.filterNot(_.isEmpty).asJava)
    // disable this because it lets us shoot ourselves in our shared foot
    // self.setAllowedOrigins(
    //   AllowedOrigins(settings.getAllowedOrigins.map(_.trim)))
    ls.setDefaultLanguage(settings.getDefaultLanguage)

    getStyleVariables.setColors(
      settings.getStyleVariables.getPrimaryColor,
      settings.getStyleVariables.getSecondaryColor,
      settings.getStyleVariables.getAccentColor
    )

    applyUpload(settings.getIconUpload, updateIcon)
    applyUpload(settings.getLogoUpload, updateLogo)
    applyUpload(settings.getLogo2Upload, updateLogo2)
    applyUpload(settings.getImageUpload, updateImage)
    applyUpload(settings.getCssUpload, updateCss)
    applyUpload(settings.getRobotsUpload, updateRobots)
    applyUpload(settings.getSitemapUpload, updateSitemap)
    applyUpload(settings.getMimeTypesUpload, updateMimeTypes)

    this
  end update

  private def applyUpload(guidOpt: Option[String], f: Option[UploadInfo] => Unit) =
    guidOpt.filterNot(_.isEmpty) foreach { guid =>
      f(getUpload(guid))
    }

  private[domain] def updateIcon(uploadOpt: Option[UploadInfo]): Unit =
    getIcon.foreach(_.delete())
    self.setFavicon(uploadOpt.map(aws.createPublicAttachment(mediaFolder.getId, _)).orNull)

  private[domain] def updateLogo(uploadOpt: Option[UploadInfo]): Unit =
    getLogo.foreach(_.delete())
    self.setLogo(uploadOpt.map(aws.createPublicAttachment(mediaFolder.getId, _)).orNull)

  private[domain] def updateLogo2(uploadOpt: Option[UploadInfo]): Unit =
    getLogo2.foreach(_.delete())
    self.setLogo2(uploadOpt.map(aws.createPublicAttachment(mediaFolder.getId, _)).orNull)

  private[domain] def updateImage(uploadOpt: Option[UploadInfo]): Unit =
    getImage.foreach(_.delete())
    self.setImage(uploadOpt.map(aws.createPublicAttachment(mediaFolder.getId, _)).orNull)

  private[domain] def updateCss(uploadOpt: Option[UploadInfo]): Unit =
    uploadOpt foreach { upload =>
      if MediaType
          .parse(mws.getMimeType(upload.getFileName))
          .is(MediaType.ZIP)
      then
        // this should be ~ a zip site
        try dws.setCssZip(upload.getFileName, upload.getFile)
        catch
          case NonFatal(e) =>
            throw new ValidationException("cssUpload", upload.getFileName, e.getMessage).initCause(e)
      else dws.setImage(DomainConstants.DATA_TYPE_DOMAIN_CSS, upload.getFileName, null, null, upload.getFile)
    }

  private[domain] def updateRobots(uploadOpt: Option[UploadInfo]): Unit =
    getRobots foreach { _.delete }
    uploadOpt foreach { upload =>
      aws
        .createPublicAttachment(mediaFolder.getId, upload)
        .facade[AttachmentFacade]
        .setUrl("/robots.txt")
    }

  private[domain] def updateSitemap(uploadOpt: Option[UploadInfo]): Unit =
    getSitemap foreach { _.delete }
    uploadOpt foreach { upload =>
      aws
        .createPublicAttachment(mediaFolder.getId, upload)
        .facade[AttachmentFacade]
        .setUrl("/sitemap.xml")
    }

  private[domain] def updateMimeTypes(uploadOpt: Option[UploadInfo]): Unit =
    uploadOpt foreach { upload =>
      mws.setMimeTypes(upload.getFile)
    }
end DomainSettings

object DomainSettings:
  private def validateUploads(settings: DomainSettingsComponent)(implicit mws: MimeWebService): Unit =
    validateUpload(settings.getIconUpload, "iconUpload", MediaType.ANY_IMAGE_TYPE)
    validateUpload(settings.getLogoUpload, "logoUpload", MediaType.ANY_IMAGE_TYPE)
    validateUpload(settings.getCssUpload, "cssUpload", MediaType.ZIP, TEXT_CSS)
    validateUpload(settings.getRobotsUpload, "robotsUpload", TEXT_PLAIN)
    validateUpload(settings.getSitemapUpload, "sitemapUpload", TEXT_XML)
    validateUpload(settings.getMimeTypesUpload, "mimeTypesUpload", MIME_TYPES)

  // Uploads is a side channel, it should be an implicit SRS parameter

  private def getUpload(guid: String): Option[UploadInfo] =
    getUpload(Option(guid))

  private def getUpload(guid: Option[String]): Option[UploadInfo] =
    guid
      .filterNot(g => g.isEmpty || (g == "remove"))
      .map(guid => Uploads.retrieveUpload(guid)) // meh, throws

  private def validateUpload(guid: Option[String], field: String, types: MediaType*)(implicit mws: MimeWebService) =
    getUpload(guid)
      .map(upload => MediaType.parse(mws.getMimeType(upload.getFileName)))
      .filterNot(t => types.exists(t.is)) // ignore upload if type is acceptable
      .foreach(mediaType =>
        throw new ValidationException(field, mediaType.toString, "Expected " + types.mkString(" or "))
      )

  private val TEXT_CSS   = MediaType.CSS_UTF_8.withoutParameters
  private val TEXT_XML   = MediaType.XML_UTF_8.withoutParameters
  private val TEXT_PLAIN = MediaType.PLAIN_TEXT_UTF_8.withoutParameters
  private val MIME_TYPES = MediaType.create("application", "x-mime-types")

  private def mediaFolder(implicit fs: FacadeService) =
    "folder-media".facade[AttachmentParentFacade]
end DomainSettings
