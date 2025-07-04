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

package loi.cp.context

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import loi.cp.enrollment.RosterWebController
import loi.cp.user.{ContextProfilesApi, Profile}

@Component
class ContextUsers(
  val componentInstance: ComponentInstance,
  course: CourseContextComponent,
)(
  roster: RosterWebController,
) extends ContextProfilesApi
    with ComponentImplementation:
  override def get(query: ApiQuery): ApiQueryResults[Profile] =
    roster
      .getEnrolledUsers(course.getId, query)
      .map(_.getUser.component[Profile])

  override def get(id: Long): Option[Profile] =
    get(ApiQuery.byId(id)).asOption
end ContextUsers
