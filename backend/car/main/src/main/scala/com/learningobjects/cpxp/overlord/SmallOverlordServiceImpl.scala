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

package com.learningobjects.cpxp.overlord

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.query.QueryService

import scala.jdk.CollectionConverters.*

@Service
class SmallOverlordServiceImpl(
  queryService: QueryService
) extends SmallOverlordService:

  override def findOverlordDomainItem(): Option[Item] =
    queryService
      .queryAllDomains(DomainConstants.ITEM_TYPE_DOMAIN)
      .setCacheQuery(true)
      .setCacheNothing(false)
      .addCondition(DataTypes.DATA_TYPE_TYPE, "eq", DomainConstants.DOMAIN_TYPE_OVERLORD)
      .getItems()
      .asScala
      .headOption
end SmallOverlordServiceImpl
