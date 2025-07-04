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

package loi.cp.mastery

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.mastery.store.MasteryRuleStateFinder

@Service
class LegacyMasteryService(queryService: QueryService):

  def getLearnerMasteryData(learner: UserId): Seq[LearnerMasteryData] =
    queryService
      .queryRoot(MasteryRuleStateFinder.ITEM_TYPE_MASTERY_RULE_STATE)
      .addCondition(MasteryRuleStateFinder.DATA_TYPE_RULE_STATE_LEARNER, Comparison.eq, learner.id)
      .getFinders[MasteryRuleStateFinder]
      .map(toMasteryData)

  private def toMasteryData(state: MasteryRuleStateFinder): LearnerMasteryData =
    LearnerMasteryData(state.competency_id, state.masteryLevel == "Mastered")
end LegacyMasteryService

final case class LearnerMasteryData(competency: Long, mastered: Boolean)
