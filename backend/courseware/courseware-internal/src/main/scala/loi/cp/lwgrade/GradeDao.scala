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

package loi.cp.lwgrade

import com.learningobjects.cpxp.component.annotation.Service as Dao
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import scalaz.NonEmptyList

/** A [[Dao]] for gradebooks and such.
  *
  * Does not know about [[StudentGradebook]]; use [[GradeService]] for that.
  */
@Dao
trait GradeDao:

  /** Loads or creates the grades for the user in the course. Does not compute rollup.
    *
    * Writes values to the DB in the create case.
    */
  def loadOrCreate(user: UserId, course: ContextId): RawGrades

  /** Loads the grades for all the `users`. Does not compute rollup. An empty grades value is returned for a user if a
    * grade row does not exist, but the empty values are not written to the DB.
    */
  def load(users: NonEmptyList[UserId], course: ContextId): Map[UserId, RawGrades]

  def save(user: UserId, course: ContextId, newGrades: RawGrades): Unit

  def loadByCourse(course: ContextId): Map[UserId, RawGrades]

  def loadUserIdsByCourse(course: ContextId): List[Long]

  def delete(user: UserId, course: ContextId): Unit

  /** Moves the grades for `userId` from `srcCourse` to `tgtCourse`. If there are somehow existing grades in
    * `tgtCourse`, we overwrite them. Then we `del` the `srcCourse` grades.
    */
  def transferGrades(user: UserId, srcCourse: ContextId, tgtCourse: ContextId): Unit
end GradeDao
