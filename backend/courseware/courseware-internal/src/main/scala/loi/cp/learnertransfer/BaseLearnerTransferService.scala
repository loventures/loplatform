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

package loi.cp.learnertransfer

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.content.CourseContentService
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.customisation.Customisation
import loi.cp.enrollment.EnrollmentService
import loi.cp.lwgrade.GradeService
import loi.cp.mastery.MasteryService
import loi.cp.notification.SubscriptionService
import loi.cp.progress.LightweightProgressService
import loi.cp.quiz.attempt.QuizAttemptService
import loi.cp.submissionassessment.attempt.SubmissionAttemptService
import loi.cp.user.UserComponent
import scalaz.{NonEmptyList, Show, \/}
import scaloi.syntax.disjunction.*

/** The default implementation of [[LearnerTransferService]].
  *
  * To conserve git history elsewhere,we're breaking from the `__Impl` naming convention in this package and using
  * `Base__`. Someone can rename this back in another PR if they want, and git doesn't get confused.
  */
@Service
class BaseLearnerTransferService(
  learnerTransferValidator: LearnerTransferValidator,
  enrollmentService: EnrollmentService,
  gradeService: GradeService,
  progressService: LightweightProgressService,
  submissionAttemptService: SubmissionAttemptService,
  quizAttemptService: QuizAttemptService,
  subscriptionService: SubscriptionService,
  courseContentService: CourseContentService,
  masteryService: MasteryService,
)(implicit cs: ComponentService)
    extends LearnerTransferService:
  import BaseLearnerTransferService.*

  /** Transfers a student's enrollment from sourceSectionId to destinationSectionId, along with their relevant work.
    * Matches on course weight to call the appropriate transfer service after validation.
    *
    * @param userId
    *   the id of the learner.
    * @param sourceSectionId
    *   the pk of the section * where the student is currently enrolled in.
    * @param destinationSectionId
    *   the pk of the section where the student has transferred to.
    */
  override def transferLearner(
    userId: Long,
    sourceSectionId: Long,
    destinationSectionId: Long
  ): NonEmptyList[String] \/ CompletedLearnerTransfer =
    learnerTransferValidator
      .validateTransfer(userId, sourceSectionId, destinationSectionId)
      .leftMap(_.map(_.msg))
      .toDisjunction
      .map(transferLearner)
      .leftTap(msgs =>
        logger.warn(
          s"Learner transfer error $userId: $sourceSectionId -> $destinationSectionId: ${msgs.list.toList.mkString("; ")}"
        )
      )

  implicit def showUser: Show[UserComponent]   = Show.shows(_.getUserName)
  implicit def showCourse: Show[CourseSection] = Show.shows(_.groupId)
  private def str[A <: Id: Show](a: A): String = s"${Show[A].shows(a)} (${a.getId})"

  private def transferLearner(
    learnerTransfer: ValidatedLearnerTransfer
  ): CompletedLearnerTransfer =
    logger.info(
      s"Transferring learner ${str(learnerTransfer.student)} from ${str(learnerTransfer.source)} to ${str(learnerTransfer.destination)}"
    )

    val destinationEnrollment = enrollmentService.transferEnrollment(
      learnerTransfer.sourceEnrollment,
      learnerTransfer.destination.id
    )

    subscriptionService.unsubscribe(
      learnerTransfer.student.userId,
      ContextId(learnerTransfer.source.id)
    )

    gradeService.transferGrades(
      learnerTransfer.student.userId,
      ContextId(learnerTransfer.source.id),
      ContextId(learnerTransfer.destination.id)
    )

    val dstGradebook = gradeService.getGradebook(
      learnerTransfer.destination,
      learnerTransfer.student.userId
    )

    progressService.transferProgress(
      learnerTransfer.source,
      learnerTransfer.destination,
      learnerTransfer.student.userId,
      dstGradebook
    )

    masteryService.transferMastery(
      learnerTransfer.student.toDTO,
      learnerTransfer.source,
      learnerTransfer.destination
    )

    val contentPaths = courseContentService
      .getCourseContentsInternal(
        learnerTransfer.source.lwc,
        Customisation.empty
      )
      .get
      .nonRootElements
      .map(_.edgePath)

    submissionAttemptService.transferAttempts(
      learnerTransfer.source,
      contentPaths,
      learnerTransfer.student.userId,
      learnerTransfer.destination
    )

    quizAttemptService.transferAttempts(
      learnerTransfer.source,
      contentPaths,
      learnerTransfer.student.userId,
      learnerTransfer.destination
    )

    CompletedLearnerTransfer(
      learnerTransfer.student,
      learnerTransfer.source,
      learnerTransfer.destination,
      learnerTransfer.sourceEnrollment,
      destinationEnrollment,
    )
  end transferLearner
end BaseLearnerTransferService

object BaseLearnerTransferService:
  final val logger = org.log4s.getLogger
