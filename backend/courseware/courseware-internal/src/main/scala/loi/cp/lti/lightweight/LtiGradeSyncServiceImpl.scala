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

package loi.cp.lti.lightweight

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.EntityContext
import loi.cp.bus.MessageBusService
import loi.cp.config.ConfigurationService
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.lti.LtiItemSyncStatus.{LtiItemSyncStatusHistory, Queued}
import loi.cp.lti.lightweight.BasicOutcomesMessageAction.{Delete, Replace}
import loi.cp.lti.lightweight.LtiGradeSyncService.*
import loi.cp.lti.storage.UserGradeSyncHistory
import loi.cp.lti.{CourseColumnIntegrations, LtiColumnIntegrationService, LtiConfiguration, LtiOutcomesService}
import loi.cp.lwgrade.*
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.user.UserComponent
import org.log4s.Logger
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.ʈry.*

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.compat.java8.OptionConverters.*
import scala.util.Try

@Service
class LtiGradeSyncServiceImpl(
  gradeDao: GradeDao,
  courseStorageService: CourseStorageService,
  messageBusService: MessageBusService,
  ltiColumnIntegrationService: LtiColumnIntegrationService,
  configurationService: ConfigurationService,
  ltiOutcomesService: LtiOutcomesService,
  ts: TimeSource
)(implicit val cs: ComponentService)
    extends LtiGradeSyncService:

  import LtiGradeSyncServiceImpl.*

  val log: Logger = org.log4s.getLogger

  override def syncColumn(
    course: CourseSection,
    edgePath: EdgePath
  ): SyncError \/ (GradeColumn, CourseColumnIntegrations) =
    for
      (column, histories) <- getColumnHistory(course, edgePath)
      _                   <- validateHistories(histories, edgePath)
      newHistory          <- syncAndGet(course, column) \/> NoSyncHistory(course.id)
    yield (column, newHistory)

  override def getColumnHistory(
    course: CourseSection,
    edgePath: EdgePath
  ): SyncError \/ (GradeColumn, CourseColumnIntegrations) =
    for
      column      <- GradeStructure(course.contents).findColumnForEdgePath(edgePath) \/> NoColumn(edgePath).widen
      historiesOp <- Try(ltiColumnIntegrationService.get(course)) \/> errorHistories(course.id)
      histories   <- historiesOp \/> NoSyncHistory(course.id)
    yield (column, histories)

  private def syncAndGet(section: CourseSection, column: GradeColumn): Option[CourseColumnIntegrations] =
    ltiOutcomesService.manuallySyncColumn(section, column)
    ltiColumnIntegrationService.get(section)

  override def syncAllGradesForColumn(course: CourseSection, edgePath: EdgePath): Map[UserId, SingleGradeHistory] =
    gradeDao.loadByCourse(course) flatMap { case (userId, rawGrades) =>
      rawGrades.get(edgePath) map { grade =>
        userId -> syncGrade(course, userId, edgePath, grade)
      }
    }

  override def syncOutcomes1Grade(userId: UserId, course: ContextId, ep: EdgePath, grade: Option[Grade]): Unit =
    for
      callbacks <- Try(courseStorageService.get[UserGradeSyncHistory](course, userId))
      config    <- callbacks.outcomes1Callbacks.get(ep)
      system    <- config.system.component_?[BasicLtiSystemComponent]
    do
      messageBusService.publishMessage(
        system,
        LightweightBasicOutcomesMessage(
          if grade.isEmpty then Delete else Replace,
          edgePath = ep,
          userId = userId.id,
          contextId = course.getId,
        )
      )
      log.info(s"Pushed User Outcomes1 Status for content $ep and user $userId: ${Queued(ts.instant)}")
      courseStorageService.modify[UserGradeSyncHistory](course, userId)(_.pushOutcomes1Status(ep)(Queued(ts.instant)))

  /** Assignment and Grading Services (AGS) - used with LTI 1.3, and is LTI's *latest* standard for sending grades
    *
    * After extracting details, this will queue a SendResultMessage on the bus, which is later processed by a worker and
    * sent as an AgsScore to the LTI Consumer
    */
  override def syncAgsGrade(
    userId: UserId,
    course: ContextId,
    ep: EdgePath,
    maybeGrade: Option[Grade],
  ): Unit =
    for
      externalConfig <- Try(ltiColumnIntegrationService.get(course)).toList.flatten
      if externalConfig.configContains(ep)
      system         <- externalConfig.systemId.component_?[BasicLtiSystemComponent].toList
      userLmsId      <- getUserIdForSystem(system, userId)
      config          = configurationService.getDomain(LtiConfiguration)
      grade          <- maybeGrade
    do
      val (points, max, date) = Grade.getGradeValues(grade)

      // for most grades (Graded, Pending, etc), a date exists - for Ungraded entries, there are none, use now
      val gradeDate = date.getOrElse(Instant.now.truncatedTo(ChronoUnit.MILLIS))

      if points.isDefined || config.syncEmptyLTIGrades then
        messageBusService.publishMessage(
          system,
          SendResultMessage(
            content = ep,
            contextId = course.getId,
            userId = userId.id,
            userLmsId = userLmsId,
            pointsAwarded = points,
            totalPossible = max,
            gradeDate = gradeDate,
          )
        )
      end if
      courseStorageService.modify[UserGradeSyncHistory](course, userId): history =>
        log.info(s"Pushed User AGS Status for content $ep and user $userId: ${Queued(gradeDate)}")
        history.pushAgsScoreStatus(ep)(Queued(gradeDate))

  override def syncAllColumnsAndGradesForCourse(
    course: CourseSection
  ): SyncError \/ Unit =
    for histories <- ltiColumnIntegrationService.get(course) \/> NoSyncHistory(course.id)
    yield syncColumns(course, histories.lineItems)

  def syncColumns[A](course: CourseSection, histories: Map[EdgePath, LtiItemSyncStatusHistory[A]]): Unit =
    histories foreach {
      case (ep, status) if status.isSynced =>
        syncAllGradesForColumn(course, ep)
        EntityContext.flush()
      case (ep, _)                         =>
        syncColumn(course, ep)
    }

  override def syncGrade(
    course: ContextId,
    userId: UserId,
    edgePath: EdgePath,
    grade: Grade,
  ): SingleGradeHistory =
    syncOutcomes1Grade(userId, course, edgePath, Some(grade))
    syncAgsGrade(userId, course, edgePath, Some(grade))
    val allHistory = courseStorageService.get[UserGradeSyncHistory](course)
    SingleGradeHistory.fromUserGradeHistory(allHistory, edgePath)
  end syncGrade

  private def getUserIdForSystem(ltiSystem: BasicLtiSystemComponent, userId: UserId): Option[String] =
    userId.component_?[UserComponent] flatMap { user =>
      if ltiSystem.getUseExternalIdentifier then user.getExternalId.asScala
      else
        user.getIntegrationRoot
          .getIntegrationBySystemId(ltiSystem.getId)
          .asScala
          .map(_.getUniqueId)
    }
end LtiGradeSyncServiceImpl

object LtiGradeSyncServiceImpl:

  def errorHistories(courseId: Long): Throwable => SyncError =
    t => Exceptional(s"Error getting course ($courseId) sync histories : ${t.getMessage}")

  def validateHistories(histories: CourseColumnIntegrations, edgePath: EdgePath): SyncError \/ Unit =
    histories.lineItems.get(edgePath) match
      case Some(status) if !status.failed => ColumnAlreadySynced(edgePath).left
      case _                              => ().right
