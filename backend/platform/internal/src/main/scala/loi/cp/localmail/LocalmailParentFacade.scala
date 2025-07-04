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

package loi.cp.localmail

import java.util.Date

import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.localmail.LocalmailFinder.*
import com.learningobjects.cpxp.service.query.{Comparison, Direction, Function}

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait LocalmailParentFacade extends Facade:
  @FacadeChild
  @FacadeQuery(orderType = DATA_TYPE_LOCALMAIL_DATE, orderDirection = Direction.DESC)
  def findLocalmailsByToAddress(
    @FacadeCondition(value = DATA_TYPE_LOCALMAIL_TO_ADDRESS, function = Function.LOWER)
    emailAddress: String,
    @FacadeCondition(value = DATA_TYPE_LOCALMAIL_DATE, comparison = Comparison.gt)
    date: Date,
  ): List[LocalmailFacade]

  @FacadeQuery(orderType = DATA_TYPE_LOCALMAIL_DATE, orderDirection = Direction.DESC)
  def findLocalmails(
    @FacadeCondition(value = DATA_TYPE_LOCALMAIL_DATE, comparison = Comparison.gt)
    date: Date,
  ): List[LocalmailFacade]

  def addLocalmail(): LocalmailFacade
end LocalmailParentFacade
