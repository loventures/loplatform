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

package loi.cp.usage

import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.usage.DomainStatsFinder.*

import java.time.Instant

@FacadeItem(ITEM_TYPE_DAILY_DOMAIN_STATISTICS)
trait DomainStatisticsFacade extends Facade:

  @FacadeData(DATA_TYPE_TYPE)
  def getType: String
  def setType(`type`: String): Unit

  @FacadeData(DATA_TYPE_TIME)
  def getTime: Instant
  def setTime(time: Instant): Unit

  @FacadeData(DATA_TYPE_VALUE)
  def getValue: Long
  def setValue(value: Long): Unit
end DomainStatisticsFacade
