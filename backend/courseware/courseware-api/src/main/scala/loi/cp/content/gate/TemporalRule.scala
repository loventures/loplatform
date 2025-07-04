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

import java.time.Instant

import loi.cp.reference.EdgePath
import scalaz.syntax.order.*
import scaloi.misc.InstantInstances.*

/** Representation of a (computed) temporal gate on a single piece of content in a course.
  *
  * The applied gate is given by an offset on the content asset's incoming edge, added to the latest gating date of its
  * parents. (This reflects the fact that a student must be able to open a module in order to open content within it.)
  */
final case class TemporalRule(lockDate: Option[Instant]):
  def evaluate(now: Instant): GateStatus =
    if lockDate.exists(_ > now) then GateStatus.Locked else GateStatus.Open

object TemporalRule:
  final val none                          = TemporalRule(None)
  def at(lockDate: Instant): TemporalRule = TemporalRule(Some(lockDate))

  final case class Status(value: GateStatus)

  /** Map over a content tree, collecting [[TemporalRule]] s onto the contents.
    *
    * @param dates
    *   the dates of the course. (Arguably this should be computed here.)
    * @see
    *   [[AnnotatedTree]]
    */
  def addTemporalRules(dates: Map[EdgePath, Instant]) = tree.mapped[TemporalRule] { content =>
    TemporalRule(dates.get(content.edgePath))
  }

  def getTemporalRules(contentList: List[CourseContent], dates: Map[EdgePath, Instant]): Map[EdgePath, TemporalRule] =
    contentList.map { c =>
      c.edgePath -> TemporalRule(dates.get(c.edgePath))
    }.toMap

  /** Map over a content tree, adding [[Status]] es based on the contained gates.
    *
    * This is separate from [[addTemporalRules]] to allow computation of the gates without reference to a specific time.
    */
  def evaluateTemporalRules(now: Instant) = tree.mappedGiven[TemporalRule, Status] { (content, gate) =>
    Status(gate.evaluate(now))
  }

  def evaluateRules(temporalRules: Map[EdgePath, TemporalRule], now: Instant): Map[EdgePath, Status] =
    temporalRules.transform { (_, gate) =>
      Status(gate.evaluate(now))
    }
end TemporalRule
