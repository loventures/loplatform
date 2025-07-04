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

package loi.cp.util

import com.learningobjects.cpxp.component.{ComponentService, ComponentSupport}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.entity.ExternallyIdentifiableEntity
import loi.cp.analytics.event.TimeSpentEvent2
import loi.cp.course.CourseSectionService
import loi.cp.group.LightweightGroupService
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.storage.CourseStorageService
import loi.cp.user.UserComponent

import scala.concurrent.{ExecutionContext, Future}

class ActorAnalyticsServiceImpl extends ActorAnalyticsService:
  implicit val ec: ExecutionContext = ExecutionContext.global

  override def emitEventWithBuilder(userId: Long, sectionId: Option[Long])(builder: EventBuilder): Unit =
    Future {
      ManagedUtils.perform { () =>
        val dws           = ManagedUtils.getService(classOf[DomainWebService])
        dws.setupUserContext(userId)
        val as            = ComponentSupport.lookupService(classOf[AnalyticsService])
        implicit val cs   = ComponentSupport.lookupService(classOf[ComponentService])
        val groupService  = ComponentSupport.lookupService(classOf[LightweightGroupService])
        val userDto       = userId.component[UserComponent].toDTO
        val user          = as.userData(userDto)
        val courseSection = sectionId.flatMap(id => groupService.fetchSectionGroup(id))
        val section       = courseSection.map(c =>
          ExternallyIdentifiableEntity(
            id = c.id,
            externalId = c.externalId
          )
        )
        val commitId      = courseSection.map(_.commitId)

        val event = builder(Current.getDomainDTO, user, section, commitId)

        as.emitEvent(event)

        // Replicate timespent from a courseLink.1 to the parent course. Alternately
        // we could do this in the FE or in the FE's an/emit endpoint. This will only
        // replicate a single step of a course link chain.
        event match
          case ts: TimeSpentEvent2 =>
            val courseStorageService = ComponentSupport.lookupService(classOf[CourseStorageService])
            val courseSectionService = ComponentSupport.lookupService(classOf[CourseSectionService])
            for
              section  <- courseSection
              callback <- courseStorageService.get[UserGradeSyncHistory](section, userDto).courseLinkData
              dest     <- courseSectionService.getCourseSection(callback.section)
              content  <- dest.contents.get(callback.edgePath)
            do
              as.emitEvent(
                ts.copy(
                  context = ExternallyIdentifiableEntity(id = dest.id, externalId = dest.externalId),
                  commitId = Some(dest.commitId),
                  edgePath = Some(content.edgePath.toString),
                  assetId = Some(content.asset.info.id),
                  originSectionId = Some(section.id),
                )
              )
            end for

          case _ =>
        end match
      }
    }
end ActorAnalyticsServiceImpl
