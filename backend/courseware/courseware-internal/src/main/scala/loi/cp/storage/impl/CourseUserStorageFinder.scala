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

package loi.cp.storage
package impl

import argonaut.Json
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.group.GroupFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

@Entity
@HCache(usage = READ_WRITE)
private[storage] class CourseUserStorageFinder extends LeafEntity:
  @ManyToOne(fetch = FetchType.LAZY)
  var course: GroupFinder = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var storage: Json = scala.compiletime.uninitialized
end CourseUserStorageFinder

private[storage] object CourseUserStorageFinder:
  final val ITEM_TYPE_COURSE_USER_STORAGE = "CourseUserStorage"
  final val DATA_TYPE_COURSE              = "CourseUserStorage.course"
  final val DATA_TYPE_COURSE_USER_STORAGE = "CourseUserStorage.storage"
  final val DATA_TYPE_USER_ID             = "CourseUserStorage.parent_id"
