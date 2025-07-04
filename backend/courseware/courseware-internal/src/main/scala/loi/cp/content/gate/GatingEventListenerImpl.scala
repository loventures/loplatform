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

package loi.cp.content
package gate

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId as CourseId
import loi.cp.course.{CourseSectionService, CourseWorkspaceService}
import loi.cp.lwgrade.StudentGradebook
import loi.cp.notification.NotificationService
import scalaz.*
import scalaz.syntax.all.*
import scaloi.data.ListTree
import scaloi.syntax.putty.*
import scaloi.syntax.`try`.*
import scaloi.syntax.option.*

@Service
private[gate] class GatingEventListenerImpl(
  courseSectionService: CourseSectionService,
  courseWorkspaceService: CourseWorkspaceService,
  notifications: NotificationService,
  structureService: PerformanceRuleStructureService,
) extends GatingEventListener:

  override def onGradeChange(
    usr: UserId,
    crs: CourseId,
    oldGb: StudentGradebook,
    newGb: StudentGradebook,
  ): Unit =
    val attempt = for
      section <- courseSectionService
                   .getCourseSection(crs.value)
                   .toTry(new IllegalArgumentException(s"No such context ${crs.value}"))
      ws       = courseWorkspaceService.loadReadWorkspace(section)
      st      <- structureService.computePerformanceRuleStructure(ws, section.lwc)
    yield
      val gatedContents = PerformanceRule.addPerformanceRules(st)(section.contents.tree.annotate)

      val oldGates = PerformanceRule.evaluateRules(oldGb)(gatedContents)
      val newGates = PerformanceRule.evaluateRules(newGb)(gatedContents)

      val newlyOpenContent = Zip[ListTree]
        .zipWith(oldGates, newGates) { (auld, neue) =>
          val auldStatus = auld._1.select[PerformanceRule.Status].value
          val neueStatus = neue._1.select[PerformanceRule.Status].value
          // less than == less restrictive...
          if neueStatus < auldStatus then Some(neue._2) else None
        }
        .flattern

      newlyOpenContent.foreach(dispatch(_, usr, crs))
    attempt.orThrow
  end onGradeChange

  def dispatch(content: CourseContent, usr: UserId, crs: CourseId): Unit =
    val init = ContentGateNotification.Init(crs, usr, content)
    notifications.nοtify[ContentGateNotification](usr, init)
end GatingEventListenerImpl
