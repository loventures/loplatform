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

package loi.cp.submissionassessment.persistence

import java.time.Instant
import java.util.Date

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.submissionassessment.{SubmissionAttemptEntity, SubmissionAttemptFolderEntity}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import com.learningobjects.cpxp.util.{PersistenceIdFactory, ThreadTerminator}
import jakarta.persistence.LockModeType
import loi.cp.assessment.{AssessmentGradingPolicy, AssessmentParticipation, AttemptId}
import loi.cp.assessment.attempt.UserAttemptCounts
import loi.cp.assessment.persistence.{AttemptStateCountRow, Baroquitechture}
import loi.cp.reference.EdgePath
import loi.cp.submissionassessment.SubmissionAssessment
import org.hibernate.query.Query
import org.hibernate.Session
import scalaz.std.anyVal.*
import scaloi.syntax.collection.*

import scala.jdk.CollectionConverters.*

/** A service for updating and retrieving persisted submission attempts.
  */
@Service
trait SubmissionAttemptDao:
  def newAttempt(
    submissionAssessment: SubmissionAssessment,
    user: UserId,
    createTime: Instant,
    attemptFolder: SubmissionAttemptFolderEntity,
    domain: DomainDTO
  ): SubmissionAttemptEntity

  def getAttempts(contextId: Long, edgePaths: Seq[EdgePath], forUpdate: Boolean = false): Seq[SubmissionAttemptEntity]

  def getUserAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    userId: Long,
    forUpdate: Boolean = false
  ): Seq[SubmissionAttemptEntity]

  def countValidAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Int

  def countValidAttempts(contextId: Long, edgePath: EdgePath, userId: Long): Int

  def write(attempt: SubmissionAttemptEntity): SubmissionAttemptEntity

  def load(id: AttemptId, pessimisticLock: Boolean): Option[SubmissionAttemptEntity]

  def load(ids: Seq[AttemptId]): List[SubmissionAttemptEntity]

  def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Seq[UserAttemptCounts]

  def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath], userId: Long): Seq[UserAttemptCounts]

  /** Gets aggregate "quiz" participation data.
    * @param contextId
    *   the course containing the attempts
    * @param edgePaths
    *   the paths of the "quizzes" for the attempts
    * @return
    *   participation data for "quizzes" with attempts.
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
end SubmissionAttemptDao

@Service
class SubmissionAttemptDaoImpl(session: => Session, idFactory: PersistenceIdFactory) extends SubmissionAttemptDao:

  override def newAttempt(
    submissionAssessment: SubmissionAssessment,
    user: UserId,
    createTime: Instant,
    attemptFolder: SubmissionAttemptFolderEntity,
    domain: DomainDTO
  ): SubmissionAttemptEntity =
    val attemptEntity: SubmissionAttemptEntity =
      SubmissionAttemptEntity.newAttempt(
        idFactory.generateId(),
        domain.id,
        submissionAssessment,
        user,
        Date.from(createTime),
        attemptFolder
      )

    session.persist(attemptEntity)

    attemptEntity
  end newAttempt

  override def getAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    forUpdate: Boolean = false
  ): Seq[SubmissionAttemptEntity] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val query: Query[SubmissionAttemptEntity] =
        session
          .createQuery[SubmissionAttemptEntity](
            s"""
               | FROM ${classOf[SubmissionAttemptEntity].getName}
               | WHERE contextId = :contextId
               | AND edgePath in :edgePaths
             """.stripMargin,
            classOf[SubmissionAttemptEntity]
          )
          .setParameter("contextId", Long.box(contextId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)

      if forUpdate then query.setLockMode(LockModeType.PESSIMISTIC_WRITE)

      query.getResultList.asScala.toList
        .sortBy(_.createTime)

  override def getUserAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    userId: Long,
    forUpdate: Boolean = false
  ): Seq[SubmissionAttemptEntity] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val query: Query[SubmissionAttemptEntity] =
        session
          .createQuery[SubmissionAttemptEntity](
            s"""
               | FROM ${classOf[SubmissionAttemptEntity].getName}
               | WHERE contextId = :contextId
               | AND userId = :userId
               | AND edgePath in :edgePaths
             """.stripMargin,
            classOf[SubmissionAttemptEntity]
          )
          .setParameter("contextId", Long.box(contextId))
          .setParameter("userId", Long.box(userId))
          .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)

      if forUpdate then query.setLockMode(LockModeType.PESSIMISTIC_WRITE)

      query.getResultList.asScala.toList
        .sortBy(_.createTime)

  override def write(attempt: SubmissionAttemptEntity): SubmissionAttemptEntity =
    ThreadTerminator.check()
    session.merge(attempt).asInstanceOf[SubmissionAttemptEntity]

  override def load(id: AttemptId, pessimisticLock: Boolean): Option[SubmissionAttemptEntity] =
    ThreadTerminator.check()
    val lockMode =
      if pessimisticLock then LockModeType.PESSIMISTIC_WRITE
      else LockModeType.NONE

    Option(session.find(classOf[SubmissionAttemptEntity], id.value, lockMode))

  override def load(ids: Seq[AttemptId]): List[SubmissionAttemptEntity] =
    ThreadTerminator.check()
    session.bulkLoadFromCaches[SubmissionAttemptEntity](ids.map(_.value))

  override def aggregateUserAttempts(contextId: Long, edgePaths: Seq[EdgePath]): Seq[UserAttemptCounts] =
    if edgePaths.isEmpty then
      // Searching on no assessments yield no results
      Nil
    else
      val aggQuery =
        s"""
           | SELECT new ${classOf[
            AttemptStateCountRow
          ].getName}(contextId, edgePath, userId, attemptState, MAX(submitTime), COUNT(attemptState))
           | FROM ${classOf[SubmissionAttemptEntity].getName}
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

      results
        .groupMap(row => (Long.unbox(row.contextIdValue), row.edgePath, Long.unbox(row.userId)))(row =>
          (row.stateName, Long.unbox(row.count).intValue(), Option(row.latestSubmissionTime).map(_.toInstant))
        )
        .map({ case ((contextIdValue, edgePath, userId), stateAndCounts) =>
          UserAttemptCounts(contextIdValue, edgePath, userId, stateAndCounts)
        })
        .toSeq

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
           | SELECT new ${classOf[
            AttemptStateCountRow
          ].getName}(contextId, edgePath, userId, attemptState, MAX(submitTime), COUNT(attemptState))
           | FROM ${classOf[SubmissionAttemptEntity].getName}
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

      results
        .groupMap(row => (Long.unbox(row.contextIdValue), row.edgePath, Long.unbox(row.userId)))(row =>
          (row.stateName, Long.unbox(row.count).intValue(), Option(row.latestSubmissionTime).map(_.toInstant))
        )
        .map({ case ((contextIdValue, edgePath, userId), stateAndCounts) =>
          UserAttemptCounts(contextIdValue, edgePath, userId, stateAndCounts)
        })
        .toSeq

  override def getParticipationData(
    contextId: Long,
    edgePaths: Seq[EdgePath]
  ): Map[EdgePath, AssessmentParticipation] =
    Baroquitechture.getParticipationData[SubmissionAttemptEntity](session, contextId, edgePaths)

  override def attemptsAwaitingGrade(
    contextId: Long,
    edgePaths: Seq[EdgePath],
    policy: AssessmentGradingPolicy
  ): Map[EdgePath, Int] =
    Baroquitechture.attemptsAwaitingGrade[SubmissionAttemptEntity](session, contextId, edgePaths, policy)

  override def countValidAttempts(
    contextId: Long,
    edgePaths: Seq[EdgePath]
  ): Int =
    edgePaths ?? {
      val countQuery =
        s"""
         | SELECT COUNT(*)
         | FROM ${entityName[SubmissionAttemptEntity]}
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
         | FROM ${entityName[SubmissionAttemptEntity]}
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
end SubmissionAttemptDaoImpl
