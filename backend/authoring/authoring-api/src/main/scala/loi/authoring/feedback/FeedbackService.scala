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

package loi.authoring.feedback

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}

import java.lang
import java.util.UUID

@Service
trait FeedbackService:
  def addFeedback(
    project: Long,
    branch: Long,
    assetName: UUID,
    contentName: Option[UUID],
    lessonName: Option[UUID],
    moduleName: Option[UUID],
    unitName: Option[UUID],
    identifier: Option[String],
    section: Option[Long],
    quote: Option[String],
    feedback: String,
    attachments: List[UploadInfo],
    assignee: Option[Long],
  ): AssetFeedbackFinder

  def getFeedback(
    branch: Long,
    user: Long,
    contentName: Option[UUID],
  ): Seq[AssetFeedbackFinder]

  def getReplies(
    ids: Seq[Long]
  ): List[FeedbackActivityFinder]

  def addReply(
    feedback: AssetFeedbackFinder,
    reply: String,
    attachments: List[lang.Long],
  ): FeedbackActivityFinder

  def transition(
    feedback: AssetFeedbackFinder,
    status: Option[String],
    closed: Boolean,
  ): Unit

  def assign(
    feedback: AssetFeedbackFinder,
    assignee: Option[lang.Long],
  ): Unit

  def archiveBranch(branch: Long): Unit
end FeedbackService
