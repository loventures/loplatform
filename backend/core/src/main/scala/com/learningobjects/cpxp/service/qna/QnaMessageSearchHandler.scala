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

package com.learningobjects.cpxp.service.qna
import com.learningobjects.cpxp.component.query.{ApiFilter, PredicateOperator}
import com.learningobjects.cpxp.service.query.{BaseCondition, Comparison, Projection, QueryBuilder}
import com.learningobjects.de.web.QueryHandler
import scaloi.syntax.any.*

import scala.jdk.CollectionConverters.*

/** Implements filtering on message text; for example, filter=messages:ts(Squirrel).
  */
class QnaMessageSearchHandler extends QueryHandler:
  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit =
    if filter.getOperator != PredicateOperator.TEXT_SEARCH then filter.unsupported()
    val section = qb.getConditions.asScala.find(_.getDataType == QnaQuestionFinder.Section)
    qb.forceNative()
      .createInitialQuery()
      .setItemType(QnaMessageFinder.ItemType)
      .setIncludeDeleted(true)
      .addConjunction(section.map(_.copy <| (_.setDataType(QnaMessageFinder.Section))))
      .addCondition(BaseCondition.getInstance(QnaMessageFinder.Search, Comparison.search, filter.getValue))
      .setProjection(Projection.PARENT_ID)
  end applyFilter
end QnaMessageSearchHandler
