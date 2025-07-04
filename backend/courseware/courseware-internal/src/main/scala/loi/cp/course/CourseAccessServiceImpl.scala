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

package loi.cp.course

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.cpxp.util.cache.{BucketGenerationalCache, Entry}
import loi.authoring.security.right.{AccessAuthoringAppRight, AllAuthoringActionsRight, ViewAllProjectsRight}
import loi.cp.admin.right.*
import loi.cp.context.ContextId
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.right.*
import loi.cp.right.*
import loi.cp.user.{RestrictedLearner, UserComponent, UserService}
import scaloi.syntax.classTag.*
import scaloi.syntax.option.*

import java.lang
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/** Implementation of [[CourseAccessService]]. */
@Service
class CourseAccessServiceImpl(
  currentUser: => UserDTO,
  currentDomain: => DomainDTO,
  rightService: RightService,
  restrictedLearnerCache: RestrictedLearnerCache
)(implicit cs: ComponentService, userService: UserService, qs: QueryService)
    extends CourseAccessService:

  import CourseAccessService.*

  override def getUserHasAccess(
    crs: ContextId,
  ): Boolean =
    Current.isRoot || { // isRoot needed for destrap scripts
      val course              = crs.component[CourseComponent]
      val courseId: ContextId = ContextId(course.getId)
      hasAnyRight(courseId, AlwaysViewRights*) || (
        hasLearnerAccess(courseId)
          && course.hasCourseStarted
          && !course.hasCourseShutdown
      )
    }

  override def actualRights(course: ContextId, user: UserId): Option[CourseRights] =
    val rights                 = rightService
      .getUserRights(course, user)
      .asScala
      .toSet
    val domainRights           = rightService
      .getUserRights(currentDomain, user)
      .asScala
      .toSet
    // If you have some authoring access but not to this specific project then
    // we strip out all your authoring rights so author-restricted content
    // remains inaccessible. This only applies to restricted authors so has
    // minimal aggregate cost.
    val reducedAuthoringRights =
      if domainRights(classOf[AccessAuthoringAppRight]) &&
        !domainRights(classOf[ViewAllProjectsRight]) &&
        !hasProjectAccess(course, user)
      then domainRights.filterNot(classOf[AllAuthoringActionsRight].isAssignableFrom)
      else domainRights
    Some(rights | reducedAuthoringRights).filterNot(_.isEmpty).map(CourseRights(_, isRestrictedLearner(course, user)))
  end actualRights

  private def hasProjectAccess(course: ContextId, user: UserId): Boolean =
    course.component[LightweightCourse].loadBranch().requireProject.userIds.contains(user.id)

  override def isRestrictedLearner(course: ContextId, user: UserId): Boolean =
    restrictedLearnerCache.isRestrictedLearner(currentDomain, course, user)

  override def getUserRights(
    course: ContextId,
    user: UserComponent,
  ): Set[Class[? <: Right]] =
    (
      if user.getId == currentUser.id && hasAdminAccess(course) then rightService.getDescendants(classOf[CourseRight])
      else rightService.getUserRights(course, user)
    ).asScala.toSet

  override def hasInstructorAccess(course: ContextId): Boolean =
    hasRight[TeachCourseRight](course) || hasAdminAccess(course)

  override def hasAdvisorAccess(course: ContextId): Boolean =
    hasRight[ViewCourseGradeRight](course) || hasAdminAccess(course)

  override def hasAdminAccess(course: ContextId): Boolean =
    hasAnyRight(course, classOf[CourseAdminRight], classOf[IntegrationAdminRight])

  override def hasAllContentAccess(course: ContextId): Boolean =
    hasRight[ContentCourseRight](course)

  override def hasLearnerAccess(course: ContextId): Boolean =
    hasRight[LearnCourseRight](course)

  override def hasReadAccess(course: ContextId): Boolean =
    hasRight[ReadCourseRight](course)

  override def hasInteractAccess(course: ContextId): Boolean =
    hasRight[InteractCourseRight](course)

  private def hasRight[R <: Right: ClassTag](course: ContextId): Boolean =
    hasAnyRight(course, classTagClass[R])

  private def hasAnyRight(course: ContextId, rights: Class[? <: Right]*): Boolean =
    val userRights = rightService.getUserRightsInPedigree(course)
    rights.exists(userRights.contains)
end CourseAccessServiceImpl

class RestrictedLearnerCache
    extends BucketGenerationalCache[(Long, Long), lang.Boolean, RestrictedLearnerEntry](
      itemAware = false,
      replicated = false,
      timeout = 5.minutes
    ):

  def isRestrictedLearner(domain: DomainDTO, course: ContextId, user: UserId)(implicit
    us: UserService,
    qs: QueryService
  ): Boolean =
    def restricted = for
      userDto <- us.getUser(user.id)
      email   <- Option(userDto.emailAddress)
    yield RestrictedLearner.isRestricted(domain, email)
    getOrCompute(() => new RestrictedLearnerEntry(course.id -> user.id, restricted.isTrue), course.id -> user.id)
end RestrictedLearnerCache

final class RestrictedLearnerEntry(key: (Long, Long), value: lang.Boolean) extends Entry(key, value)
