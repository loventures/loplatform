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

package loi.cp.content
package gate

import loi.cp.lwgrade.{Grade, StudentGradebook}
import loi.cp.reference.EdgePath
import scalaz.std.option.*
import scalaz.std.lazylist.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.double.*

/** A performance rule.
  *
  * a.k.a. "gradebook gating policy" or "activity gating policy".
  *
  * Allows a student access to content only when they have achieved a minimum grade on a set of assignments.
  *
  * @param criteria
  *   a list of assignment IDs and their normalised (between 0 and 1) grade which must be achieved to allow access
  * @param disabled
  *   a set of assignment IDs (from `criteria.keySet`) which should not be applied. This exists to replace the
  *   functionality of heavyweight CAS whereby a single gradebook policy could be deleted, corresponding to the deletion
  *   of a database row.
  */
final case class PerformanceRule(
  criteria: List[(EdgePath, Double)],
  disabled: Set[EdgePath],
)

object PerformanceRule:
  final val none = PerformanceRule(List.empty, Set.empty)

  final case class Status(value: GateStatus)

  /** @param gaters
    *   map from gated paths to gating assignment name+criteria
    * @param disabled
    *   whether it's disabled
    */
  final case class Structure(
    gaters: Map[EdgePath, List[(EdgePath, Double)]],
    disabled: Map[EdgePath, Set[EdgePath]],
  )

  def loadPerformanceRules[D <: Tuple](
    structure: Structure,
  ): AnnotatedTree[D] => Map[EdgePath, PerformanceRule] = tree =>
    // edge path => node
    val assetsForContents = tree.unannotated.flatten
    getPerformanceRules(assetsForContents, structure)

  def addPerformanceRules[D <: Tuple](
    structure: Structure,
  ): AnnotatedTree[D] => AnnotatedTree[PerformanceRule *: D] = ct =>
    val pr = loadPerformanceRules(structure)(ct)
    tree.mapped(cc => pr(cc.edgePath))(ct)

  def getPerformanceRules(contentList: List[CourseContent], structure: Structure): Map[EdgePath, PerformanceRule] =
    contentList.map { cc =>
      cc.edgePath -> PerformanceRule(
        criteria = structure.gaters.getOrElse(cc.edgePath, List.empty),
        disabled = structure.disabled.getOrElse(cc.edgePath, Set.empty),
      )
    }.toMap

  def evaluateRule(rule: PerformanceRule, gb: StudentGradebook): Status = Status {
    rule.criteria
      .to(LazyList)
      .map { case (path, threshold) =>
        val grade    = gb.grades.get(path).flatMap(Grade.fraction) // fraction means drop ExtraCredit...?
        val passes   = grade ∃ (_ ⪆ threshold)
        val disabled = rule.disabled contains path
        if passes || disabled then GateStatus.Open else GateStatus.Locked
      }
      .maximum | GateStatus.Open // greater value == more restrictive
  }

  def evaluateRules(gb: StudentGradebook) =
    tree.mappedGiven[PerformanceRule, Status] { (_, rule) =>
      evaluateRule(rule, gb)
    }

  def evaluateRules(performanceRules: Map[EdgePath, PerformanceRule], gb: StudentGradebook): Map[EdgePath, Status] =
    performanceRules.transform { (_, rule) =>
      evaluateRule(rule, gb)
    }
end PerformanceRule
