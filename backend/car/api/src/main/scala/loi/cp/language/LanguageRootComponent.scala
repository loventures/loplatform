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

import com.learningobjects.cpxp.component.RestfulRootComponent
import com.learningobjects.cpxp.component.annotation.{Controller, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

import java.util.Locale

/** Root controller for managing languages. */
@Controller(value = "languages", root = true)
@RequestMapping(path = "languages")
@Secured(Array(classOf[AdminRight]))
trait LanguageRootComponent extends RestfulRootComponent[LanguageComponent]:

  /** Update domain-wide 18n settings. Primary for int tests. */
  @RequestMapping(path = "settings", method = Method.PUT)
  def updateSettings(@RequestBody settings: LanguageSettings): Unit

  // This really belongs, along with timeZones, on a ServerRootComponent
  @RequestMapping(path = "locales", method = Method.GET)
  def locales: Seq[LocaleInfo]

  @RequestMapping(path = "timeZones", method = Method.GET)
  def timeZones: Seq[TimeZoneInfo]

  def installOrReplace(name: String, locale: String, country: Option[String], upload: UploadInfo): Unit
end LanguageRootComponent

case class LocaleInfo(id: String, name: String, localizedName: String, language: String, country: String)

object LocaleInfo:
  def apply(l: Locale, displayLocale: Locale): LocaleInfo =
    new LocaleInfo(
      l.toLanguageTag,
      l.getDisplayName(displayLocale),
      l.getDisplayName(l),
      l.getLanguage,
      l.getCountry,
    )

  def apply(l: Locale, displayName: String): LocaleInfo =
    new LocaleInfo(
      l.toLanguageTag,
      displayName,
      displayName,
      l.getLanguage,
      l.getCountry,
    )
end LocaleInfo

case class TimeZoneInfo(id: String, name: String)

case class LanguageSettings(defaultLanguage: String)
