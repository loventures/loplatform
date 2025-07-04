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
import de.tomcat.juli.LogMeta
import loi.cp.assessment.attempt.AssessmentAttempt
import loi.cp.course.lightweight.LightweightLogMeta

/** Utilities for logging common assessment meta information using [LogMeta].
  */
object AssessmentLogMeta:

  def attemptMetadata(attempt: AssessmentAttempt): Unit =
    LightweightLogMeta.context(attempt.contentId.contextId)
    LightweightLogMeta.edgePath(attempt.contentId.edgePath)
    LogMeta.put("attempt", attempt.id.value)
    LogMeta.put("subject", attempt.user.id)
