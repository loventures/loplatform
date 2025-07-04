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

package com.learningobjects.cpxp.usage

import com.learningobjects.cpxp.entity.DomainEntity
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.util.Date

@Entity
@Table(
  indexes = Array(
    new Index(name = "domain_stats_idx", columnList = "time,statType")
  )
)
@HCache(usage = READ_WRITE)
class DomainStatsFinder extends DomainEntity:

  @Column(nullable = false)
  var statType: String = scala.compiletime.uninitialized

  @Column(nullable = false)
  var time: Date = scala.compiletime.uninitialized

  @Column(nullable = false)
  var value: java.lang.Long = scala.compiletime.uninitialized
end DomainStatsFinder

object DomainStatsFinder:
  final val ITEM_TYPE_DAILY_DOMAIN_STATISTICS = "DomainStats"

  final val DATA_TYPE_TYPE  = "DomainStats.statType"
  final val DATA_TYPE_TIME  = "DomainStats.time"
  final val DATA_TYPE_VALUE = "DomainStats.value"
