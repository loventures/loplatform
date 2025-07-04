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

package loi.cp.user

import com.learningobjects.cpxp.component.query.{ApiFilter, ApiQuerySupport}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.integration.IntegrationFinder
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.de.web.{QueryHandler, Queryable}

import scala.jdk.CollectionConverters.*

class UniqueIdHandler(qs: QueryService) extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, af: ApiFilter): Unit =
    val uqb = qs
      .queryRoot(IntegrationFinder.ITEM_TYPE_INTEGRATION)
      .addCondition(ApiQuerySupport.getApplyFilterCondition(IntegrationFinder.DATA_TYPE_UNIQUE_ID, CaseInsensitive, af))
      .setProjection(Projection.PARENT_ID)
    qb.addCondition(BaseCondition.inQuery(DataTypes.META_DATA_TYPE_ID, uqb))

  private final val CaseInsensitive = List(Queryable.Trait.CASE_INSENSITIVE).asJava
