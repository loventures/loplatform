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
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.de.web.QueryHandler

import scala.jdk.CollectionConverters.*

class DomainRoleHandler(qs: QueryService, domain: DomainDTO) extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, af: ApiFilter): Unit =
    val roleQuery = qs
      .queryAllDomains(EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
      .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, Comparison.eq, domain)
      .addCondition(
        ApiQuerySupport.getApplyFilterCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, List.empty.asJava, af)
      )
      .setProjection(Projection.PARENT_ID)
    qb.addCondition(BaseCondition.inQuery(DataTypes.META_DATA_TYPE_ID, roleQuery))
end DomainRoleHandler
