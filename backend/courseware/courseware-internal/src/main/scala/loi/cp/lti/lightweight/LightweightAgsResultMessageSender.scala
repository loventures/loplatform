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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.user.UserId as Uzr
import com.learningobjects.de.web.MediaType
import loi.cp.bus.*
import loi.cp.context.ContextId
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.integration.LtiSystemComponent
import loi.cp.lti.LtiItemSyncStatus.{Attempted, Failed, LtiItemSyncStatusHistory, LtiItemSyncStatusHistoryOps, Synced}
import loi.cp.lti.ags.{AgsActivityProgress, AgsGradingProgress, AgsScore, LtiRequestService}
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import scalaz.syntax.std.`try`.*
import scalaz.syntax.std.option.*
import scalaz.{-\/, \/, \/-}
import scaloi.misc.TimeSource

import scala.util.Try

@Component
@MessageSenderBinding
class LightweightAgsResultMessageSender(
  val componentInstance: ComponentInstance,
  ltiRequestService: LtiRequestService,
  courseStorageService: CourseStorageService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  ts: TimeSource
)(implicit cs: ComponentService)
    extends MessageSender[LtiSystemComponent[?], SendResultMessage]
    with ComponentImplementation:

  private val logger = org.log4s.getLogger

  override def onDropped(message: SendResultMessage, failure: DeliveryFailure): Unit =
    failure match
      case TransientFailure(Right(info)) =>
        pushFailure(Failed(ts.instant, HttpError.from(info)))(message)
      case TransientFailure(Left(t))     =>
        pushFailure(Failed(ts.instant, InternalError(t.getMessage)))(message)
      case PermanentFailure(t)           =>
        pushFailure(Failed(ts.instant, InternalError(t.getMessage)))(message)

  def pushFailure(value: LtiItemSyncStatus[Nothing])(msg: SendResultMessage) =
    courseStorageService.modify[UserGradeSyncHistory](ContextId(msg.contextId), Uzr(msg.userId))(history =>
      logger.warn(s"Failed to send LTI grade: $msg")
      history.pushAgsScoreStatus(msg.content)(value)
    )

  override def sendMessage(system: LtiSystemComponent[?], message: SendResultMessage, untx: YieldTx): DeliveryResult =
    (for lwc <- message.contextId.component_![LightweightCourse].toDisjunction
    yield sendAgsResultRequest(lwc, system, message, untx)).leftMap(PermanentFailure.apply).merge

  type ExtractColumnStatus[T] = CourseColumnIntegrations => Map[EdgePath, LtiItemSyncStatusHistory[T]]
  type ExtractGradeStatus[T]  = UserGradeSyncHistory => Map[EdgePath, LtiItemSyncStatusHistory[T]]

  def processRequest[A, B](
    lwc: LightweightCourse,
    system: LtiSystemComponent[?],
    message: SendResultMessage,
    untx: YieldTx,
    extractColumnStatus: ExtractColumnStatus[A],
    extractGradeStatus: ExtractGradeStatus[B]
  )(process: Synced[A] => DeliveryFailure \/ Unit): DeliveryResult =
    (for
      externalConfig   <- getCourseLtiConfiguration(lwc)
      columnStatus     <- extractColumnStatus(externalConfig).get(message.content) \/> PermanentFailure(
                            s"Config for EdgePath ${message.content} not found"
                          )
      columnConfig     <-
        columnStatus.lastValid \/> PermanentFailure(s"Column for EdgePath ${message.content} is not synced")
      gradesStatus     <- Try(courseStorageService.get[UserGradeSyncHistory](lwc, Uzr(message.userId))).toDisjunction
                            .leftMap(PermanentFailure.apply)
      gradeSyncHistory <-
        extractGradeStatus(gradesStatus)
          .get(message.content)
          .toRightDisjunction(
            PermanentFailure(
              s"Grade History for user: ${message.userId} and content: ${message.content} does not exist"
            )
          )
    yield
      if !gradeSyncHistory.isUpToDate(message.gradeDate) then
        process(columnConfig) match
          case \/-(_)                                    =>
            logger.info(
              s"Successfully sent LTI grade result, last status ${gradeSyncHistory.last}, updated with message: $message"
            )
            Delivered
          case -\/(t @ TransientFailure(Left(e)))        =>
            pushFailure(Attempted(ts.instant, e))(message)
            t
          case -\/(t @ TransientFailure(Right(failure))) =>
            pushFailure(Attempted(ts.instant, HttpError.from(failure)))(message)
            t
          case -\/(p @ PermanentFailure(e))              =>
            pushFailure(Attempted(ts.instant, e))(message)
            p
      else
        logger.info(
          s"Skipped sending LTI grade result, last entry ${gradeSyncHistory.last}, not updated with message: $message"
        )
        Delivered
    ).merge

  private def sendAgsResultRequest(
    lwc: LightweightCourse,
    system: LtiSystemComponent[?],
    message: SendResultMessage,
    untx: YieldTx
  ): DeliveryResult =
    processRequest(lwc, system, message, untx, _.lineItems, _.agsScores) { columnConfig =>
      val result = message.pointsAwarded
        .map(points =>
          // The common case - scoreGiven exists, so we send that value and set progress as Completed/Graded
          AgsScore(
            message.userLmsId,
            scoreGiven = Some(BigDecimal(points)),
            message.totalPossible,
            comment = "",
            message.gradeDate,
            AgsActivityProgress.Completed,
            AgsGradingProgress.FullyGraded
          )
        )
        .getOrElse(
          // The deletion case - if all scored items are removed, send no value and set progress to Initialized/NotReady
          // see: https://www.imsglobal.org/spec/lti-ags/v2p0/#migrating-from-basic-outcomes-service
          AgsScore(
            message.userLmsId,
            scoreGiven = None,
            message.totalPossible,
            comment = "",
            message.gradeDate,
            AgsActivityProgress.Initialized,
            AgsGradingProgress.NotReady
          )
        )
      ltiRequestService
        .sendLtiServicePostWithoutResp[AgsScore](
          scoreUrl(columnConfig.syncedValue.id),
          system,
          MediaType.APPLICATION_IMS_SCORE_V1_VALUE,
          result,
          untx
        )
        .map({ _ =>
          courseStorageService.modify[UserGradeSyncHistory](lwc, Uzr(message.userId)) { history =>
            history.pushAgsScoreStatus(message.content)(Synced(message.gradeDate, ts.instant, result))
          }
        })
    }

  private def scoreUrl(id: String): String =
    if id.endsWith("/") then s"${id}scores"
    else s"$id/scores"

  def getCourseLtiConfiguration(lwc: LightweightCourse): PermanentFailure \/ CourseColumnIntegrations =
    Try(ltiColumnIntegrationService.get(lwc)).toDisjunction
      .leftMap(e => PermanentFailure(e))
      .flatMap(_ \/> PermanentFailure(s"Course with id: ${lwc.getId} does not have a CourseColumnConfiguration"))
end LightweightAgsResultMessageSender
