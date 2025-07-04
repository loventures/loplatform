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

import loi.cp.lwgrade.Grade.Graded
import loi.cp.lwgrade.{Grade, StudentGradebook}
import loi.cp.reference.EdgePath

import scalaz.\/
import scalaz.syntax.std.option.*

object LightweightOutcomesService:

  def getGraded(studentId: Long, contextId: Long, edgePath: EdgePath, grade: Grade): Exception \/ Graded =
    PartialFunction.condOpt(grade)({ case g: Graded => g }) \/>
      new RuntimeException(
        s"Grade for student: $studentId in course $contextId for edgePath: $edgePath " +
          s"is an unsupported type (${grade.getClass})"
      )

  def getSingleLtiScore(studentId: Long, contextId: Long, edgePath: EdgePath, grade: Grade): Exception \/ Double =
    Grade.fraction(grade) \/> new RuntimeException(
      s"Grade for student: $studentId, and edgePath: $edgePath with value $grade was not able to be fractioned"
    )

  def getGradeForStudent(
    studentId: Long,
    contextId: Long,
    edgePath: EdgePath,
    studentGradebook: StudentGradebook
  ): Exception \/ Grade =
    studentGradebook.grades.get(edgePath) \/> new RuntimeException(
      s"Grade for student: $studentId doesn't exist for edgePath: $edgePath in context: $contextId"
    )
end LightweightOutcomesService
