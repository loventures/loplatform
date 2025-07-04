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

package loi.cp.quiz.persistence

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.quiz.{QuizAttemptEntity, QuizAttemptFolderEntity}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.{PersistenceIdFactory, ThreadTerminator}
import jakarta.persistence.LockModeType
import loi.cp.assessment.attempt.UserAttemptCounts
import loi.cp.assessment.persistence.{AttemptStateCountRow, Baroquitechture}
import loi.cp.assessment.{AssessmentGradingPolicy, AssessmentParticipation, AttemptId}
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.{QuizAttemptQuestionUsage, QuizQuestionResponse}
import loi.cp.reference.*
import org.hibernate.Session
import org.hibernate.query.Query
import scalaz.std.anyVal.*
import scaloi.syntax.collection.*

import java.util.Date
import scala.jdk.CollectionConverters.*

/** A service for updating and retrieving persisted quiz attempts.
  */
@Service
trait QuizAttemptDao:
  def newAttempt(
    quiz: Quiz,
    user: UserDTO,
    createTime: Date,
    maxMinutes: Option[Long],
    questionsContainer: Seq[QuizAttemptQuestionUsage],
    responses: Seq[QuizQuestionResponse],
    attemptFolder: QuizAttemptFolderEntity,
    domain: DomainDTO
  ): QuizAttemptEntity

  def write(attempt: QuizAttemptEntity): QuizAttemptEntity

  def load(id: AttemptId, lock: Boolean): Option[QuizAttemptEntity]

  def load(ids: Seq[AttemptId]): List[QuizAttemptEntity]

  /** Gets all attempts for all quizzes with the given [[EdgePath]] s in the given course.
    *
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the quizzes for the attempts
    * @param forUpdate
    *   whether to pessimistically lock the attempts for update
    * @return
    *   the found attempts
    */
  def getAttempts(contextId: Long, edgePaths: Seq[EdgePath], forUpdate: Boolean = false): Seq[QuizAttemptEntity]

  /** Gets aggregate quiz participation data.
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the quizzes for the attempts
    * @return
    *   participation data for quizzes with attempts.
    */
  def getParticipationData(contextId: Long, edgePaths: Seq[EdgePath]): Map[EdgePath, AssessmentParticipation]

  /** Counts the number of attempts in a submitted (non-finalized) state per the grading policy.
    * @param contextId
    * @param edgePaths
    * @param policy
    * @return
    */
  def attemptsAwaitingGrade(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    policy: AssessmentGradingPolicy
  ): Map[EdgePath, Int]

  /** Gets all attempts for all quizzes with the given [[EdgePath]] s in the given course for a single user.
    *
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the quizzes for the attempts
    * @param userId
    *   the subject of the attempts
    * @param forUpdate
    *   whether to pessimistically lock the attempts for update
    * @return
    *   the found attempts
    */
  def getUserAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    userId: Long,
    forUpdate: Boolean = false
  ): Seq[QuizAttemptEntity]

  /** Calculates attempt counts for all users for all quizzes with the given [[EdgePath]] s.
    *
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the quizzes for the attempts
    * @return
    *   the attempt counts for the found attempts
    */
  def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Seq[UserAttemptCounts]

  /** Calculates attempt counts for a single user for all quizzes with the given [[EdgePath]] s.
    *
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the quizzes for the attempts
    * @param userId
    *   the subject of the attempts
    * @return
    *   the attempt counts for the found attempts
    */
  def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath], userId: Long): Seq[UserAttemptCounts]

  def countValidAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Int

  def countValidAttempts(contextId: Long, edgePath: EdgePath, userId: Long): Int
end QuizAttemptDao

@Service
class QuizAttemptDaoImpl(session: => Session, idFactory: PersistenceIdFactory) extends QuizAttemptDao:

  def newAttempt(
    quiz: Quiz,
    user: UserDTO,
    createTime: Date,
    maxMinutes: Option[Long],
    questions: Seq[QuizAttemptQuestionUsage],
    responses: Seq[QuizQuestionResponse],
    attemptFolder: QuizAttemptFolderEntity,
    domain: DomainDTO
  ): QuizAttemptEntity =
    val attemptEntity: QuizAttemptEntity = QuizAttemptEntity.newAttempt(
      idFactory.generateId(),
      domain.id,
      quiz,
      user,
      createTime,
      maxMinutes,
      questions,
      responses,
      attemptFolder
    )

    session.persist(attemptEntity)

    attemptEntity
  end newAttempt

  override def write(attempt: QuizAttemptEntity): QuizAttemptEntity =
    ThreadTerminator.check()
    session.merge(attempt).asInstanceOf[QuizAttemptEntity]

  override def load(id: AttemptId, pessimisticLock: Boolean): Option[QuizAttemptEntity] =
    ThreadTerminator.check()
    val lockMode =
      if pessimisticLock then LockModeType.PESSIMISTIC_WRITE
      else LockModeType.NONE

    Option(session.find(classOf[QuizAttemptEntity], id.value, lockMode))

  override def load(ids: Seq[AttemptId]): List[QuizAttemptEntity] =
    ThreadTerminator.check()
    session.bulkLoadFromCaches[QuizAttemptEntity](ids.map(_.value))

  override def getAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    forUpdate: Boolean = false
  ): Seq[QuizAttemptEntity] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val query: Query[QuizAttemptEntity] =
        session
          .createQuery[QuizAttemptEntity](
            s"""
               | FROM ${entityName[QuizAttemptEntity]}
               | WHERE contextId = :contextId
               | AND edgePath in :edgePaths
             """.stripMargin,
            classOf[QuizAttemptEntity]
          )
          .setParameter("contextId", Long.box(contextId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)

      if forUpdate then query.setLockMode(LockModeType.PESSIMISTIC_WRITE)

      query.getResultList.asScala.toList
        .sortBy(_.createTime)

  override def getParticipationData(contextId: Long, edgePaths: Seq[EdgePath]): Map[EdgePath, AssessmentParticipation] =
    Baroquitechture.getParticipationData[QuizAttemptEntity](session, contextId, edgePaths)

  override def attemptsAwaitingGrade(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    policy: AssessmentGradingPolicy
  ): Map[EdgePath, Int] =
    Baroquitechture.attemptsAwaitingGrade[QuizAttemptEntity](session, contextId, edgePaths, policy)

  override def getUserAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    userId: Long,
    forUpdate: Boolean = false
  ): Seq[QuizAttemptEntity] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val query: Query[QuizAttemptEntity] =
        session
          .createQuery[QuizAttemptEntity](
            s"""
               | FROM ${entityName[QuizAttemptEntity]}
               | WHERE contextId = :contextId
               | AND userId = :userId
               | AND edgePath in :edgePaths
             """.stripMargin,
            classOf[QuizAttemptEntity]
          )
          .setParameter("contextId", Long.box(contextId))
          .setParameter("userId", Long.box(userId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)

      if forUpdate then query.setLockMode(LockModeType.PESSIMISTIC_WRITE)

      query.getResultList.asScala.toList
        .sortBy(_.createTime)

  override def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Seq[UserAttemptCounts] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val aggQuery =
        s"""
           | SELECT new ${entityName[
            AttemptStateCountRow
          ]}(contextId, edgePath, userId, attemptState, MAX(submitTime), COUNT(attemptState))
           | FROM ${entityName[QuizAttemptEntity]}
           | WHERE contextId = :contextId
           | AND edgePath in :edgePaths
           | AND valid = true
           | GROUP BY contextId, edgePath, userId, attemptState
         """.stripMargin

      val results: Seq[AttemptStateCountRow] =
        session
          .createQuery[AttemptStateCountRow](aggQuery, classOf[AttemptStateCountRow])
          .setParameter("contextId", Long.box(contextId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)
          .getResultList
          .asScala
          .toSeq

      AttemptStateCountRow.toUserAttemptCounts(results)

  override def aggregateUserAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    userId: Long
  ): Seq[UserAttemptCounts] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val aggQuery =
        s"""
           | SELECT new ${entityName[
            AttemptStateCountRow
          ]}(contextId, edgePath, userId, attemptState, MAX(submitTime), COUNT(attemptState))
           | FROM ${entityName[QuizAttemptEntity]}
           | WHERE contextId = :contextId
           | AND userId = :userId
           | AND edgePath in :edgePaths
           | AND valid = true
           | GROUP BY contextId, edgePath, userId, attemptState
         """.stripMargin

      val results: Seq[AttemptStateCountRow] =
        session
          .createQuery[AttemptStateCountRow](aggQuery, classOf[AttemptStateCountRow])
          .setParameter("contextId", Long.box(contextId))
          .setParameter("userId", Long.box(userId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)
          .getResultList
          .asScala
          .toSeq

      AttemptStateCountRow.toUserAttemptCounts(results)

  override def countValidAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath]
  ): Int =
    edgePaths ?? {
      val countQuery =
        s"""
         | SELECT COUNT(*)
         | FROM ${entityName[QuizAttemptEntity]}
         | WHERE contextId = :contextId
         | AND edgePath IN :edgePaths
         | AND valid = TRUE
       """.stripMargin

      session
        .createQuery(countQuery, classOf[Long])
        .setParameter("contextId", Long.box(contextId))
        .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)
        .getSingleResult
        .intValue
    }

  override def countValidAttempts(
    contextId: Long,
    edgePath: EdgePath,
    userId: Long
  ): Int =
    val countQuery =
      s"""
         | SELECT COUNT(*)
         | FROM ${entityName[QuizAttemptEntity]}
         | WHERE contextId = :contextId
         | AND userId = :userId
         | AND edgePath = :edgePath
         | AND valid = TRUE
       """.stripMargin

    session
      .createQuery(countQuery, classOf[Long])
      .setParameter("contextId", Long.box(contextId))
      .setParameter("userId", Long.box(userId))
      .setParameter("edgePath", edgePath.toString)
      .getSingleResult
      .intValue
  end countValidAttempts
end QuizAttemptDaoImpl
