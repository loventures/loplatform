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

package loi.authoring.index.web

import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.operation.{Operations, VoidOperation}
import com.learningobjects.cpxp.service.presence.EventType
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.task.Priority
import com.learningobjects.de.authorization.Secured
import loi.authoring.index.IndexService
import loi.authoring.project.AccessRestriction
import loi.authoring.security.right.{AccessAuthoringAppRight, EditContentAnyProjectRight}
import loi.authoring.web.AuthoringWebUtils
import loi.cp.presence.PresenceService

import java.util.concurrent.ConcurrentHashMap

@Component
@Controller(root = true)
private[web] class IndexWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  indexService: IndexService,
  presenceService: PresenceService,
  user: UserDTO
) extends ApiRootComponent
    with ComponentImplementation:

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/branches/{id}/reindex", method = Method.POST)
  def reindex(@PathVariable("id") branchId: Long): Unit =
    val branch =
      webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])
    Operations.deferTransact(new IndexOperation(branch.id), Priority.Low, s"IndexWebController.reindex($branchId)")

  private final class IndexOperation(branchId: Long) extends VoidOperation:
    import IndexWebController.*

    override def execute(): Unit =
      if currentlyIndexing.putIfAbsent(branchId, user) eq null then
        try
          indexService.indexBranch(branchId, delete = true)
          presenceService.deliverToUsers(ReindexComplete(branchId))(user.id)
        finally currentlyIndexing.remove(branchId)
end IndexWebController

object IndexWebController:
  private val currentlyIndexing = new ConcurrentHashMap[Long, UserDTO] // control concurrency on at least this appserver

// This ought to be an algebra of authoring messages, but Jackson...
case class ReindexComplete(branch: Long)

object ReindexComplete:
  implicit val ReindexCompleteEventType: EventType[ReindexComplete] = EventType("ReindexComplete")
