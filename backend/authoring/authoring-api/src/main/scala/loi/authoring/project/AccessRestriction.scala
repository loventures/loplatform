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

package loi.authoring.project
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.security.right.ViewAllProjectsRight
import loi.cp.user.UserService
import scaloi.syntax.classTag.*

import scala.reflect.ClassTag

/** Some security for authoring clients that goes beyond @Secured. Courseware clients should use
  * [[AccessRestriction.none]] probably.
  */
sealed trait AccessRestriction:
  def pass(user: UserDTO, owner: Long, contributors: Set[Long], userService: UserService): Boolean

  // nonstrict `project` because of preponderance of `AccessRestriction.none` usage.
  // (and my local having many `assetbranch.project_id` null or deleted `assetproject`)
  def pass(user: UserDTO, project: => Project, userService: UserService): Boolean =
    pass(user, project.ownedBy, project.contributedBy.keySet, userService)

object AccessRestriction:

  private val logger: org.log4s.Logger = org.log4s.getLogger

  /** All users can access
    */
  val none: AccessRestriction = new AccessRestriction:
    override def pass(user: UserDTO, owner: Long, contributors: Set[Long], userService: UserService): Boolean =
      true

    override def pass(user: UserDTO, project: => Project, userService: UserService): Boolean = true

    override def toString: String = "AccessRestriction.none"

  /** Project owner or contributors or ViewAllProjectsRight holders can access
    */
  val projectMember: AccessRestriction = projectMemberOr[ViewAllProjectsRight]

  /** Project owner or contributors or A holders can access
    */
  def projectMemberOr[A <: loi.cp.right.Right: ClassTag]: AccessRestriction = new AccessRestriction:
    override def pass(user: UserDTO, owner: Long, contributors: Set[Long], userService: UserService): Boolean =
      val isMember      = owner == user.id || contributors.contains(user.id)
      lazy val hasRight = userService.userHasDomainRight[A](user.id)
      if isMember || hasRight then true
      else
        logger.info(s"access denied; is member: $isMember; has ${classTagClass[A].getName}: $hasRight")
        false

    override def toString: String = s"AccessRestriction.projectMemberOr[${classTagClass[A].getName}]"

  /** Project owner or A holders can access
    */
  def projectOwnerOr[A <: loi.cp.right.Right: ClassTag]: AccessRestriction = new AccessRestriction:
    override def pass(user: UserDTO, owner: Long, contributors: Set[Long], userService: UserService): Boolean =
      val isOwner       = owner == user.id
      lazy val hasRight = userService.userHasDomainRight[A](user.id)
      if isOwner || hasRight then true
      else
        logger.info(s"access denied; is owner: $isOwner; has ${classTagClass[A].getName}: $hasRight")
        false

    override def toString: String = s"AccessRestriction.projectOwnerOr[${classTagClass[A].getName}"
end AccessRestriction
