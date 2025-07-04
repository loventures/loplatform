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

import com.learningobjects.cpxp.component.query.{ApiFilter, ApiOrder, PredicateOperator}
import com.learningobjects.cpxp.service.query.{Function as QBFunction, *}
import com.learningobjects.cpxp.service.user.UserConstants
import com.learningobjects.de.web.QueryHandler
import scaloi.syntax.AnyOps.*

/** Implements filtering on full name; for example, filter=fullName:ts(Bob Dobbs).
  */
class UserFullNameHandler extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit =
    qb.addCondition(asCondition(filter))

  override def asCondition(filter: ApiFilter): Condition =
    if Option(filter.getOperator).exists(_ != PredicateOperator.TEXT_SEARCH) then filter.unsupported()
    BaseCondition.getInstance(UserConstants.DATA_TYPE_FULL_NAME, Comparison.search, filter.getValue) <| { cond =>
      cond.setLanguage(Condition.LANGUAGE_SIMPLE)
    }

  // Efficiency questions abound. Given an index on parent_id, lower(familyname) where del is null,
  // postgresql can use said index an does in local. But in practice on prod it just doesn't.
  override def applyOrder(qb: QueryBuilder, order: ApiOrder): Unit =
    OrderTypes foreach { t =>
      qb.addOrder(BaseOrder.byData(t, QBFunction.LOWER, order.getQbDirection))
    }

  private final val OrderTypes = List(
    UserConstants.DATA_TYPE_FAMILY_NAME,
    UserConstants.DATA_TYPE_GIVEN_NAME,
    UserConstants.DATA_TYPE_MIDDLE_NAME,
    UserConstants.DATA_TYPE_USER_NAME
  )
end UserFullNameHandler
