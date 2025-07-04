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

import argonaut.CodecJson
import com.learningobjects.cpxp.scala.json.ArgoOps.*
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.reference.EdgePath
import scalaz.Semigroup
import scalaz.std.anyVal.*
import scalaz.std.option.*
import scalaz.syntax.semigroup.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras

import java.time.Instant
import scala.jdk.CollectionConverters.*

/** A single user's progress on a single piece of content.
  *
  * Wholly decontextualized. This is stored on the user's [[UserProgressFinder]] entity keyed by edge path. It is not
  * intended to be used outside of progress internals; see [[report.Progress the external DTO]] and similar for that.
  */
final case class UserProgressNode(
  completions: Int,
  total: Int,
  incrementTypes: IncrementTypes,
  forCreditGrades: Option[Int],
  forCreditGradesPossible: Option[Int]
):
  def toWebProgress: report.Progress =
    report.Progress(
      completions = completions,
      total = total,
      progressTypes = incrementTypes.toSet.asJava,
      forCreditGrades = forCreditGrades,
      forCreditGradesPossible = forCreditGradesPossible
    )

  // upgrade refers to the March 2023 addition of forCreditGrades and forCreditGradesPossible
  def upgraded(forCreditGrades: Int, forCreditGradesPossible: Int): UserProgressNode =
    copy(forCreditGrades = forCreditGrades.some, forCreditGradesPossible = forCreditGradesPossible.some)
end UserProgressNode

object UserProgressNode:
  def emptyLeaf(forCreditGrades: Int, forCreditGradesPossible: Int): UserProgressNode = UserProgressNode(
    0,
    1,
    IncrementTypes.empty,
    forCreditGrades.some,
    forCreditGradesPossible.some
  )

  val Excluded: UserProgressNode = UserProgressNode(0, 0, IncrementTypes.empty, 0.some, 0.some)

  type Map = Predef.Map[EdgePath, UserProgressNode]

  implicit val codec: CodecJson[UserProgressNode] =
    CodecJson.casecodec5(UserProgressNode.apply, ArgoExtras.unapply)(
      "completions",
      "total",
      "incrementTypes",
      "forCreditGrades",
      "forCreditGradesPossible"
    )

  implicit val semigroupForUserProgressNode: Semigroup[UserProgressNode] = Semigroup.instance((l, r) =>
    UserProgressNode(
      l.completions |+| r.completions,
      l.total |+| r.total,
      l.incrementTypes |+| r.incrementTypes,
      l.forCreditGrades |+| r.forCreditGrades,
      l.forCreditGradesPossible |+| r.forCreditGradesPossible
    )
  )
end UserProgressNode

/** @param lastModified
  *   timestamp when learner activity changed the visitation-based progress. Content-update recalculation can change
  *   visitation-based progress without changing lastModified because content-update recalculation is not learner
  *   activity. Grade-based progress changes do not change lastModified.
  */
final case class UserProgressEntity(
  map: Map[EdgePath, UserProgressNode],
  lastModified: Option[Instant],
  generation: Option[Long],
):
  def toWebProgress(user: UserId): report.ProgressReport = report.ProgressReport(
    user.id,
    lastModified,
    map.map { case (path, prog) =>
      ProgressPath.fromEdgePath(path) -> prog.toWebProgress
    }.asJava
  )
end UserProgressEntity

object UserProgressEntity:
  def fromFaçade(f: UserProgressFacade): UserProgressEntity =

    val map = f.getProgressMap
      .as_![Map[EdgePath, UserProgressNode]](s"cannot decode userprogressfinder ${f.getId}")
      .get

    UserProgressEntity(map, f.getLastModified, f.getGeneration)

  def fromFinder(f: UserProgressFinder): UserProgressEntity =

    val map = f.progressMap
      .as_![Map[EdgePath, UserProgressNode]](s"cannot decode userprogressfinder ${f.getId}")
      .get

    UserProgressEntity(map, Option(f.lastModified).map(_.toInstant), Option(f.generation))
end UserProgressEntity
