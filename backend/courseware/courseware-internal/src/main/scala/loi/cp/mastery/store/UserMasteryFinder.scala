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

import argonaut.Json
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.group.GroupFinder
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import jakarta.persistence.*

@Entity
@HCache(usage = READ_WRITE)
class UserMasteryFinder extends LeafEntity:
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  var course: GroupFinder = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB", nullable = false)
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var state: Json = scala.compiletime.uninitialized
end UserMasteryFinder

private[mastery] object UserMasteryFinder:
  final val ITEM_TYPE_USER_MASTERY        = "UserMastery"
  final val DATA_TYPE_USER_MASTERY_COURSE = "UserMastery.course"
  final val DATA_TYPE_USER_MASTERY_STATE  = "UserMastery.state"
