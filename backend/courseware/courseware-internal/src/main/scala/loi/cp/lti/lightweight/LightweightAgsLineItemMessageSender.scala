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
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.de.web.MediaType
import loi.cp.bus.*
import loi.cp.context.ContextId
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.gradebook.CreditType
import loi.cp.integration.LtiSystemComponent
import loi.cp.lti.LtiItemSyncStatus.{Attempted, Failed, LtiItemSyncStatusHistory, Synced}
import loi.cp.lti.ags.{AgsLineItem, CreateAgsLineItemRequest, LtiRequestService}
import loi.cp.lwgrade.GradeColumn
import loi.cp.reference.EdgePath
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*
import scalaz.{-\/, \/, \/-}
import scaloi.misc.TimeSource
import scaloi.syntax.disjunction.*

@Component
@MessageSenderBinding
class LightweightAgsLineItemMessageSender(
  val componentInstance: ComponentInstance,
  ltiRequestService: LtiRequestService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  courseSectionService: CourseSectionService,
  ltiGradeSyncService: LtiGradeSyncService,
  ts: TimeSource
) extends MessageSender[LtiSystemComponent[?], LineItemMessage]
    with ComponentImplementation:

  import LightweightAgsLineItemMessageSender.*

  private val logger = org.log4s.getLogger

  override def onDropped(message: LineItemMessage, failure: DeliveryFailure): Unit =
    failure match
      case TransientFailure(Right(info)) =>
        pushFailure(Failed(ts.instant, HttpError.from(info)))(message)
      case TransientFailure(Left(t))     =>
        pushFailure(Failed(ts.instant, InternalError(t.getMessage)))(message)
      case PermanentFailure(t)           =>
        pushFailure(Failed(ts.instant, InternalError(t.getMessage)))(message)

  private def pushFailure(failure: LtiItemSyncStatus[Nothing])(msg: LineItemMessage) =
    ltiColumnIntegrationService.modify(ContextId(msg.contextId))(_ map { history =>
      history.pushAgsStatus(msg.content, failure)
    })

  override def sendMessage(
    system: LtiSystemComponent[?],
    message: LineItemMessage,
    untx: YieldTx
  ): DeliveryResult =

    val result =
      for section <- getCourseSection(message.contextId)
      yield message match
        case msg: CreateLineItemMessage => sendCreateMessage(system, section, msg, untx)
        case msg: DeleteLineItemMessage => sendDeleteMessage(system, section, msg, untx)

    result.merge
  end sendMessage

  private def getCourseSection(contextId: Long): DeliveryFailure \/ CourseSection =
    courseSectionService
      .getCourseSection(contextId)
      .toRightDisjunction(PermanentFailure(s"No such course $contextId"))

  private def sendCreateMessage(
    system: LtiSystemComponent[?],
    lwc: CourseSection,
    message: CreateLineItemMessage,
    untx: YieldTx
  ): DeliveryResult =
    sendAgsLineItem(message, system, lwc, untx)

  private def sendDeleteMessage(
    system: LtiSystemComponent[?],
    section: CourseSection,
    message: DeleteLineItemMessage,
    untx: YieldTx
  ): DeliveryResult =
    fail("AGS Delete Line Item not implemented")

  type ExtractColumnStatus[T] = CourseColumnIntegrations => Map[EdgePath, LtiItemSyncStatusHistory[T]]

  def fail(s: String): PermanentFailure = PermanentFailure(s)

  def processRequest[A, B](
    message: CreateLineItemMessage,
    system: LtiSystemComponent[?],
    lwc: CourseSection,
    untx: YieldTx,
    extractColumnStatus: ExtractColumnStatus[A],
  )(process: () => DeliveryFailure \/ Unit): DeliveryResult =
    (for
      integrations      <-
        ltiColumnIntegrationService.get(lwc) \/> fail(s"No integration configuration for course: ${lwc.id}")
      columnIntegration <- extractColumnStatus(integrations).get(message.column.path) \/> fail(
                             s"No integration configuration for column: ${message.column.path} in course: ${lwc.id}"
                           )
    yield
      if !columnIntegration.isSynced then
        val result = process().map(_ => ltiGradeSyncService.syncAllGradesForColumn(lwc, message.column.path))
        deliveredOrPushFailure(result, message)
      else Delivered).merge[DeliveryResult]

  private def deliveredOrPushFailure[A](either: DeliveryFailure \/ A, message: LineItemMessage): DeliveryResult =
    either match
      case \/-(a)                                      => Delivered
      case -\/(failure @ TransientFailure(Left(e)))    =>
        pushFailure(Attempted(ts.instant, InternalError(e.getMessage)))(message)
        failure
      case -\/(failure @ TransientFailure(Right(why))) =>
        pushFailure(Attempted(ts.instant, HttpError.from(why)))(message)
        failure
      case -\/(failure @ PermanentFailure(e))          =>
        pushFailure(Attempted(ts.instant, InternalError(e.getMessage)))(message)
        failure

  private def sendAgsLineItem(
    message: CreateLineItemMessage,
    system: LtiSystemComponent[?],
    lwc: CourseSection,
    untx: YieldTx
  ) =
    processRequest(message, system, lwc, untx, _.lineItems) { () =>
      val agsLineItem = CreateAgsLineItemRequest(
        message.column.pointsPossible,
        message.column.title,
        Some(message.column.creditType),
        message.column.path.toString
      )
      logger.info(s"Sending AGS line Item: $agsLineItem")
      ltiRequestService
        .sendLtiServicePost[CreateAgsLineItemRequest, AgsLineItem](
          message.lineItemsUrl,
          system,
          MediaType.APPLICATION_IMS_LINE_ITEM_VALUE,
          agsLineItem,
          untx
        )
        .rightTap(newLineItem =>
          ltiColumnIntegrationService.modify(lwc)(integrations =>
            integrations.map(_.pushAgsStatus(message.column.path, Synced(ts.instant, ts.instant, newLineItem)))
          )
        )
        .void
    }
end LightweightAgsLineItemMessageSender

object LightweightAgsLineItemMessageSender:

  implicit class GradeColumnOps(gradeColumn: GradeColumn):
    def creditType: String =
      if gradeColumn.isForCredit then CreditType.Credit.toString
      else CreditType.NoCredit.toString
