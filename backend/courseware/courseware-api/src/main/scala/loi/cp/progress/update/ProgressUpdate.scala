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
package update

import java.lang.Long as JLong

import com.learningobjects.cpxp.service.presence.EventType
import loi.cp.progress.report.{Progress, ProgressReport}

/** A message object for when a user's proficiency in a context is updated.
  *
  * @param courseId
  *   the context where progress was made
  * @param systemId
  *   the system recording the progress
  * @param overallProgress
  *   the overall progress for the course
  * @param progressReport
  *   the updated progress
  */
case class ProgressUpdate(
  courseId: Long,
  systemId: Long,
  overallProgress: Progress,
  progressReport: ProgressReport
)

object ProgressUpdate:
  def apply(
    courseId: JLong,
    systemId: JLong,
    overallProgress: Progress,
    progressReport: ProgressReport
  ): ProgressUpdate =
    ProgressUpdate(courseId.longValue(), systemId.longValue(), overallProgress, progressReport)

  implicit val ProgressUpdateType: EventType[ProgressUpdate] = EventType("ProgressUpdate")
end ProgressUpdate
