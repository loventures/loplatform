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

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{BaseCondition, Comparison, Direction, QueryService}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import loi.authoring.project.{AccessRestriction, CommitDao2}
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.workspace.{AttachedReadWorkspace, WorkspaceService}
import loi.cp.course.right.TeachCourseRight
import loi.cp.right.RightService
import scalaz.std.list.*
import scaloi.misc.TimeSource
import scaloi.syntax.boxes.*
import scaloi.syntax.collection.*

import java.lang
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
class FeedbackServiceImpl(implicit
  fs: FacadeService,
  is: ItemService,
  qs: QueryService,
  rightService: RightService,
  ontology: Ontology,
  workspaceService: WorkspaceService,
  commitDao2: CommitDao2,
  now: => TimeSource,
  user: => UserDTO,
  domain: => DomainDTO,
) extends FeedbackService:
  import FeedbackServiceImpl.*

  override def addFeedback(
    project: Long,
    branch: Long,
    assetName: UUID,
    contentName: Option[UUID],
    lessonName: Option[UUID],
    moduleName: Option[UUID],
    unitName: Option[UUID],
    identifier: Option[String],
    sectionId: Option[Long],
    quote: Option[String],
    comment: String,
    uploads: List[UploadInfo],
    assigneeId: Option[Long],
  ): AssetFeedbackFinder =
    val section  = sectionId.map(_.finder[GroupFinder])
    val role     = guessRole(section)
    // TODO: validate assignee?
    val assignee = assigneeId.map(_.finder[UserFinder])

    val ws      = workspaceService.requireReadWorkspace(branch, AccessRestriction.none, cache = true)
    val remotes = getTransitiveRemotes(ws, assetName)

    val folder      = projectFeedbackFolder
    val attachments = uploads.map(upload => folder.addAttachment(upload).getId)
    folder.addChild[AssetFeedbackFinder] { feedback =>
      feedback.project = project
      feedback.branch = branch
      feedback.remotes = remotes.boxInsideTo[Array]()
      feedback.assetName = assetName
      feedback.contentName = contentName.orNull
      feedback.lessonName = lessonName.orNull
      feedback.moduleName = moduleName.orNull
      feedback.unitName = unitName.orNull
      feedback.identifier = identifier.orNull
      feedback.section = section.orNull
      feedback.status = null
      feedback.assignee = assignee.orNull
      feedback.created = now.date
      feedback.modified = now.date
      feedback.creator = user.finder[UserFinder]
      feedback.role = role.entryName
      feedback.quote = quote.orNull
      feedback.feedback = comment
      feedback.attachments = attachments.toArray
      feedback.closed = false
      feedback.replies = 0
      feedback.archived = false
    }
  end addFeedback

  private def getTransitiveRemotes(ws: AttachedReadWorkspace, assetName: UUID): List[Long] =
    // find all transitive dependencies that use the asset
    def loop(commitId: Long): List[Long] =
      val depDeps = for
        commit <- commitDao2.loadWithInitializedDocs(commitId).toList
        layer  <- commit.comboDoc.findLayerN(assetName)
      yield layer.projectId :: loop(layer.commitId)
      depDeps.flatten
    loop(ws.commitId)

  override def getFeedback(branch: Long, user: Long, contentName: Option[UUID]): Seq[AssetFeedbackFinder] =
    val qb = domain
      .queryAll[AssetFeedbackFinder]
      .addCondition(AssetFeedbackFinder.Branch, Comparison.eq, branch)
      .addDisjunction0(
        List(
          BaseCondition.getInstance(AssetFeedbackFinder.Creator, Comparison.eq, user),
          BaseCondition.getInstance(AssetFeedbackFinder.Assignee, Comparison.eq, user),
        ).asJava
      )
      .addCondition(AssetFeedbackFinder.Archived, Comparison.eq, false)
      .setOrder(AssetFeedbackFinder.Created, Direction.DESC)
    contentName.foreach(name => qb.addCondition(AssetFeedbackFinder.ContentName, Comparison.eq, name.toString))
    qb.getFinders[AssetFeedbackFinder]
  end getFeedback

  override def getReplies(ids: Seq[Long]): List[FeedbackActivityFinder] = ids ?? {
    val qb = domain
      .queryAll[FeedbackActivityFinder]
      .addCondition(FeedbackActivityFinder.Feedback, Comparison.in, ids)
      .addCondition(FeedbackActivityFinder.Event, Comparison.eq, FeedbackEvent.Reply.entryName)
      .setOrder(FeedbackActivityFinder.Created, Direction.ASC)
    qb.getFinders[FeedbackActivityFinder].toList
  }

  override def addReply(
    feedback: AssetFeedbackFinder,
    reply: String,
    attachments: List[lang.Long]
  ): FeedbackActivityFinder =
    feedback.modified = now.date
    feedback.replies = 1 + feedback.replies
    projectFeedbackFolder.addChild[FeedbackActivityFinder] { activity =>
      activity.feedback = feedback
      activity.created = now.date
      activity.creator = user.finder[UserFinder]
      activity.event = FeedbackEvent.Reply.entryName
      activity.value = reply.asJson
      activity.attachments = attachments.toArray
    }
  end addReply

  override def transition(
    feedback: AssetFeedbackFinder,
    status: Option[String],
    closed: Boolean,
  ): Unit =
    if status.orNull != feedback.status || closed != feedback.closed then
      projectFeedbackFolder.addChild[FeedbackActivityFinder] { activity =>
        activity.feedback = feedback
        activity.created = now.date
        activity.creator = user.finder[UserFinder]
        activity.event = FeedbackEvent.Status.entryName
        activity.value = status.asJson
        activity.attachments = Array()
        feedback.modified = now.date
        feedback.status = status.orNull
        feedback.closed = closed
      }

  override def assign(
    feedback: AssetFeedbackFinder,
    assignee: Option[lang.Long],
  ): Unit =
    if assignee != Option(feedback.assignee).map(_.id) then
      projectFeedbackFolder.addChild[FeedbackActivityFinder] { activity =>
        activity.feedback = feedback
        activity.created = now.date
        activity.creator = user.finder[UserFinder]
        activity.event = FeedbackEvent.Assign.entryName
        activity.value = assignee.asJson
        activity.attachments = Array()
        feedback.modified = now.date
        feedback.assignee = assignee.map(_.finder[UserFinder]).orNull
      }

  override def archiveBranch(branch: Long): Unit =
    // there will only be a couple of hundred so do it the entity way..
    domain
      .queryAll[AssetFeedbackFinder]
      .addCondition(AssetFeedbackFinder.Branch, Comparison.eq, branch)
      .addCondition(AssetFeedbackFinder.Archived, Comparison.eq, false)
      .getFinders[AssetFeedbackFinder] foreach { feedback =>
      feedback.archived = true
    }

  // horrible heuristic, we should have roles...
  private def guessRole(section: Option[GroupFinder]): FeedbackRole =
    if rightService.getUserHasRight(classOf[AccessAuthoringAppRight]) then FeedbackRole.Author
    else
      section match
        case Some(group) =>
          if group.xtype == GroupType.PreviewSection.name then
            // Authors work in preview sections
            FeedbackRole.Author
          else if group.xtype == GroupType.TestSection.name then
            // SMEs work in test sections as learners and instructors
            FeedbackRole.SME
          else if rightService.getUserHasRight(group, classOf[TeachCourseRight]) then FeedbackRole.Instructor
          else FeedbackRole.Student
        case None        =>
          FeedbackRole.Unknown // ultimately this is not a thing
end FeedbackServiceImpl

object FeedbackServiceImpl:
  def projectFeedbackFolder(implicit fs: FacadeService): AssetFeedbackFolderFacade =
    AssetFeedbackFolderFacade.Identifier.facade[AssetFeedbackFolderFacade]
