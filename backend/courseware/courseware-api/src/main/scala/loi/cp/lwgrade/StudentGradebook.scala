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

import argonaut.{DecodeJson, EncodeJson}
import loi.cp.content.CourseContents
import loi.cp.context.ContextId
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection}
import loi.cp.lwgrade.Grade.GradeExtractor
import loi.cp.reference.EdgePath
import scalaz.*
import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.std.option.*
import scalaz.syntax.align.*
import scalaz.syntax.equal.*
import scalaz.syntax.foldable.*
import scaloi.syntax.all.toCollectionOps

sealed abstract case class StudentGradebook(grades: RawGrades)(val structure: GradeStructure):

  import StudentGradebook.*

  def overall: Option[Grade] = grades.get(EdgePath.Root)

  def get(path: EdgePath): Option[Grade] = grades.get(path)

  def +(pathGrade: (EdgePath, Grade)): StudentGradebook =
    plus(pathGrade._1, pathGrade._2)

  def plus(path: EdgePath, grade: Grade): StudentGradebook =
    rollup(Grade.RawGradeExtractor, grades + (path -> grade), structure)

  def ++(pgs: Seq[(EdgePath, Grade)]): StudentGradebook =
    rollup(Grade.RawGradeExtractor, grades ++ pgs, structure)
end StudentGradebook

object StudentGradebook:

  implicit val encodeGradebook: EncodeJson[StudentGradebook] =
    EncodeJson.jencode1L[StudentGradebook, RawGrades](_.grades)("grades")

  implicit def decodeGradebook(implicit structure: GradeStructure): DecodeJson[StudentGradebook] =
    DecodeJson { hc =>
      for grades <- hc.downField("grades").as[RawGrades]
      yield apply(grades)(structure)
    }

  def empty(structure: GradeStructure): StudentGradebook =
    new StudentGradebook(mapDefaultGrades(structure))(structure) {}

  def apply(grades: RawGrades)(structure: GradeStructure): StudentGradebook =
    empty(structure) ++ grades.toSeq

  def diff(l: StudentGradebook, r: StudentGradebook): List[GradeChange] =
    val notInLeft = (l.grades -- r.grades.keySet).iterator.map { case (edge, grade) =>
      GradeChange(edge, Some(grade), None)
    }.toList

    val notInRight = (r.grades -- l.grades.keySet).iterator.map { case (edge, grade) =>
      GradeChange(edge, None, Some(grade))
    }.toList

    val changed = (l.grades alignWith r.grades) {
      case \&/.Both(leftGrade, rightGrade) if leftGrade =/= rightGrade =>
        Some((leftGrade, rightGrade))
      case _                                                           =>
        None
    }.toList.collect { case (path, Some((leftGrade, rightGrade))) =>
      GradeChange(path, Some(leftGrade), Some(rightGrade))
    }

    notInLeft ::: notInRight ::: changed
  end diff

  /** Applies the course's configuration of the grade view to this gradebook.
    */
  def applyRollupGradeViewConfig(
    cs: CourseConfigurationService,
    course: CourseSection,
    realGb: StudentGradebook
  ): StudentGradebook =
    applyRollupGradeViewConfig(cs, course.contents, course, realGb)

  /** Applies the course's configuration of the grade view to this gradebook.
    */
  def applyRollupGradeViewConfig(
    cs: CourseConfigurationService,
    contents: CourseContents,
    courseId: ContextId,
    realGb: StudentGradebook
  ): StudentGradebook =
    val useProjectedGrade = cs.getGroupConfig(CoursePreferences, courseId).useProjectedGrade
    if useProjectedGrade then
      val structure = GradeStructure(contents)
      StudentGradebook.rollup(Grade.ProjectedGradeExtractor, realGb.grades, structure)
    else realGb
  end applyRollupGradeViewConfig

  /** Traverse the course structure computing rollups for the entire course.
    */
  def rollup(
    extractor: GradeExtractor,
    grades: Map[EdgePath, Grade],
    courseStructure: GradeStructure,
  ): StudentGradebook =
    // DANGER! WARNING! This is not a monoid! rollup(ungraded, a) !== a !== rollup(a, ungraded)
    val ungraded: Grade = Grade.Ungraded(0.0)
    val rollup          = Grade.rollup(extractor)
    val categories      = courseStructure.categories.toList
    val totalWeight     = categories.flatMap(_.weight).foldMap(_.doubleValue)

    val categoryGrades: List[(EdgePath, Grade, Grade)] =
      for category <- categories
      yield
        val columns        = category.columns.toList
        val categoryRollup = columns.map(_.path).flatMap(grades.get).foldLeft(ungraded)(rollup)
        val weightedRollup =
          if totalWeight == 0.0 then categoryRollup
          else Grade.weightedRollup(categoryRollup, category.weight.foldMap(_.doubleValue))
        (category.path, categoryRollup, weightedRollup)

    val overall: (EdgePath, Grade) = EdgePath.Root -> categoryGrades.map(_._3).foldLeft(ungraded)(rollup)

    val categoryGradeMap = categoryGrades.groupMapUniq(_._1)(_._2)

    new StudentGradebook(grades ++ categoryGradeMap + overall)(courseStructure) {}
  end rollup

  /** Map course structure to a default [[Grade]] for each path.
    */
  private def mapDefaultGrades(courseStructure: GradeStructure): RawGrades = {
    for
      category   <- courseStructure.categories
      assignment <- category.columns
      grade       = gradeDefault(assignment)
    yield assignment.path -> grade
  }.toMap

  implicit val doubleMonoid: Monoid[Double] = Monoid.instance(_ + _, 0.0)
end StudentGradebook
