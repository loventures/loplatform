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

package loi.cp.discussion

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.discussion.model.Discussion1
import loi.authoring.render.LtiLinkRenderer
import loi.cp.content.{CourseContent, courseAssetInstructionsUrl}
import loi.cp.course.lightweight.Lwc
import loi.cp.instructions.{BlockInstructions, Instructions}

@Service
class DiscussionInstructionService:

  def getInstructions(
    course: Lwc,
    discussion: CourseContent,
  ): Option[Instructions] =

    discussion.asset match
      case Discussion1.Asset(d1) =>
        Option(d1.data.instructions)
          .map(LtiLinkRenderer.rewriteContentPart(_, courseAssetInstructionsUrl(course, discussion)))
          .map(BlockInstructions.apply)
end DiscussionInstructionService
