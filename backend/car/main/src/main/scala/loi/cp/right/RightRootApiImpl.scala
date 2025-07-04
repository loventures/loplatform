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

package loi.cp.right

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.admin.right.{AdminRight, HostingAdminRight}
import loi.cp.course.right.CourseRight
import loi.cp.right.RightRootApi.{RightDesc, RoleRightMap}
import loi.cp.role.SupportedRoleFacade.RightsList
import loi.cp.role.{DomainRoleService, SupportedRole, SupportedRoleFacade}
import scalaz.\/
import scaloi.syntax.BooleanOps.*

import scala.jdk.CollectionConverters.*

@Component
class RightRootApiImpl(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  rightService: RightService,
  domainRoleService: DomainRoleService,
  componentEnvironment: ComponentEnvironment
) extends RightRootApi
    with ComponentImplementation:

  override def getRightsTree: RightDTO =
    rightToRightDTO(apexRight)

  override def getCourseRightTree: RightDTO = rightToRightDTO(classOf[CourseRight])

  override def getAllRights: List[RightDesc] =
    rightService.getDescendants(apexRight).asScala.map(rightToRightDesc).toList

  override def getCourseRights: List[RightDesc] =
    rightService.getDescendants(classOf[CourseRight]).asScala.map(rightToRightDesc).toList

  private def apexRight =
    if rightService.getUserHasRight(classOf[HostingAdminRight]) then classOf[HostingAdminRight] else classOf[AdminRight]

  override def updateRights(roleRightMap: RoleRightMap): ErrorResponse \/ Seq[SupportedRole] =
    val roleIdsToRights  = roleRightMap.rolesToRights.map(entry => entry._1.toLong -> entry._2)
    val facadesToRights  =
      roleIdsToRights.map(tuple => Option(fs.getFacade(tuple._1, classOf[SupportedRoleFacade])) -> tuple._2)
    val supportedFacades = facadesToRights.keys
    for
      _ <- !supportedFacades.exists(_.isEmpty) \/> ErrorResponse.notFound
      _ <- supportedFacades.forall(srf =>
             srf.exists(rf => rightService.isSuperiorToRole(rf.getRole))
           ) \/> ErrorResponse.forbidden
    yield
      facadesToRights foreach { tuple =>
        tuple._1 foreach { srf =>
          srf.setRights(new RightsList(tuple._2.asJavaCollection))
        }
      }
      domainRoleService.getRoles(roleRightMap.contextId)
    end for
  end updateRights

  private def sortByName(o1: Class[? <: Right], o2: Class[? <: Right]): Boolean =
    val binding1 = rightService.getRightBinding(o1)
    val binding2 = rightService.getRightBinding(o2)
    ComponentUtils.i18n(binding1.name, componentDescriptor) > ComponentUtils.i18n(binding2.name, componentDescriptor)

  private def rightToRightDesc(right: Class[? <: Right]): RightDesc =
    val binding     = rightService.getRightBinding(right)
    val description = ComponentUtils.i18n(binding.description, componentDescriptor)
    val name        = ComponentUtils.i18n(binding.name, componentDescriptor)
    RightDesc(name, description, right)

  private def rightToRightDTO(right: Class[? <: Right]): RightDTO =
    val rightDesc = rightToRightDesc(right)
    val children  =
      rightService.getChildren(right).asScala.toList.sortWith(sortByName).map(right => rightToRightDTO(right))
    RightDTO(rightDesc.name, rightDesc.description, "", right, children)

  private def componentDescriptor = componentInstance.getComponent
end RightRootApiImpl

object RightRootApiImpl:
  case class ClassDepth(depth: Int, right: Class[? <: Right])
