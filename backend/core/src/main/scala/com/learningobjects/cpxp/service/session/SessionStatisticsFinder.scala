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

package com.learningobjects.cpxp.service.session

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class SessionStatisticsFinder extends DomainEntity:

  @Column
  var count: jl.Long = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var date: String = scala.compiletime.uninitialized

  @Column
  var duration: jl.Long = scala.compiletime.uninitialized
end SessionStatisticsFinder

object SessionStatisticsFinder:
  final val ITEM_TYPE_SESSION_STATISTICS          = "SessionStatistics"
  final val DATA_TYPE_SESSION_STATISTICS_COUNT    = "SessionStatistics.count"
  final val DATA_TYPE_SESSION_STATISTICS_DATE     = "SessionStatistics.date"
  final val DATA_TYPE_SESSION_STATISTICS_DURATION = "SessionStatistics.duration"
