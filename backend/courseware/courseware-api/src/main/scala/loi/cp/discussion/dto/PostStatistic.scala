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

package loi.cp.discussion.dto
import java.time.Instant

/** Container for a user-specfic view of a post. Will contain properties that are specific to the user that we are
  * generating this object for, such as if it has been explicitly marked read.
  */
trait PostStatistic:

  /** If None, there are no descendants to have activity on.
    */
  def descendantActivity: Option[Instant]

  /** If None, this value has not been calculated, otherwise it is the number of descendants of this post.
    */
  def descendantCount: Option[Long]

  /** If None, this value has not been calculated, otherwise it is the number of descendants of this post that were
    * created after some passed in time value.
    */
  def newDescendantCount: Option[Long]

  /** If None, this value has not been calculated, otherwise it is the number of descendants of this post that have not
    * been explicitly marked read.
    */
  def unreadDescendantCount: Option[Long]
end PostStatistic
