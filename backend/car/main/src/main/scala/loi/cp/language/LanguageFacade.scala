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

import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.internationalization.InternationalizationConstants.*

@FacadeItem(ITEM_TYPE_INTERNATIONALIZATION)
trait LanguageFacade extends Facade:
  @FacadeData(value = DataTypes.DATA_TYPE_NAME)
  def getName: String
  def setName(name: String): Unit

  @FacadeData(value = DataTypes.DATA_TYPE_DISABLED)
  def getDisabled: Option[Boolean] // legacy languages have null disabled flag
  def setDisabled(disabled: Boolean): Unit

  @FacadeData(value = DATA_TYPE_INTERNATIONALIZATION_LOCALE)
  def getLocale: String
  def setLocale(name: String): Unit

  @FacadeData(value = DATA_TYPE_INTERNATIONALIZATION_MESSAGES)
  def getMessages: Map[String, String]
  def setMessages(messages: Map[String, String]): Unit

  @FacadeParent
  def getParent: LanguageParentFacade
end LanguageFacade
