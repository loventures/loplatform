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

package loi.cp.mastery

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.asset.competency.model.CompetencySet
import loi.authoring.asset.Asset
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.event.{CompetencyMasteryEvent, CompetencySetMasteryEvent}
import loi.cp.competency.Competency
import loi.cp.course.CourseSection
import loi.cp.mastery.notification.CompetencyMasteryNotification
import loi.cp.mastery.store.MasteryDao
import loi.cp.notification.NotificationService
import loi.cp.quiz.attempt.QuizAttempt
import loi.cp.submissionassessment.attempt.SubmissionAttempt
import scalaz.syntax.std.option.*

import java.time.Instant
import java.util.{Date, UUID}

/** This processes quiz/attempt submissions and updates user mastery. Updating a score in the gradebook will not update
  * your mastery, rescoring an attempt will.
  */
@Service
class MasteryService(
  masteryDao: MasteryDao,
  nodeService: AssetNodeService,
  analyticsService: AnalyticsService,
  notificationService: NotificationService,
  domain: => DomainDTO,
)(implicit edgeService: EdgeService):
  import MasteryService.*

  def computeMasteryForQuizAttempt(ws: AttachedReadWorkspace, attempt: QuizAttempt): Set[UUID] =
    MasteryCalculator.updateMasteryForQuizAttempt(ws, UserMasteryState.Empty, attempt).competencyMastery

  def getMasteredCompetencies(user: UserId, section: CourseSection): Set[UUID] =
    masteryDao.getMasteryFacade(user.id, section.id).cata(_.getState.competencyMastery, Set.empty)

  def deleteMastery(user: UserId, section: CourseSection): Unit =
    masteryDao.getMasteryFacade(user.id, section.id) foreach { mastery =>
      mastery.delete()
    }

  def transferMastery(user: UserId, from: CourseSection, to: CourseSection): Unit =
    masteryDao.getMasteryFacade(user.id, from.id) foreach { src =>
      val dst = masteryDao.getMasteryFacadeForUpdate(user.id, to.id)
      dst.setState(src.getState)
      src.delete()
    }

  def recomputeUserMasteryForAttempts(
    ws: AttachedReadWorkspace,
    user: UserDTO,
    section: CourseSection,
    legacyMastery: Set[UUID],
    attempts: Seq[Either[QuizAttempt, SubmissionAttempt]],
  ): UserMasteryState =
    applyMasteryUpdate(
      ws,
      Instant.now,
      user,
      section,
      state =>
        attempts
          .sortBy(_.fold(_.submitTime, _.submitTime))
          .foldLeft(state) { case (state, attempt) =>
            attempt.fold(
              MasteryCalculator.updateMasteryForQuizAttempt(ws, state, _),
              MasteryCalculator.updateMasteryForSubmissionAttempt(ws, state, _)
            )
          },
      Some(UserMasteryState.Empty.copy(competencyMastery = legacyMastery, recomputed = true))
    )

  def updateUserMasteryForQuizAttempt(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: QuizAttempt
  ): Unit =
    logger.info(s"Processing mastery from quiz attempt ${attempt.id}")
    applyMasteryUpdate(
      ws,
      attempt.submitTime | attempt.updateTime,
      attempt.user,
      section,
      MasteryCalculator.updateMasteryForQuizAttempt(ws, _, attempt)
    )
  end updateUserMasteryForQuizAttempt

  // the submission attempt assignment has all the competencies in it but shrug.
  def updateUserMasteryForSubmissionAttempt(
    ws: AttachedReadWorkspace,
    section: CourseSection,
    attempt: SubmissionAttempt
  ): Unit =
    logger.info(s"Processing mastery from submission attempt ${attempt.id}")
    applyMasteryUpdate(
      ws,
      attempt.submitTime | attempt.updateTime,
      attempt.user,
      section,
      MasteryCalculator.updateMasteryForSubmissionAttempt(ws, _, attempt)
    )
  end updateUserMasteryForSubmissionAttempt

  private def applyMasteryUpdate(
    ws: AttachedReadWorkspace,
    when: Instant,
    user: UserDTO,
    section: CourseSection,
    update: UserMasteryState => UserMasteryState,
    resetState: Option[UserMasteryState] = None
  ): UserMasteryState =
    val competencyFacade = masteryDao.getMasteryFacadeForUpdate(user.id, section.id)
    val oldState         = resetState | competencyFacade.getState
    val oldMastery       = oldState.competencyMastery
    val state            = update(oldState)
    val newMastery       = state.competencyMastery
    val mastered         = nodeService.load(ws).byName(newMastery -- oldMastery).get

    logger.info(s"Mastered ${mastered.size} new competencies")

    competencyFacade.setState(state)
    if mastered.nonEmpty then
      deliverNotifications(when, user, section, mastered)
      emitAnalytics(when, user, mastered)
    state
  end applyMasteryUpdate

  private def deliverNotifications(
    when: Instant,
    user: UserDTO,
    course: CourseSection,
    mastered: Seq[Asset[?]]
  ): Unit =
    for
      asset      <- mastered
      competency <- Competency.fromAsset(asset) // excludes competency set
      init        = CompetencyMasteryNotification.Init(when, user, course, competency)
    do notificationService.nοtify[CompetencyMasteryNotification](user, init)

  private def emitAnalytics(when: Instant, user: UserDTO, mastered: Seq[Asset[?]]): Unit =
    mastered foreach {
      case CompetencySet.Asset(competencySet) =>
        analyticsService `emitEvent` CompetencySetMasteryEvent(
          id = UUID.randomUUID(),
          time = Date.from(when),
          source = domain.hostName,
          subject = user,
          competencySetId = competencySet.info.id,
          competencySetGuid = competencySet.info.name,
          competencySetTitle = competencySet.data.title
        )

      case Competency(competency) =>
        analyticsService `emitEvent` CompetencyMasteryEvent(
          id = UUID.randomUUID(),
          time = Date.from(when),
          source = domain.hostName,
          subject = user,
          ruleId = 0L, // !
          ruleName = None,
          ruleFilter = None,
          ruleCalculator = None,
          competencyId = competency.id,
          competencyGuid = competency.nodeName,
          competencyLevel = competency.level,
          competencyTitle = competency.title
        )
    }
end MasteryService

object MasteryService:
  private final val logger = org.log4s.getLogger
