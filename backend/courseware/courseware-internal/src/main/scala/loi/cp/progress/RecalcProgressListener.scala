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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.EntityContext
import loi.cp.appevent.{AppEvent, OnEvent, OnEventBinding, OnEventComponent}
import loi.cp.course.CourseSectionService
import loi.cp.lwgrade.GradeService
import loi.cp.progress.store.ProgressDao

/** Upon updating the content of an offering, update the progress documents for the learners of its sections
  */
@Component
@OnEventBinding(Array(classOf[RecalcProgressAppEvent]))
class RecalcProgressListener(
  ci: ComponentInstance,
  courseSectionService: CourseSectionService,
  progressDao: ProgressDao,
  progressService: LightweightProgressService,
  gradeService: GradeService,
) extends BaseComponent(ci)
    with OnEventComponent:

  @OnEvent
  def onRecalc(ev: RecalcProgressAppEvent): Unit =
    for
      section    <- courseSectionService.getCourseSection(ev.sectionId).toList
      generation <- section.generation.toList
      userId     <- progressDao.loadStaleUserProgressUserIds(ev.sectionId, generation)
    yield
      val user = UserId(userId)
      progressService
        .updateProgress(section, user, gradeService.getGradebook(section, user), Nil)
        .valueOr(err =>
          // this should never happen since we pass Nil changes
          throw new RuntimeException(
            s"Unable to update progress for user $userId in section ${ev.sectionId}: ${err.msg}"
          )
        )
      EntityContext.flushClearAndCommit()
end RecalcProgressListener

case class RecalcProgressAppEvent(sectionId: Long) extends AppEvent
