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

import java.io.IOException

import argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId

/** Support for storaging course section data. */
@Service
trait CourseStorageService:

  /** Get the course storage for a given course section. May fail due to unparseable data.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def get[T: CourseStoreable](course: ContextId): T

  /** Gets or creates, locks, refreshes, and modificates the course storage for a given course section. May fail due to
    * unparseable data or under contention.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def modify[T: CourseStoreable](course: ContextId)(mod: T => T): T

  /** Gets or creates, locks, refreshes, and sets the course storage for a given course section. May fail due to
    * unparseable data or under contention.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def set[T: CourseStoreable](course: ContextId, t: T): Unit = modify(course)((_: T) => t)

  def reset(course: ContextId, user: UserId): Unit

  /** Get the course user storage for a given course section and user. May fail due to unparseable data.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def get[T: CourseStoreable](course: ContextId, user: UserId): T

  /** Get the course user storage for a given course section and users. May fail due to unparseable data.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def get[T: CourseStoreable](course: ContextId, users: List[UserId]): Map[Long, T]

  /** Gets or creates, locks, refreshes, and modificates the course user storage for a given course section and user.
    * May fail due to unparseable data or under contention.
    */
  @throws[UnstoragingError]("if the data are ill-formed")
  def modify[T: CourseStoreable](course: ContextId, user: UserId)(mod: T => T): T
end CourseStorageService

final case class UnstoragingError(
  what: String,
  why: String,
  where: Long,
  who: Option[Long],
  how: CursorHistory,
) extends IOException({
      val whereStr = where.toString + who.map(user => s" (user $user)").getOrElse("")
      println(s"Constructing unstoraging error!!!!!!!!!! $why")
      s"error decoding storaged data '$what' in '$whereStr' because $why: $how"
    })

trait CourseStoreable[T]:
  val key: String
  val codec: CodecJson[T]
  val empty: T

object CourseStoreable:
  def apply[T: EncodeJson: DecodeJson](key0: String)(empty0: T): CourseStoreable[T] =
    new CourseStoreable[T]:
      override val key   = key0
      override val codec = CodecJson.derived
      override val empty = empty0
