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

package loi.cp.progress
package store

import argonaut.Json
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.entity.LeafEntity
import com.learningobjects.cpxp.entity.annotation.DataType
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.user.{UserFacade, UserFinder}
import jakarta.persistence.{Column, Entity, ManyToOne}
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import scaloi.GetOrCreate

import java.time.Instant
import java.util.Date
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class UserProgressFinder extends LeafEntity:
  import UserProgressFinder.*

  @ManyToOne(optional = false)
  @DataType(DATA_TYPE_USER_PROGRESS_COURSE)
  var course: GroupFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_USER_PROGRESS_GENERATION)
  var generation: jl.Long = 0L

  @Column(nullable = false, columnDefinition = "JSONB")
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var progressMap: Json = scala.compiletime.uninitialized

  @Column
  var lastModified: Date = scala.compiletime.uninitialized
end UserProgressFinder

object UserProgressFinder:
  final val ITEM_TYPE_USER_PROGRESS               = "UserProgress"
  final val DATA_TYPE_USER_PROGRESS_COURSE        = "UserProgress.course"
  final val DATA_TYPE_USER_PROGRESS_MAP           = "UserProgress.progressMap"
  final val DATA_TYPE_USER_PROGRESS_LAST_MODIFIED = "UserProgress.lastModified"
  final val DATA_TYPE_USER_PROGRESS_GENERATION    = "UserProgress.generation"

@FacadeItem(UserProgressFinder.ITEM_TYPE_USER_PROGRESS)
trait UserProgressFacade extends Facade:
  @FacadeParent
  def getStudent: UserFacade
  def setStudent(id: Long): Unit

  @FacadeData
  def getGeneration: Option[Long]
  def setGeneration(gen: Long): Unit

  @FacadeData
  def getProgressMap: Json
  def setProgressMap(json: Json): Unit

  @FacadeData
  def getLastModified: Option[Instant]
  def setLastModified(lm: Option[Instant]): Unit

  def refresh(pessi: Boolean): Unit
end UserProgressFacade

@FacadeItem(UserFinder.ITEM_TYPE_USER)
trait UserProgressParentFacade extends UserFacade:
  @FacadeChild
  def getProgress(
    @FacadeCondition(UserProgressFinder.DATA_TYPE_USER_PROGRESS_COURSE)
    courseId: Long,
  ): Option[UserProgressFacade]
  def getOrCreateProgress(
    @FacadeCondition(UserProgressFinder.DATA_TYPE_USER_PROGRESS_COURSE)
    courseId: Long,
    init: UserProgressFacade => Unit
  ): GetOrCreate[UserProgressFacade]
  def refresh(pessi: Boolean): Unit
  def jefresh(): Unit = refresh(pessi = true)
end UserProgressParentFacade

@FacadeItem(GroupFinder.ITEM_TYPE_GROUP)
trait CourseProgressRelativeFacade extends Facade:
  @FacadeChild
  @FacadeQuery(domain = true)
  def getProgresses(
    @FacadeCondition(UserProgressFinder.DATA_TYPE_USER_PROGRESS_COURSE)
    courseId: Long,
  ): Seq[UserProgressFacade]
