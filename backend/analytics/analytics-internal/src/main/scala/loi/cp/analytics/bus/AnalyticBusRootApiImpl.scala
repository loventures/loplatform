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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainDTO, DomainState}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.Join.Inner
import com.learningobjects.cpxp.service.query.{BaseDataProjection, Comparison, QueryService}
import org.hibernate.Session

import scala.jdk.CollectionConverters.*

/** Implementation of the analytic bus API root.
  */
@Component
class AnalyticBusRootApiImpl(
  val componentInstance: ComponentInstance,
  qs: QueryService,
  session: Session
)(implicit
  fs: FacadeService,
  domain: DomainDTO,
  cs: ComponentService,
  is: ItemService
) extends AnalyticBusRootApi
    with ComponentImplementation:
  override def getAnalyticBuses: Seq[AnalyticBus] =
    AnalyticBusServiceImpl.analyticBusFolder.queryAnalyticBuses
      .getComponents[AnalyticBus]

  override def getAnalyticBus(id: Long): Option[AnalyticBus] = id.tryComponent[AnalyticBus]

  override def pauseAnalyticBus(id: Long): Unit =
    id.tryComponent[AnalyticBus]
      .filter(_.getState == AnalyticBusState.Active)
      .foreach(_.setState(AnalyticBusState.Paused))

  override def resumeAnalyticBus(id: Long): Unit =
    id.tryComponent[AnalyticBus]
      .filter(_.getState != AnalyticBusState.Active)
      .foreach { a =>
        a.setFailureCount(0)
        a.setState(AnalyticBusState.Active)
      }

  override def getAnalyticBusesStatus: List[AnalyticBusDTO] =
    qs.queryAllDomains(AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS)
      .addJoin(
        Inner(
          DataTypes.META_DATA_TYPE_ROOT_ID,
          qs.queryAllDomains(DomainConstants.ITEM_TYPE_DOMAIN)
            .addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, Comparison.eq, DomainState.Normal)
            .setDataProjection(DomainConstants.DATA_TYPE_DOMAIN_ID)
        )
      )
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_STATE, Comparison.ne, AnalyticBusState.Disabled)
      .setDataProjection(
        BaseDataProjection.ofData(
          DataTypes.META_DATA_TYPE_ID,                                // bus id
          AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER, // bus name
          AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_STATE,             // bus state
          AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_FAILURE_COUNT      // bus fail count
        )
      )
      .getProjectedResults
      .asScala
      .map(row =>
        AnalyticBusDTO(
          row.get(AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS, DataTypes.META_DATA_TYPE_ID, classOf[Number]).longValue(),
          row.get(DomainConstants.ITEM_TYPE_DOMAIN, DomainConstants.DATA_TYPE_DOMAIN_ID, classOf[String]),
          row.get(
            AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS,
            AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SENDER_IDENTIFIER,
            classOf[String]
          ),
          AnalyticBusState.valueOf(
            row.get(
              AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS,
              AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_STATE,
              classOf[String]
            )
          ),
          row
            .get(
              AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS,
              AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_FAILURE_COUNT,
              classOf[Number]
            )
            .longValue()
        )
      )
      .toList

  override def pumpBus(id: Long): Unit = getAnalyticBus(id).foreach(_.pump())
end AnalyticBusRootApiImpl
