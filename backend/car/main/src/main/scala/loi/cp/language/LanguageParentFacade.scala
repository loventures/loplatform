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
import com.learningobjects.cpxp.service.folder.{FolderConstants, FolderFacade}
import com.learningobjects.cpxp.service.internationalization.InternationalizationConstants.*
import com.learningobjects.cpxp.service.query.QueryBuilder
import scaloi.GetOrCreate

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait LanguageParentFacade extends FolderFacade:
  @FacadeComponent
  def addLanguage[T <: LanguageComponent](language: T): T
  def getLanguage(id: Long): Option[LanguageComponent]
  def getLanguages: List[LanguageComponent]
  def queryLanguages: QueryBuilder

  @FacadeComponent
  def getOrCreateLanguage(
    @FacadeCondition(DataTypes.DATA_TYPE_NAME) name: String,
    @FacadeCondition(DATA_TYPE_INTERNATIONALIZATION_LOCALE) locale: String,
    language: LanguageComponent,
  ): GetOrCreate[LanguageComponent]
end LanguageParentFacade
