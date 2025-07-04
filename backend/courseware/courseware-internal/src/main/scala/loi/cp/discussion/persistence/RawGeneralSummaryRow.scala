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

package loi.cp.discussion.persistence

import java.util.Date

import com.learningobjects.cpxp.scala.util.JTypes.JLong
import loi.cp.context.ContextId
import loi.cp.discussion.dto.{DiscussionSummary, ReviewerDiscussionSummary}
import loi.cp.reference.{ContentIdentifier, EdgePath}

case class RawReviewerSummaryRow(
  contextId: JLong,
  edgePathString: String,
  latestUpdateTime: Date,
  lastVisited: Date,
  userCount: JLong,
  postCount: JLong,
  unreadPostCount: JLong,
  unrespondedThreadCount: JLong
):

  def toReviewerSummaryTuple: (ContentIdentifier, DiscussionSummary) =
    ContentIdentifier(ContextId(contextId), EdgePath.parse(edgePathString)) -> ReviewerDiscussionSummary(
      Option(latestUpdateTime).map(_.toInstant),
      Option(lastVisited).map(_.toInstant),
      userCount,
      postCount,
      unreadPostCount,
      unrespondedThreadCount
    )
end RawReviewerSummaryRow
