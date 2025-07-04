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

package loi.cp.content
package gate

import java.time.Instant
import java.lang as jl

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.{EnrollmentFacade, EnrollmentWebService}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.appevent.{AppEvent, OnEvent, OnEventBinding, OnEventComponent}
import loi.cp.course.lightweight.Lwc
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.notification.NotificationService
import loi.cp.storage.{CourseStorageService, CourseStoreable}
import loi.cp.user.UserComponent
import scalaz.std.function.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.std.tuple.*
import scalaz.syntax.bitraverse.*
import scalaz.syntax.profunctor.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.syntax.OptionOps.*
import scaloi.syntax.TryOps.*

import scala.jdk.CollectionConverters.*
import scala.util.Try

/** Event scheduled for the next gate date event in an instructor-led or self-study course. */
case class GateDateEvent(course: Long, user: Option[jl.Long]) extends AppEvent

/** Process gate date events. */
@Component
@OnEventBinding(Array(classOf[GateDateEvent]))
class GateDateEventListener(val componentInstance: ComponentInstance, domain: => DomainDTO)(implicit
  courseSectionService: CourseSectionService,
  enrollmentService: EnrollmentWebService,
  courseStorageService: CourseStorageService,
  notificationService: NotificationService,
  ts: TimeSource,
  cs: ComponentService
) extends OnEventComponent
    with ComponentImplementation:

  import GateDateEventListener.*

  @OnEvent
  def onGateDateEvent(event: GateDateEvent): Option[Instant] =
    log info s"Executing $event"
    onGateEvent(event.course, event.user).get

  private def onGateEvent(cid: Long, uid: Option[jl.Long]): Try[Option[Instant]] =
    (cid, uid)
      .bitraverse(c => courseSectionService.getCourseSection(c), u => u.traverse(_.component_?[UserComponent])) match
      case Some((section, userOpt)) =>
        onGateEventImpl(section, userOpt)
      case None                     =>
        log info s"Ignoring event for deleted course/user"
        None.success

  private def onGateEventImpl(section: CourseSection, user: Option[UserComponent]): Try[Option[Instant]] =
    for
      start    <- user.cata(userStartDate(section, _), courseStartDate(section)) // this will drop unenroled users
      modify    = storageModification(section.lwc, user.map(_.userId))
      gateDates = ContentDateUtils.contentDates(section.contents.tree, start.atZone(domain.timeZoneId)).gateDates
      now       = ts.instant
      _        <- modify { gateDating =>
                    val lastScheduled = gateDating.lastScheduled.getOrElse(Instant.MIN)
                    // notify all gates that opened between the last event and now
                    for
                      content  <- section.contents.tree.flatten
                      gateDate <- gateDates.get(content.edgePath)
                      if (gateDate `isAfter` lastScheduled) && !(gateDate `isAfter` now)
                    do /* side effect no map */
                      log info s"Gate notification in course ${section.id} for content ${content.edgePath} ${user
                          .map(u => s"to user ${u.id} ")
                          .orZero}at time $gateDate"
                      val init = GateDateNotificationInit(section.id, user.map(_.id), gateDate, content)
                      notificationService.nοtify[GateDateNotification](user.map(_.id) | section.id, init)
                    end for
                    // update course status with current time
                    gateDating.copy(lastScheduled = Some(now))
                  }
    yield
      // reschedule for next gating event
      ContentDateUtils.nextDate(gateDates, now)

  private def courseStartDate(section: CourseSection): Try[Instant] =
    section.startDate <@~* new IllegalStateException("Course gating event in self-study course")

  private def userStartDate(section: CourseSection, user: UserComponent): Try[Instant] =
    val enrollments: List[EnrollmentFacade] = enrollmentService.getUserEnrollments(user.id, section.id).asScala.toList
    section.startDateForUser(enrollments) <@~* new IllegalStateException("User gating event for non-enrolled student")

//    gateDateService.startDate(course, user.userId) <@~* new IllegalStateException(
//      "User gating event for non-enrolled student")

  /** Modify either the course storage facade (instructor-led) or the course user storage facade (self-study).
    *
    * @return
    *   a function from a gate-dating-modification endofunction to a fallible effectfous modification result
    */
  private def storageModification(
    course: Lwc,
    user: Option[UserId],
  ): ((GateDating => GateDating) => Try[GateDating]) =
    val mod = user match
      case Some(usr) =>
        (x: StoragedDating => StoragedDating) => Try(courseStorageService.modify[StoragedDating](course, usr)(x))
      case None      =>
        (x: StoragedDating => StoragedDating) => Try(courseStorageService.modify[StoragedDating](course)(x))
    mod.dimap(_.dimap(_.value, StoragedDating.apply), _.map(_.value))
  end storageModification
end GateDateEventListener

object GateDateEventListener:
  private val log = org.log4s.getLogger

  final case class StoragedDating(value: GateDating)

  implicit val codec: CodecJson[StoragedDating] =
    CodecJson.derived[GateDating].xmap(StoragedDating.apply)(_.value)

  implicit val storageable: CourseStoreable[StoragedDating] =
    CourseStoreable("gateDating")(StoragedDating(GateDating(None)))
end GateDateEventListener
