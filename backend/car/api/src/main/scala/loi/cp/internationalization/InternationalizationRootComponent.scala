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

package loi.cp.internationalization

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.util.Out
import com.learningobjects.de.authorization.Secured
import loi.cp.language.LocaleInfo

import scalaz.\/

/** Root controller for managing i18n. */
@Controller(value = "internationalization", root = true)
@RequestMapping(path = "i18n")
@Secured(allowAnonymous = true)
trait InternationalizationRootComponent extends ApiRootComponent:
  @RequestMapping(path = "{locale}/{component}/meta", method = Method.GET)
  def i18nMeta(
    @PathVariable("locale") locale: String,
    @PathVariable("component") component: String
  ): ErrorResponse \/ I18nMeta

  @RequestMapping(path = "{locale}/{component}", method = Method.GET, csrf = false)
  def i18nMessages(
    @PathVariable("locale") locale: String,
    @PathVariable("component") component: String,
    cacheOptions: Out[CacheOptions]
  ): ErrorResponse \/ Lazy[Map[String, String]]
end InternationalizationRootComponent

case class I18nMeta(component: String, locale: LocaleInfo, availableLocales: Seq[LocaleInfo])
