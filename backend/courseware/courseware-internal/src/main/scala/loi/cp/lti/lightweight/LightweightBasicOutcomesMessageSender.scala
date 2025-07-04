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

package loi.cp.lti
package lightweight

import java.time.Instant

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.bus.*
import loi.cp.context.ContextId
import loi.cp.course.CourseSectionService
import loi.cp.lti.LtiResultMessageSender
import loi.cp.integration.LtiSystemComponent
import loi.cp.lti.LtiItemSyncStatus.{Attempted, Failed, LtiItemSyncStatusHistoryOps}
import loi.cp.lti.lightweight.BasicOutcomesMessageAction.Replace
import loi.cp.lti.lightweight.LightweightOutcomesService.*
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.lwgrade.{Grade, GradeService}
import loi.cp.storage.CourseStorageService
import scaloi.misc.TimeSource
import scaloi.syntax.any.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*

import scala.util.Try

@Component
@MessageSenderBinding
class LightweightBasicOutcomesMessageSender(
  val componentInstance: ComponentInstance,
  courseSectionService: CourseSectionService,
  ltiResultMessageSender: LtiResultMessageSender,
  courseStorageService: CourseStorageService,
  gradeService: GradeService,
  ts: TimeSource
) extends MessageSender[LtiSystemComponent[?], LightweightBasicOutcomesMessage]
    with ComponentImplementation:

  override def onDropped(message: LightweightBasicOutcomesMessage, failure: DeliveryFailure): Unit =
    failure match
      case TransientFailure(Right(info)) =>
        pushStatus(Failed(ts.instant, HttpError.from(info)))(message)
      case TransientFailure(Left(t))     =>
        pushStatus(Failed(ts.instant, InternalError(t.getMessage)))(message)
      case PermanentFailure(t)           =>
        pushStatus(Failed(ts.instant, InternalError(t.getMessage)))(message)

  override def sendMessage(
    system: LtiSystemComponent[?],
    message: LightweightBasicOutcomesMessage,
    untx: YieldTx
  ): DeliveryResult =
    message.action match
      case Replace =>
        (for
          section   <- courseSectionService
                         .getCourseSection(message.contextId, None)
                         .toTry(new IllegalArgumentException(s"No such context ${message.contextId}"))
          gradebook  = gradeService.getGrades(UserId(message.userId), ContextId(message.contextId), section.contents)
          grade     <- getGradeForStudent(message.userId, message.contextId, message.edgePath, gradebook).toTry
          ltiScore  <- getSingleLtiScore(message.userId, message.contextId, message.edgePath, grade).toTry
          callbacks <- Try(courseStorageService.get[UserGradeSyncHistory](section.lwc, UserId(message.userId)))
          cb        <- callbacks.outcomes1Callbacks.get(message.edgePath) `toTry` new RuntimeException(
                         s"Lti Callback for edgepath: ${message.edgePath} not found"
                       )
        yield
          val isUpToDate = Grade.date(grade).fold(false) { gradeDate =>
            cb.statusHistory.fold(false)(_.isUpToDate(gradeDate))
          }
          if isUpToDate then Delivered
          else
            ltiResultMessageSender.sendLtiScore(
              system,
              cb.url.toString,
              cb.resultSourceDid,
              ltiScore,
              untx
            ) <| {
              case Delivered                     =>
                updateSynced(grade, ltiScore)(message)
              case TransientFailure(Left(t))     =>
                pushStatus(Attempted(ts.instant, InternalError(t.getMessage)))(message)
              case TransientFailure(Right(info)) =>
                pushStatus(Attempted(ts.instant, HttpError.from(info)))(message)
              case _                             =>
            }
          end if
        ).fold(PermanentFailure.apply, identity)
      case _       => Delivered

  private def pushStatus(newStatus: LtiItemSyncStatus[Double])(message: LightweightBasicOutcomesMessage): Unit =
    val section =
      courseSectionService
        .getCourseSection(message.contextId, None)
        .getOrElse(throw new IllegalStateException(s"No such context ${message.contextId}"))
    courseStorageService.modify[UserGradeSyncHistory](section.lwc, UserId(message.userId))(
      _.pushOutcomes1Status(message.edgePath)(newStatus)
    )

  import LtiItemSyncStatus.*

  private def updateSynced(grade: Grade, ltiScore: Double) =
    pushStatus(Synced(Grade.date(grade).getOrElse(Instant.EPOCH), Instant.now, ltiScore))
end LightweightBasicOutcomesMessageSender
