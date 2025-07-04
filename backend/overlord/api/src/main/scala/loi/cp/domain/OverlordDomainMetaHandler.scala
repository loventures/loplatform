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

package loi.cp.domain

import com.learningobjects.cpxp.component.query.{ApiFilter, PredicateOperator}
import com.learningobjects.cpxp.service.domain.DomainConstants.{
  DATA_TYPE_DOMAIN_HOST_NAME,
  DATA_TYPE_DOMAIN_ID,
  DATA_TYPE_DOMAIN_SHORT_NAME,
  DATA_TYPE_NAME
}
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.util.StringUtils
import com.learningobjects.de.web.QueryHandler

import scala.jdk.CollectionConverters.*

class OverlordDomainMetaHandler extends QueryHandler:
  final val searchable =
    List(DATA_TYPE_DOMAIN_ID, DATA_TYPE_NAME, DATA_TYPE_DOMAIN_SHORT_NAME, DATA_TYPE_DOMAIN_HOST_NAME)

  override def applyFilter(qb: QueryBuilder, filter: ApiFilter): Unit =
    if filter.getOperator != PredicateOperator.TEXT_SEARCH then filter.unsupported()
    // this would be inefficient over thousands of domains...
    val conditions = searchable map { dt =>
      filter.getValue.split("\\s+").filter(_.nonEmpty).map(StringUtils.escapeSqlLike) map { str =>
        BaseCondition.getInstance(dt, Comparison.like, s"%$str%", Function.LOWER)
      }
    }
    qb.addDisjunction(conditions.map(_.toList.asJava).asJava)
end OverlordDomainMetaHandler
