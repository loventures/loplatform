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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.EntityContext
import loi.cp.appevent.{AppEvent, OnEvent, OnEventBinding, OnEventComponent}
import loi.cp.course.CourseSectionService
import loi.cp.lti.LtiOutcomesService
import loi.cp.offering.PublishAnalysis.{CreateLineItem, DeleteLineItem, UpdateLineItem}

import java.time.Instant

@Component
@OnEventBinding(Array(classOf[RecalcGradesAppEvent]))
class RecalcGradesListener(
  ci: ComponentInstance,
  courseSectionService: CourseSectionService,
  gradeDao: GradeDao,
  gradeService: GradeService,
  ltiOutcomesService: LtiOutcomesService,
)(implicit itemService: ItemService)
    extends BaseComponent(ci)
    with OnEventComponent:

  @OnEvent
  def onRecalc(ev: RecalcGradesAppEvent): Unit =

    for
      section  <- courseSectionService.getCourseSection(ev.sectionId)
      structure = GradeStructure(section.contents)
    yield

      for
        create <- ev.creates
        column <- structure.findColumnForEdgePath(create.edgePath)
      do ltiOutcomesService.manuallySyncColumn(section, column)

      for
        update <- ev.updates
        column <- structure.findColumnForEdgePath(update.edgePath)
      do ltiOutcomesService.manuallySyncColumn(section, column)

      for delete <- ev.deletes
      do ltiOutcomesService.deleteColumn(section, delete.edgePath)

      if ev.updates.nonEmpty then

        for
          userId     <- gradeDao.loadUserIdsByCourse(section)
          userFinder <- userId.finder_?[UserFinder]
        do
          val userDto   = userFinder.loadDtoNoInit
          val gradebook = gradeService.getGradebook(section, userDto)
          for
            update    <- ev.updates
            content   <- section.contents.get(update.edgePath)
            column    <- structure.findColumnForEdgePath(update.edgePath)
            prevGrade <- gradebook.get(update.edgePath)
            percent   <- getPercent(prevGrade)
          do
            gradeService.setGradePercent(
              userDto,
              section,
              content,
              structure,
              column,
              percent,
              Instant.now,
            )
          end for

          EntityContext.flushClearAndCommit()
      end if

  private def getPercent(prevGrade: Grade): Option[Double] =
    for
      _       <- Grade.grade(prevGrade)    // discards exotic grades (e.g. Unassigned, Pending) ...
      percent <- Grade.fraction(prevGrade) // ... that Grade.fraction would return 0.0 for
    yield percent
end RecalcGradesListener

case class RecalcGradesAppEvent(
  sectionId: Long,
  creates: List[CreateLineItem],
  updates: List[UpdateLineItem],
  deletes: List[DeleteLineItem]
) extends AppEvent
