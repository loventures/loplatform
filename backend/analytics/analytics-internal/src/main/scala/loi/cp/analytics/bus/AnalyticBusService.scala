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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryBuilder
import loi.cp.analytics.AnalyticsSystem
import loi.cp.bus.BusStatistics
import scaloi.misc.TimeSource

@Service
trait AnalyticBusService:

  /** Creates an analytics bus for this system if it doesn't exist.
    *
    * @param system
    *   the analytics system
    */
  def createBus(system: AnalyticsSystem[? <: AnalyticsSystem[?]]): Unit

  /** Creates an analytics bus for the specified implementation.
    *
    * @param senderIdentifier:
    *   The identifier of the implementation used to send events
    */
  def createBus(senderIdentifier: String, state: AnalyticBusState, config: AnalyticBusConfiguration): AnalyticBusFacade

  /** Deletes an analytics bus for this system if it exists.
    *
    * @param system
    *   the analytics system
    */
  def deleteBus(system: AnalyticsSystem[? <: AnalyticsSystem[?]]): Unit

  def queryBuses(): QueryBuilder
end AnalyticBusService

@Service
class AnalyticBusServiceImpl(implicit fs: FacadeService, domain: () => DomainDTO, ts: TimeSource)
    extends AnalyticBusService:
  import AnalyticBusServiceImpl.*

  override def createBus(system: AnalyticsSystem[? <: AnalyticsSystem[?]]): Unit =
    analyticBusFolder.getOrCreateAnalyticBusBySystem(system) { bus =>
      logger info s"Creating analytics bus: ${system.getName}"
      bus.setScheduled(ts.date)
      bus.setWindowTime(ts.time)
      bus.setWindowIds(Nil)
      bus.setStatistics(BusStatistics.Zero)
      bus.setState(AnalyticBusState.Active)
      bus.setConfiguration(AnalyticBusConfiguration())
      bus.setFailureCount(0)
    }

  override def createBus(
    senderIdentifier: String,
    state: AnalyticBusState,
    config: AnalyticBusConfiguration
  ): AnalyticBusFacade =
    analyticBusFolder.addAnalyticBus { bus =>
      logger info s"Creating analytics bus for sender identifier: $senderIdentifier"
      bus.setSenderIdentifier(Some(senderIdentifier))
      bus.setScheduled(ts.date)
      bus.setWindowTime(ts.time)
      bus.setWindowIds(Nil)
      bus.setStatistics(BusStatistics.Zero)
      bus.setConfiguration(config)
      bus.setState(state)
      bus.setFailureCount(0)
    }

  override def deleteBus(system: AnalyticsSystem[? <: AnalyticsSystem[?]]): Unit =
    analyticBusFolder.findAnalyticBusBySystem(system) foreach { bus =>
      logger info s"Deleting analytics bus: ${system.getName}"
      bus.lock(true)
      bus.delete()
    }

  override def queryBuses(): QueryBuilder = analyticBusFolder.queryAnalyticBuses
end AnalyticBusServiceImpl

object AnalyticBusServiceImpl:
  private final val logger = org.log4s.getLogger

  private[bus] def analyticBusFolder(implicit fs: FacadeService, domain: DomainDTO): AnalyticBusParentFacade =
    domain.facade[AnalyticBusRootFacade].findFolderByType(AnalyticFolderType)

  private[bus] val AnalyticFolderType = "analytic"
