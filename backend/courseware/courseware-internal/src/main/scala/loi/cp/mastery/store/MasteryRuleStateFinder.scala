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

package loi.cp.mastery.store

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.Cache as HCache

import java.util.Date
import java.lang as jl

// This stays around for upgrade purposes
@Entity
@HCache(usage = READ_WRITE)
class MasteryRuleStateFinder extends LeafEntity:

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var competency_id: jl.Long = scala.compiletime.uninitialized

  @Column
  var lastEvaluatedTime: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var learner: UserFinder = scala.compiletime.uninitialized

  @Column
  var masteryLevel: String = scala.compiletime.uninitialized

  @Column
  var updateTime: Date = scala.compiletime.uninitialized
end MasteryRuleStateFinder

object MasteryRuleStateFinder:
  final val ITEM_TYPE_MASTERY_RULE_STATE             = "MasteryRuleState"
  final val DATA_TYPE_RULE_STATE_RULE                = "MasteryRuleState.rule"
  final val DATA_TYPE_RULE_STATE_COMPETENCY          = "MasteryRuleState.competency_id"
  final val DATA_TYPE_RULE_STATE_LAST_EVALUATED_TIME = "MasteryRuleState.lastEvaluatedTime"
  final val DATA_TYPE_RULE_STATE_UPDATE_TIME         = "MasteryRuleState.updateTime"
  final val DATA_TYPE_RULE_STATE_LEVEL               = "MasteryRuleState.masteryLevel"
  final val DATA_TYPE_RULE_STATE_LEARNER             = "MasteryRuleState.learner"
