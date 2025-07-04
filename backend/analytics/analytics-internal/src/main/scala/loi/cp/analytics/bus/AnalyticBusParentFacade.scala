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

package loi.cp.analytics.bus

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.dto.{Facade, FacadeChild, FacadeCondition, FacadeItem}
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.query.QueryBuilder

/** Analytic bus parent facade. Binds to the analytics folder.
  */
@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
trait AnalyticBusParentFacade extends Facade:
  import AnalyticConstants.*

  @FacadeChild
  def queryAnalyticBuses: QueryBuilder
  def findAnalyticBusBySystem(
    @FacadeCondition(DATA_TYPE_ANALYTIC_BUS_SYSTEM) system: Id
  ): Option[AnalyticBusFacade]
  def getOrCreateAnalyticBusBySystem[A](
    @FacadeCondition(DATA_TYPE_ANALYTIC_BUS_SYSTEM) system: Id
  )(init: AnalyticBusFacade => A): AnalyticBusFacade

  def addAnalyticBus[A](init: AnalyticBusFacade => A): AnalyticBusFacade
end AnalyticBusParentFacade
