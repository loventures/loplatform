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

package loi.authoring.feedback

import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import loi.authoring.web.AuthoringWebUtils
//import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
//import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.item.ItemService
//import com.learningobjects.cpxp.service.query.{Comparison, Projection}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
//import loi.authoring.security.right.EditContentAnyProjectRight
import loi.cp.right.RightService
import loi.cp.role.RoleService
import loi.cp.web.HandleService

//import scala.jdk.CollectionConverters._

@Component
@Controller(root = true)
@RequestMapping(path = "metaFeedback")
class MetaFeedbackWebController(
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  user: UserDTO,
)(implicit
  is: ItemService,
  hs: HandleService,
  roleService: RoleService,
  rightService: RightService,
  enrollmentService: EnrollmentWebService,
  authoringWebUtils: AuthoringWebUtils
) extends ApiRootComponent
    with ComponentImplementation:
  @RequestMapping(path = "branches/{branchId}/assignees", method = Method.GET)
  def branchAssignees(@PathVariable("branchId") branchId: Long): List[FeedbackProfileDto] =
    // because why would any sane api to get this data exist...
//    val roles            = roleService.getDomainRoles.asScala
//    val authoringRoles   =
//      roles.filter(role => rightService.getRoleRights(role).contains(classOf[EditContentAnyProjectRight]))
//    val enrollmentsQuery = enrollmentService
//      .getDomainActiveUsersByRolesQuery(authoringRoles.map(_.getId).asJava, Projection.PARENT_ID)
//    val authors          = enrollmentService
//      .getEnrollmentUsersQuery(enrollmentsQuery, EnrollmentType.ALL)
//      .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false)
//      .setProjection(Projection.ID)
//      .getValues[Long]

    val project      = authoringWebUtils.branchOrFakeBranchOrThrow404(branchId).requireProject
    val projectUsers = project.userIds
    val allUsers     = projectUsers // ++ authors
    allUsers.toList.flatMap(_.finder_?[UserFinder]).map(FeedbackProfileDto.apply)
  end branchAssignees
end MetaFeedbackWebController
