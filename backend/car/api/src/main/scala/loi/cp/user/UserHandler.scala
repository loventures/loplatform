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

import com.learningobjects.cpxp.component.query.ApiFilter
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentConstants, EnrollmentWebService}
import com.learningobjects.cpxp.service.query.{BaseCondition, QueryBuilder}
import com.learningobjects.cpxp.service.user
import com.learningobjects.de.web.QueryHandler

class UserHandler(
  enrollmentService: EnrollmentWebService,
  userService: user.UserService
) extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, af: ApiFilter): Unit =
    val userOp = Option(userService.get(af.getValue.toLong))
    userOp
      .map(user =>
        enrollmentService
          .getUserEnrollmentsQuery(user.getId, EnrollmentType.ACTIVE_ONLY)
          .setDataProjection(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP)
      )
      .map(eqb => qb.addCondition(BaseCondition.inQuery(DataTypes.META_DATA_TYPE_ID, eqb)))
      .getOrElse(qb.setNoResults())
  end applyFilter
end UserHandler
