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

package loi.cp.enrollment

import com.learningobjects.cpxp.component.query.{ApiFilter, ApiOrder}
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.de.web.QueryHandler

class OverallGradeHandler(courseId: Long, queryService: QueryService) extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit = filter.unsupported()

  override def applyOrder(qb: QueryBuilder, order: ApiOrder): Unit =
    val gqb = queryService.queryAllDomains("LWGrade")
    qb.addJoin(
      Join.Left(
        "#id",
        gqb,
        "LWGrade.user",
        DNF.apply(BaseCondition.getInstance("LWGrade.course", Comparison.eq, courseId))
      )
    )
    qb.addJoinOrder(
      gqb,
      BaseOrder.byJsonField("LWGrade.graphJson", "grades._root_.rollup.grade::DOUBLE PRECISION", order.getQbDirection)
    )
  end applyOrder
end OverallGradeHandler
