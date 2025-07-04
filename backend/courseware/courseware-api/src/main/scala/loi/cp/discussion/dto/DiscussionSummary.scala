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

package loi.cp.discussion.dto

import java.time.Instant

/** Service object for discussion boards.
  */
sealed trait DiscussionSummary:
  val lastPostCreationDate: Option[Instant]
  val lastVisited: Option[Instant]
  val participantCount: Long
  val postCount: Long

case class GeneralDiscussionSummary(
  lastPostCreationDate: Option[Instant],
  lastVisited: Option[Instant],
  participantCount: Long,
  postCount: Long,
  newPostCount: Long
) extends DiscussionSummary

object GeneralDiscussionSummary:
  def empty: DiscussionSummary = GeneralDiscussionSummary(None, None, 0, 0, 0)

case class ReviewerDiscussionSummary(
  lastPostCreationDate: Option[Instant],
  lastVisited: Option[Instant],
  participantCount: Long,
  postCount: Long,
  unreadPostCount: Long,
  unrespondedThreadCount: Long
) extends DiscussionSummary
object ReviewerDiscussionSummary:
  def empty: DiscussionSummary = ReviewerDiscussionSummary(None, None, 0, 0, 0, 0)
