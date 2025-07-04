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

package loi.cp.assessment
package attempt

import java.time.Instant
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.reference.{ContentIdentifier, EdgePath}

/** A user's attempt on any type of [[loi.cp.assessment.Assessment]].
  */
trait AssessmentAttempt extends Id with AssessmentAttemptProperties:
  val id: AttemptId = AttemptId(getId)
  def user: UserDTO

  def assessment: Assessment
  def contentId: ContentIdentifier = assessment.contentId
  def edgePath: EdgePath           = contentId.edgePath
  def contextId: Long              = contentId.contextId.value

  def createTime: Instant
  def updateTime: Instant
  def scoreTime: Option[Instant]
  def maxMinutes: Option[Long]
  def autoSubmitted: Boolean

  def score: Option[Score]
  def scorer: Option[Long]
end AssessmentAttempt

/** Captures a minimal set of properties used in some counting computations.
  */
trait AssessmentAttemptProperties:
  def id: AttemptId
  def state: AttemptState
  def valid: Boolean
  def submitTime: Option[Instant]
