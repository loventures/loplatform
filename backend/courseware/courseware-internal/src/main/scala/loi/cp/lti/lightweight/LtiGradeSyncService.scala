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

package loi.cp.lti.lightweight
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.Widen
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.lti.CourseColumnIntegrations
import loi.cp.lti.lightweight.LtiGradeSyncService.SyncError
import loi.cp.lwgrade.{Grade, GradeColumn, SingleGradeHistory}
import loi.cp.reference.EdgePath
import scalaz.\/

@Service
trait LtiGradeSyncService:

  /** Grades
    */
  def syncGrade(
    course: ContextId,
    userId: UserId,
    edgePath: EdgePath,
    grade: Grade,
  ): SingleGradeHistory
  def syncAllGradesForColumn(section: CourseSection, edgePath: EdgePath): Map[UserId, SingleGradeHistory]

  /** Columns
    */
  def syncColumn(course: CourseSection, edgePath: EdgePath): SyncError \/ (GradeColumn, CourseColumnIntegrations)
  def getColumnHistory(course: CourseSection, edgePath: EdgePath): SyncError \/ (GradeColumn, CourseColumnIntegrations)
  def syncAllColumnsAndGradesForCourse(
    course: CourseSection
  ): SyncError \/ Unit // List[(GradeColumn, CourseColumnIntegrations)]

  /** Legacy
    */
  def syncOutcomes1Grade(userId: UserId, course: ContextId, ep: EdgePath, grade: Option[Grade]): Unit

  def syncAgsGrade(
    userId: UserId,
    course: ContextId,
    ep: EdgePath,
    maybeGrade: Option[Grade],
  ): Unit
end LtiGradeSyncService

object LtiGradeSyncService:
  sealed trait SyncError extends Widen[SyncError]

  case class Exceptional(message: String)            extends SyncError
  case class NoCourse(courseId: Long)                extends SyncError
  case class NoColumn(ep: EdgePath)                  extends SyncError
  case class NoSyncHistory(courseId: Long)           extends SyncError
  case class ColumnAlreadySynced(edgePath: EdgePath) extends SyncError
