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

package loi.cp.analytics.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.component.misc.AnalyticFinder.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.query.{Comparison as CMP, *}
import loi.cp.analytics.event.Event

import java.time.Instant

@Component(
  name = "Native Events for Integration Tests (DON'T ENABLE)",
  enabled = false
)
class AnalyticForTestingApiImpl(
  domain: => DomainDTO,
  queryService: QueryService,
  mapper: ObjectMapper,
  val componentInstance: ComponentInstance
) extends AnalyticForTestingApi
    with ComponentImplementation:

  def events(startingAt: Instant): Seq[Event] =
    queryService
      .queryRoot(ITEM_TYPE_DEAN_ANALYTIC)
      .addCondition(DATA_TYPE_DEAN_TIME, CMP.ge, startingAt)
      .addCondition(
        BaseCondition.jsonInstance(
          DATA_TYPE_DEAN_DATA_JSON,
          "source",
          CMP.eq,
          domain.hostName
        )
      )
      .setDataProjection(AnalyticConstants.DATA_TYPE_ANALYTIC_DATA_JSON)
      .getValues[AnyRef]
      .collect {
        case ev: ObjectNode => mapper.treeToValue(ev, classOf[Event])
        case ev: String     => mapper.readValue(ev, classOf[Event])
      }
end AnalyticForTestingApiImpl
