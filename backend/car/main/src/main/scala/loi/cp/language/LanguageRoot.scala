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

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.BeanProxy.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.internationalization.InternationalizationConstants.*
import com.learningobjects.cpxp.service.language.LanguageService
import com.learningobjects.cpxp.util.{Encheferize, InternationalizationUtils}
import scalaz.std.list.listMonoid
import scalaz.std.string.stringInstance
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.AnyOps.*

import java.util.{Locale, TimeZone}

@Component
class LanguageRoot(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  ls: LanguageService,
  sm: ServiceMeta
) extends LanguageRootComponent
    with ComponentImplementation:
  import LanguageRoot.*

  override def updateSettings(settings: LanguageSettings): Unit =
    ls.setDefaultLanguage(settings.defaultLanguage)

  override def get(q: ApiQuery): ApiQueryResults[LanguageComponent] =
    ApiQueryUtils.query(languageFolder.getLanguages, q)

  override def get(id: Long): Option[LanguageComponent] =
    languageFolder.getLanguage(id)

  override def create[T <: LanguageComponent](language: T): T =
    val folder = languageFolder
    val lang   = folder.addLanguage(language)
    ls.invalidateDomainMessages()
    lang

  // threadlocal sidechannel badness, could pull locale from explicit web request param
  override def locales: Seq[LocaleInfo] =
    val installedLocales = Locale.getAvailableLocales
      .map(l => LocaleInfo(l, InternationalizationUtils.getLocale))
      .toSeq
      .filter(_.language.nonEmpty) // filter out "undefined"
    val extraLocales = (sm.isProdLike !? Encheferize.Locales).map { case (l, dn) => LocaleInfo(l, dn) }

    (installedLocales ++ extraLocales) sortBy (_.id)

  // threadlocal sidechannel badness, could pull locale from explicit web request param
  override def timeZones: Seq[TimeZoneInfo] =
    TimeZone.getAvailableIDs.toSeq
      .map(t => TimeZone.getTimeZone(t))
      .map(t => TimeZoneInfo(t.getID, t.getDisplayName(InternationalizationUtils.getLocale)))

  override def installOrReplace(name: String, language: String, country: Option[String], upload: UploadInfo): Unit =
    val locale: Locale = Locale.forLanguageTag(language + country.orZero.transformNZ("-".concat))
    val lang           = languageFolder.getOrCreateLanguage(
      name,
      locale.toLanguageTag,
      LanguageInfo(name, language, country, false)
        .beanProxy[LanguageComponent]
    )
    lang.result.upload(upload)
    ls.invalidateDomainMessages()
  end installOrReplace
end LanguageRoot

case class LanguageInfo(name: String, language: String, country: Option[String], disabled: Boolean)

object LanguageRoot:
  def languageFolder(implicit fs: FacadeService): LanguageParentFacade =
    FOLDER_ID_INTERNATIONALIZATION.facade[LanguageParentFacade]
