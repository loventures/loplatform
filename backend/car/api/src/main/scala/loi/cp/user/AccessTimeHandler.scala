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

import com.learningobjects.cpxp.component.query.ApiOrder
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.user.UserHistoryFinder
import com.learningobjects.de.web.QueryHandler

class AccessTimeHandler(qs: QueryService) extends QueryHandler:
  override def applyOrder(qb: QueryBuilder, ao: ApiOrder): Unit =
    val hqb = qb.getOrAddJoinQuery(
      "#id",
      UserHistoryFinder.ITEM_TYPE_USER_HISTORY,
      Join.Left(
        "#id",
        qs.queryAllDomains(UserHistoryFinder.ITEM_TYPE_USER_HISTORY),
        "#parent"
      )
    )
    qb.addJoinOrder(
      hqb,
      BaseOrder
        .byData(
          UserHistoryFinder.DATA_TYPE_ACCESS_TIME,
          ao.getQbDirection
        )
        .coalesceWith(
          qb,
          DataTypes.DATA_TYPE_CREATE_TIME
        )
    )
  end applyOrder
end AccessTimeHandler
