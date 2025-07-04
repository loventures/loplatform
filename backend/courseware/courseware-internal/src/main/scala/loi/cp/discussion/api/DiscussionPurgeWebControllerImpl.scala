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

package loi.cp.discussion.api

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSource}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.GuidUtil
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.discussion.DiscussionBoardService
import loi.cp.discussion.persistence.DiscussionPurgeDao

import java.util.Date
import scala.util.{Success, Try}

@Component
class DiscussionPurgeWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  discussionPurgeDao: DiscussionPurgeDao,
  currentUser: => UserDTO,
)(implicit cs: ComponentSource)
    extends DiscussionPurgeWebController
    with ComponentImplementation:

  override def dryRunPurgePosts(
    sectionId: ContextId,
    body: PurgeRequestBody
  ): Try[Map[String, PurgeApiResponse]] =
    doPurge(sectionId, body, true)

  override def purgePosts(section: ContextId, body: PurgeRequestBody): Try[Map[String, PurgeApiResponse]] =
    doPurge(section, body, false)

  private def doPurge(
    sectionId: ContextId,
    body: PurgeRequestBody,
    dry: Boolean
  ): Try[Map[String, PurgeApiResponse]] =

    val section     = courseWebUtils.sectionOrThrow404(sectionId.id)
    val discussions = DiscussionBoardService.filterDiscussionContent(section.contents.nonRootElements)

    val titles = discussions.map(d => d.edgePath.toString -> d.title).toMap

    val postIds = discussionPurgeDao.purgeablePostIds(sectionId.id, body.date, body.timezoneOffset)

    val purgeCounts = for
      (edgePath, ids) <- postIds
      title           <- titles.get(edgePath)
    yield edgePath -> PurgeApiResponse(ids.size, title)

    if !dry then
      val delGuid = generateDeleteGuid()

      discussionPurgeDao.purgePostIds(postIds.values.flatten.toList, delGuid)

      Success(purgeCounts.map { case (edge, resp) => edge -> resp.copy(delGuid = Some(delGuid)) })
    else Success(purgeCounts)
  end doPurge

  private def generateDeleteGuid() = s"${GuidUtil.temporalGuid(new Date())}/${currentUser.id}"
end DiscussionPurgeWebControllerImpl
