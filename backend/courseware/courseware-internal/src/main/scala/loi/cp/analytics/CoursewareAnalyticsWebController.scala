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

package loi.cp.analytics

import argonaut.JsonIdentity.*
import cats.syntax.either.*
import com.learningobjects.cpxp.component.annotation.{Component, Controller, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.SessionDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import com.learningobjects.de.authorization.Secured
import de.tomcat.juli.LogMeta
import loi.cp.analytics.entity.ExternallyIdentifiableEntity.*
import loi.cp.analytics.event.*
import loi.cp.content.CourseContent
import loi.cp.course.CourseSectionService
import loi.cp.reference.EdgePath
import scaloi.misc.TimeSource

import java.util.UUID

@Controller(value = "courseware-analytics", root = true)
trait CoursewareAnalyticsWebController extends ApiRootComponent:

  /** Web endpoint for emitting an analytics event. *NOTE* this is not /analytics/emit because that gets blacklisted by
    * ad blockers. "analytics" and "event" are probably somewhat unsafe for front-ends to use for this reason.
    */
  @Secured(allowAnonymous = true)
  @RequestMapping(path = "an/emit", method = Method.POST)
  def emit(@RequestBody frontEndEvents: Seq[FrontEndEvent]): Unit

@Component
class CoursewareAnalyticsWebControllerImpl(
  analyticsService: AnalyticsService,
  courseSectionService: CourseSectionService,
  ci: ComponentInstance,
  domainDto: => DomainDTO,
  sessionDto: => SessionDTO,
  timeSource: TimeSource,
  userDto: => UserDTO
) extends BaseComponent(ci)
    with CoursewareAnalyticsWebController:

  import CoursewareAnalyticsWebControllerImpl.log

  override def emit(frontEndEvents: Seq[FrontEndEvent]): Unit =
    val (errors, events) = frontEndEvents
      .filter(_ => userDto != null && userDto.userType != UserType.Anonymous)
      .toList
      .zipWithIndex
      .partitionMap({ case (event, i) =>
        validate(event).leftMap(err => s"[$i] $err")
      })

    if errors.nonEmpty then LogMeta.let("discardedEvents" -> errors.asJson)(log.warn("discarding front end events"))

    events.foreach(analyticsService.emitEvent)
  end emit

  private def validate(event: FrontEndEvent): Either[String, Event] = event match
    case event: PageNavFEEvent        => pageNavEvent(event)
    case event: CourseEntryFEEvent    => courseEntryEvent(event)
    case event: QuestionViewedFEEvent => questionViewedEvent(event)
    case event: TutorialViewFEEvent   => tutorialViewEvent(event)
    case event                        => s"unknown event ${event.eventType}".asLeft

  private def pageNavEvent(event: PageNavFEEvent): Either[String, PageNavEvent] =
    validatePageNavEvent(event).map(content =>
      PageNavEvent(
        id = UUID.randomUUID(),
        time = timeSource.date,
        source = domainDto.hostName,
        session = sessionDto.id,
        clientTime = event.clientTime,
        user = userDto,
        url = event.url,
        title = event.pageTitle,
        course = event.course,
        content = event.content,
        contentType = content.map(_.asset.info.typeId),
        contentTitle = content.map(_.title),
        impersonatedUserId = event.impersonatedUserId,
        er = Some(event.er),
      )
    )

  private def validatePageNavEvent(event: PageNavFEEvent): Either[String, Option[CourseContent]] = event.course match
    case None           => None.asRight // valid visit to a non-section page (such as? no idea)
    case Some(courseId) =>
      courseSectionService.getCourseSection(courseId.section.id) match
        case None          => s"no such section ${courseId.section.id}".asLeft
        case Some(section) =>
          event.content match
            case None            => None.asRight // valid visit to a non-content in a section
            case Some(contentId) =>
              section.contents.get(EdgePath.parse(contentId.contentId)) match
                case None          => s"no such content ${contentId.contentId}".asLeft
                case Some(content) => Some(content).asRight // valid visit to content

  private def courseEntryEvent(event: CourseEntryFEEvent): Either[String, CourseEntryEvent] =
    CourseEntryEvent(
      id = UUID.randomUUID(),
      time = timeSource.date,
      source = domainDto.hostName,
      session = sessionDto.id,
      clientTime = event.clientTime,
      user = analyticsService.userData(userDto),
      course = event.course,
      impersonatedId = event.impersonatedId,
      userAgent = event.userAgent,
      framed = event.framed
    ).asRight

  private def questionViewedEvent(event: QuestionViewedFEEvent): Either[String, QuestionViewedEvent] =
    QuestionViewedEvent(
      id = UUID.randomUUID(),
      time = timeSource.date,
      source = domainDto.hostName,
      session = sessionDto.id,
      clientTime = event.clientTime,
      user = userDto,
      question = event.question,
      assessment = event.assessment,
      course = event.course
    ).asRight

  private def tutorialViewEvent(event: TutorialViewFEEvent): Either[String, TutorialViewEvent1] =
    TutorialViewEvent1(
      id = UUID.randomUUID(),
      time = timeSource.date,
      source = domainDto.hostName,
      session = sessionDto.id,
      userId = userDto.id,
      tutorialName = event.tutorialName.take(255),
      autoPlay = event.autoplay,
      step = event.step,
      complete = event.complete
    ).asRight
end CoursewareAnalyticsWebControllerImpl

object CoursewareAnalyticsWebControllerImpl:
  private val log: org.log4s.Logger = org.log4s.getLogger
