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

import loi.cp.lwgrade.StudentGradebook
import loi.cp.reference.EdgePath
import scalaz.syntax.order.*
import scaloi.syntax.OptionOps.*

import java.time.Instant

/** A summary of collected gates on a tree.
  *
  * @param temporal
  *   the [[TemporalRule]] found in the tree
  * @param performance
  *   the [[PerformanceRule]] found in the tree
  * @param overridden
  *   whether the gate has been overridden
  * @param policy
  *   whether the content is afflicted with the [[PolicyRule]], if such a rule exists.
  * @param status
  *   the overall gate status
  */
final case class GateSummary(
  temporal: TemporalRule,
  performance: PerformanceRule,
  policy: Option[PolicyType],
  overridden: Boolean,
  status: GateStatus,
):

  /** Whether the annotated content has no gating data whatsoever.
    *
    * This means that all types of gates are unused, and the instructor has not overridden the gate.
    */
  // noinspection EmptyCheck
  def isEmpty = !overridden &&
    temporal == TemporalRule.none &&
    performance == PerformanceRule.none &&
    policy == None

  /** Whether this content can be readed by the user. */
  def isReadable = status <= GateStatus.ReadOnly || overridden
end GateSummary

object GateSummary:
  final val empty = GateSummary(
    temporal = TemporalRule.none,
    performance = PerformanceRule.none,
    policy = None,
    overridden = false,
    status = GateStatus.Open,
  )

  final case class CollectedGateData(
    temporal: TemporalRule,
    temporalStatus: TemporalRule.Status,
    performance: PerformanceRule,
    performanceStatus: PerformanceRule.Status,
    policyStatus: Option[PolicyRule.Status],
  )

  /** Walk through a tree, collecting the [[GateSummary]] as you go.
    *
    * The tree must have the data that are enumerated in [[CollectedGateData]].
    */
  // should overrides be collected in tree instead?
  def collectGateSummary(isInstructor: Boolean, isOverridden: EdgePath => Boolean) =
    tree.combine[CollectedGateData, GateSummary] { (content, collected) =>
      val overridden = isOverridden(content.edgePath)
      val status     =
        if isInstructor || overridden then GateStatus.Open
        // arbitrarily deciding that the business rules have max priority
        else
          List(
            collected.temporalStatus.value,
            collected.performanceStatus.value,
          ).max // select most restrictive
      GateSummary(
        temporal = collected.temporal,
        performance = collected.performance,
        policy = collected.policyStatus.filter(_.value != GateStatus.Open).map(_.policyType),
        overridden = overridden,
        status = collected.policyStatus.map(_.value) `max` status,
      )
    }

  def calculateGateData(
    contentList: List[CourseContent],
    gateDates: Map[EdgePath, Instant],
    perfStructure: PerformanceRule.Structure,
    policyStatus: Map[EdgePath, PolicyRule.Status],
    time: Instant,
    gradebook: StudentGradebook
  ): Map[EdgePath, CollectedGateData] =

    val temporalRules     = TemporalRule.getTemporalRules(contentList, gateDates)
    val temporalStatus    = TemporalRule.evaluateRules(temporalRules, time)
    val performanceRules  = PerformanceRule.getPerformanceRules(contentList, perfStructure)
    val performanceStatus = PerformanceRule.evaluateRules(performanceRules, gradebook)

    // These defaults may never be used - this structure was a weakness of circumventing shapeless
    contentList.map { cc =>
      cc.edgePath -> CollectedGateData(
        temporalRules.getOrElse(cc.edgePath, TemporalRule.none),
        temporalStatus.getOrElse(cc.edgePath, TemporalRule.Status(GateStatus.Open)),
        performanceRules.getOrElse(cc.edgePath, PerformanceRule.none),
        performanceStatus.getOrElse(cc.edgePath, PerformanceRule.Status(GateStatus.Open)),
        policyStatus.get(cc.edgePath)
      )
    }.toMap
  end calculateGateData

  def collectGateSummary(
    collectedGateMap: Map[EdgePath, CollectedGateData],
    isInstructor: Boolean,
    isOverridden: EdgePath => Boolean
  ): Map[EdgePath, GateSummary] =
    collectedGateMap.map { case (edgePath, collected) =>
      val overridden = isOverridden(edgePath)
      val status     =
        if isInstructor || overridden then GateStatus.Open
        // arbitrarily deciding that the business rules have max priority
        else
          List(
            collected.temporalStatus.value,
            collected.performanceStatus.value,
          ).max // select most restrictive
      edgePath -> GateSummary(
        temporal = collected.temporal,
        performance = collected.performance,
        policy = collected.policyStatus.filter(_.value != GateStatus.Open).map(_.policyType),
        overridden = overridden,
        status = collected.policyStatus.map(_.value) `max` status,
      )
    }
end GateSummary
