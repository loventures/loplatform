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

package loi.cp.role

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.TRIAL_LEARNER_ROLE_ID
import com.learningobjects.cpxp.service.relationship.RelationshipWebService
import loi.cp.role.SupportedRoleFacade.RightsList
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

import scala.jdk.CollectionConverters.*

/** Helpful utilities for startup tasks that create/update supported roles. Supported roles are items in a group/domain
  * that associate a role to a set of rights. At startup many are created/updated in the domain
  */
trait RoleStartupOps:

  def rs: RoleService

  def rws: RelationshipWebService

  def domain: DomainDTO

  type Right = Class[? <: loi.cp.right.Right]

  // if final, compiler fails with
  // "outer reference in this type test cannot be checked at run time"
  case class SupportedRoleInfo(
    roleName: String,
    rights: List[Right] = Nil,
    negativeRights: List[Right] = Nil
  )

  /** Creates roles. Note that roles are unused until some supported role item in a group or domain maps them to a set
    * of rights. To do that in the domain, use `putSupportedRolesInDomain`.
    */
  protected def putRoles(roleIds: String*): Unit =
    roleIds.foreach(roleId =>
      // I don't want to do this but ...
      val roleName = if roleId == TRIAL_LEARNER_ROLE_ID then "role-trial-learner" else s"role-$roleId"
      addNewRole(roleName, roleId)
    )

  private def roleFolder: Long = rws.getRoleFolder

  private def addNewRole(idStr: String, roleId: String): Unit =
    val role = Option(rws.getRoleByRoleId(roleFolder, roleId))
      .getOrElse(rws.addRole(roleFolder))
    role.setRoleId(roleId)
    role.setIdStr(idStr)
    role.setMsg(s"domain_role_$roleId")

  /** Create or update supported roles in the domain. A supported role maps a role id to a set of rights.
    */
  protected def putSupportedRolesInDomain(
    supportedRoles: SupportedRoleInfo*
  ): Unit =

    supportedRoles.foreach {
      case SupportedRoleInfo(NamedRole(role), rights, negativeRights) =>
        updateRole(role, rights, negativeRights)
      case SupportedRoleInfo(newRoleName, rights, negativeRights)     =>
        val newRightStrings =
          rightStrings(rights, negate = false) ::: rightStrings(negativeRights, negate = true)
        addRole(newRoleName, newRightStrings)
    }

  private def addRole(roleName: String, rightStrings: List[String]): Unit =
    rs.addSupportedRole(domain, roleName)
      .setRights(new RightsList(rightStrings.asJavaCollection))

  private def updateRole(
    role: SupportedRoleFacade,
    rights: List[Right],
    negativeRights: List[Right]
  ): Unit =
    var oldRights = role.getRights.asScala.toSet
    /* clear old positive (negative) rights which will be overwritten with a negative (positive) right */
    oldRights --= rightStrings(rights, negate = true)
    oldRights --= rightStrings(negativeRights, negate = false)
    /* then add the positive+negative rights */
    oldRights ++= rightStrings(rights, negate = false)
    oldRights ++= rightStrings(negativeRights, negate = true)
    role.setRights(new RightsList(oldRights.asJavaCollection))
  end updateRole

  private def rightStrings(rights: List[Right], negate: Boolean) =
    rights.map(right => negate ?? "-" + right.getName) // ::: negativeRights.map(right => "-" + right.getName)

  private object NamedRole:
    def unapply(idStr: String): Option[SupportedRoleFacade] =
      Option(rs.getSupportedRoles).toSeq
        .flatMap(roles => roles.asScala.toList)
        .find(sr => Option(sr.getRole).exists(_.getIdStr == idStr))
end RoleStartupOps
