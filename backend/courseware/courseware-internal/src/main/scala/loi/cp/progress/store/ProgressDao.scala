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

package loi.cp.progress
package store

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.reference.EdgePath
import org.hibernate.Session
import scalaz.Endo
import scaloi.syntax.boxes.*
import scaloi.syntax.option.*

import java.time.Instant
import scala.jdk.CollectionConverters.*

@Service
class ProgressDao(
  session: => Session,
  domain: => DomainDTO
)(implicit
  facadeService: FacadeService,
):

  def loadUserProgress(userId: Long, courseId: Long, forUpdate: Boolean): Option[UserProgressEntity] =
    userId
      .facade[UserProgressParentFacade]
      .getProgress(courseId)
      .tap({ pf =>
        if forUpdate then
          session.flush(); pf.refresh(pessi = true)
      })
      .map(UserProgressEntity.fromFaçade)

  def loadUserProgresses(userIds: List[Long], courseId: Long): Map[UserId, UserProgressEntity] =

    if userIds.isEmpty then Map.empty
    else

      session
        .createQuery(
          """
        |FROM UserProgressFinder up
        |WHERE up.parent.id IN :userIds
        |  AND up.course.id = :courseId
        |  AND up.del IS NULL
        |  AND up.root.id = :rootId
      """.stripMargin,
          classOf[UserProgressFinder]
        )
        .setParameter("userIds", userIds.boxInside().asJava)
        .setParameter("courseId", courseId)
        .setParameter("rootId", domain.id)
        .getResultList
        .asScala
        .map(finder => UserId(finder.parent.getId) -> UserProgressEntity.fromFinder(finder))
        .toMap

  def loadUserProgresses(courseId: Long): Seq[(Long, UserProgressEntity)] =
    for façade <- courseId.facade[CourseProgressRelativeFacade].getProgresses(courseId)
    yield façade.getParentId.longValue -> UserProgressEntity.fromFaçade(façade)

  /** Loads the user IDs of progress documents for `sectionId` whose generation is less than `generation.`
    */
  // free sections can reach 20,000 learners, hence the return of ID instead of the full object
  // plus the caller wants to write anyway, which is its own transaction and pessi lock.
  // then again, JPA is probably loading the whole entity anyway
  // the generation check is not very important for the one caller of this
  def loadStaleUserProgressUserIds(sectionId: Long, generation: Long): List[Long] =
    session
      .createQuery(
        """SELECT up.parent.id
          |FROM UserProgressFinder up
          |WHERE up.course.id = :sectionId
          |  AND up.generation < :generation
          |  AND up.del IS NULL
          |  AND up.root.id = :rootId
          |""".stripMargin,
        classOf[Number]
      )
      .setParameter("sectionId", sectionId)
      .setParameter("generation", generation)
      .setParameter("rootId", domain.id)
      .getResultList
      .asScala
      .toList
      .map(_.longValue)

  def saveUserProgress(
    userId: Long,
    courseId: Long,
    generation: Option[Long],
    map: Map[EdgePath, UserProgressNode],
    lastModified: Option[Instant],
  ): UserProgressEntity =

    def update(facade: UserProgressFacade): Unit =
      facade.setProgressMap(map.asJson)
      facade.setLastModified(lastModified)

    val facade = userId
      .facade[UserProgressParentFacade]
      // We pass the initialization function, `update` to `getOrCreate` because
      // the `progress` and `lastModified` columns are not null, and Hibernate
      // will not allow you to attach an object to its session when its
      // properties violate its model of the entity. The init function runs
      // before we attach the object to the session.
      .getOrCreateProgress(courseId, update)

      // We run the `update` function when we have a `Gotten` `GetOrCreate`
      // value, because initialization function above doesn't run on `Gotten`
      // values.
      .update(update)
      .result

    generation foreach facade.setGeneration
    UserProgressEntity.fromFaçade(facade)
  end saveUserProgress

  def deleteUserProgress(
    userId: Long,
    sectionId: Long,
  ): Unit =
    userId.facade[UserProgressParentFacade].getProgress(sectionId) foreach { facade =>
      facade.delete()
    }

  def transferUserProgress(
    userId: Long,
    srcCrsId: Long,
    dstCrsId: Long,
    adjust: Endo[UserProgressEntity]
  ): Unit =
    val parent = userId.facade[UserProgressParentFacade]
    parent.jefresh()
    parent
      .getProgress(srcCrsId)
      .foreach(srcFacade =>
        val srcEntity   = UserProgressEntity.fromFaçade(srcFacade)
        val dstEntity   = adjust(srcEntity)
        val dstProgress =
          saveUserProgress(userId, dstCrsId, dstEntity.generation, dstEntity.map, srcEntity.lastModified)
        srcFacade.delete()
      )
  end transferUserProgress
end ProgressDao
