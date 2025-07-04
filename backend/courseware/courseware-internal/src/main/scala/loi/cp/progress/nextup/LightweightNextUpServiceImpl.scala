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
package nextup

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.course.model.Course
import loi.asset.module.model.Module
import loi.cp.content.{ContentTree, CourseContent}
import loi.cp.course.CourseSection
import loi.cp.lwgrade.StudentGradebook
import loi.cp.reference.EdgePath
import scalaz.syntax.functor.*

import scala.jdk.CollectionConverters.*

@Service
class LightweightNextUpServiceImpl(
  progressService: LightweightProgressService,
) extends LightweightNextUpService:

  override def nextUpContent(
    section: CourseSection,
    user: UserDTO,
    gradebook: StudentGradebook
  ): Option[CourseContent] =
    val progress = progressService.loadProgress(section, user, gradebook)
    LightweightNextUpServiceImpl.nextUpContent(section.contents.tree, progress.map.asScala.toMap)
end LightweightNextUpServiceImpl

object LightweightNextUpServiceImpl:
  import report.*

  import PartialFunction.*

  private def nextUpContent(content: ContentTree, progress: Map[EdgePath, Progress]) =
    def progressForPath(c: CourseContent) = progress.get(c.edgePath)
    (content fproduct progressForPath).foldTree[Option[CourseContent]] { (here, children) =>
      val (content, progressOpt) = here

      if !disqualifies(content) && progressOpt.forall(notStarted) then Some(content)
      else children.collectFirst { case Some(p) => p }
    }

  /** Is `cc` disqualified _a priori_ from being next-up content? */
  private def disqualifies(cc: CourseContent): Boolean = cond(cc.asset) {
    case Course.Asset(_) => true
    case Module.Asset(_) => true
  }

  private def notStarted(p: Progress): Boolean =
    p.completions == 0
end LightweightNextUpServiceImpl
