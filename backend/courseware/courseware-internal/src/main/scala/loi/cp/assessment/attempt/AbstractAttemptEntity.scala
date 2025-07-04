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

package loi.cp.assessment.attempt

import java.util.Date

import com.learningobjects.cpxp.entity.annotation.DataType
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import jakarta.persistence.Column
import loi.cp.reference.AssetReference

/** An entity that represents a user attempt. This contains the reference to the content it was created from and the
  * subject of the attempt.
  */
trait AbstractAttemptEntity:

  /** the column containing the node name for the authored content (for querying by unversioned reference) */
  @Column(nullable = false)
  @DataType(AssetReference.DATA_TYPE_NODE_NAME)
  var nodeName: String = scala.compiletime.uninitialized

  /** the column containing the versioned content graph id */
  @Column(nullable = false)
  @DataType(AssetReference.DATA_TYPE_COMMIT_ID)
  var commitId: JLong = scala.compiletime.uninitialized

  /** the column containing the reference to a context */
  @DataType(AssetReference.DATA_TYPE_CONTEXT_ID)
  var contextId: JLong = scala.compiletime.uninitialized

  /** the column containing the reference to the location of the content in the context */
  @DataType(AssetReference.DATA_TYPE_EDGE_PATH)
  var edgePath: String = scala.compiletime.uninitialized

  /** a column containing the id of the subject (the driver of the attempt may be a different user) */
  @Column(nullable = false)
  var userId: JLong = scala.compiletime.uninitialized
end AbstractAttemptEntity

trait AttemptyWempty[T]:
  def updateTimeSql: String
  def valid(t: T): Boolean
  def attemptState(t: T): String
  def submitTime(t: T): Date
  def updateTime(t: T): Date

object AttemptyWempty:
  def apply[T](implicit T: AttemptyWempty[T]): AttemptyWempty[T] = T
