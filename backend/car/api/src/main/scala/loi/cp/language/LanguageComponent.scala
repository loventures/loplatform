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

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.RestfulComponent
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{Method, WebRequest, WebResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.internationalization.InternationalizationConstants.*
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait

import javax.validation.groups.Default

/** A language. */
@Schema("language")
@ItemMapping(value = ITEM_TYPE_INTERNATIONALIZATION, singleton = true)
trait LanguageComponent extends RestfulComponent[LanguageComponent]:

  /** The name of this language pack). */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonProperty
  def getName: String

  /** The language (en). */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonProperty
  def getLanguage: String

  /** The country (US). */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonProperty
  def getCountry: Option[String]

  /** Is disabled. */
  @Queryable(traits = Array(Trait.NOT_SORTABLE))
  @JsonProperty
  def isDisabled: Boolean

  /** The full name of this language. */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonView(Array(classOf[Default]))
  def getLanguageName: String

  /** The full name of this language. */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonView(Array(classOf[Default]))
  def getLanguageCode: String

  @RequestMapping(path = "messages", method = Method.GET)
  def getMessages: Map[String, String]

  @RequestMapping(path = "messages", method = Method.PUT)
  def setMessages(@RequestBody messages: Map[String, String]): Unit

  @RequestMapping(path = "upload", method = Method.POST)
  def upload(@RequestBody upload: UploadInfo): Unit

  @RequestMapping(path = "download", method = Method.GET, csrf = false)
  def download(request: WebRequest): WebResponse
end LanguageComponent
