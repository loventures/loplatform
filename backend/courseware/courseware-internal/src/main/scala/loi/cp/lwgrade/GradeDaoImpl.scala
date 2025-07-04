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

package loi.cp.lwgrade

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.json.ArgoOps.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.grade.LWGradeFinder
import com.learningobjects.cpxp.service.query.{BaseCondition, BaseDataProjection, Comparison}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import scalaz.NonEmptyList
import scaloi.syntax.CollectionOps.*

import scala.jdk.CollectionConverters.*

/** This class is responsible for storing grades to the database. It creates a row for each user and course, persisting
  * a JSON document containing all the grades user has received.
  *
  * Example json document describing users grade in a course: "1/2/3/4/5" = content path
  *
  * { "root" : "12345", "grades" : { "1/2/3/4/6" : { "score" : { "value" : 1.0, "max": 1.0 }, "submission" :
  * "2018-04-20T15:41:10.568Z" "assignment" : "12345567891L" } "1/2/3/4/7" : { "score" : { "value" 14.5, "max": "50" },
  * "submission" : "2018-04-20T16:10:10.269Z" "assignment" : "12345567891L" } } }
  *
  * TODO: Would storing records in a User,Enrollment tuple make more sense? Can a user be enrolled more than once to a
  * course? What are the semantics for the grade book if that is the case?
  */
@Service
final class GradeDaoImpl(implicit
  facadeService: FacadeService,
) extends GradeDao:

  def loadOrCreate(user: UserId, course: ContextId): RawGrades =
    parseGrades(fetchGradeFacade(user, course))

  override def load(
    users: NonEmptyList[UserId],
    course: ContextId
  ): Map[UserId, RawGrades] =

    val gradeFacades = course
      .facade[LWGradeParentFacade]
      .queryLWGrades
      .addCondition(BaseCondition.getInstance(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE, Comparison.eq, course))
      .addCondition(BaseCondition.inIterable(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_USER, users.stream.toList))
      .getFacades[LWGradeFacade]

    val gradesByUser = gradeFacades.groupMapUniq(facade => UserId(facade.getUser))(parseGrades)

    // add empty grades for any user that doesn't have one so that we behave
    // like loadOrCreate does
    users.map(user => user -> gradesByUser.getOrElse(user, Map.empty)).stream.toMap
  end load

  def save(user: UserId, course: ContextId, newGradeBook: RawGrades): Unit =
    fetchGradeFacade(user, course).setGradeGraph(GradeJson(newGradeBook).asJson)

  // TODO: init gradebooks for enrolled users that have not been created yet.
  def loadByCourse(course: ContextId): Map[UserId, RawGrades] =
    val gradeFacades = course
      .facade[LWGradeParentFacade]
      .queryLWGrades
      .addCondition(BaseCondition.getInstance(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE, Comparison.eq, course))
      .getFacades[LWGradeFacade]

    gradeFacades
      .map(lwgf => UserId(lwgf.getUser) -> parseGrades(lwgf))
      .toMap
  end loadByCourse

  override def loadUserIdsByCourse(course: ContextId): List[Long] =
    course
      .facade[LWGradeParentFacade]
      .queryLWGrades
      .addCondition(BaseCondition.getInstance(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_COURSE, Comparison.eq, course))
      .setDataProjection(BaseDataProjection.ofDatum(LWGradeFinder.DATA_TYPE_LIGHTWEIGHT_USER))
      .getResultList
      .asInstanceOf[java.util.List[Number]]
      .asScala
      .view
      .map(_.longValue)
      .toList

  def delete(user: UserId, course: ContextId): Unit =
    course.facade[LWGradeParentFacade].getLWGrade(user = user.id, course = course.id) foreach { facade =>
      facade.delete()
    }

  def transferGrades(user: UserId, srcCourse: ContextId, tgtCourse: ContextId): Unit =
    srcCourse.facade[LWGradeParentFacade].getLWGrade(user = user.id, course = srcCourse.id) foreach { srcGradeFacade =>
      val srcGrades = parseGrades(srcGradeFacade)
      save(user, tgtCourse, srcGrades)
      srcGradeFacade.delete()
    }

  // JSON parse failures are fatal, like a dropped column would be
  private def parseGrades(grade: LWGradeFacade): RawGrades =
    grade.getGradeGraph
      .as_![GradeJson](s"json parse failure on grade ${grade.getId}")
      .map(_.grades)
      .get

  private def fetchGradeFacade(user: UserId, course: ContextId): LWGradeFacade =
    course
      .facade[LWGradeParentFacade]
      .getOrCreateLWGrade(user = user.id, course = course.id) {
        _.setGradeGraph(GradeJson(Map.empty).asJson)
      }
end GradeDaoImpl
