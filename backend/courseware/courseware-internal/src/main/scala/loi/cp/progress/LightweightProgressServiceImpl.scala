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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.session.SessionDTO
import com.learningobjects.cpxp.service.user.UserId
import com.learningobjects.cpxp.util.{Ids, ThreadTerminator}
import loi.asset.discussion.model.Discussion1
import loi.cp.analytics.AnalyticsService
import loi.cp.analytics.event.{ProgressPutEvent, ProgressPutEvent2}
import loi.cp.appevent.AppEventService
import loi.cp.content.{CourseContent, CourseContents}
import loi.cp.context.ContextId
import loi.cp.course.{CourseConfigurationService, CoursePreferences, CourseSection}
import loi.cp.lwgrade.{Grade, GradeStructure, StudentGradebook}
import loi.cp.presence.PresenceService
import loi.cp.progress.ProgressChange.{Skipped, TestOut, Unvisit, Visited}
import loi.cp.progress.store.{ProgressDao, UserProgressEntity, UserProgressNode}
import loi.cp.reference.EdgePath
import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*
import scalaz.{Endo, \/}
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
class LightweightProgressServiceImpl(
  appEventService: AppEventService,
  courseConfigurationService: CourseConfigurationService,
  analyticsService: AnalyticsService,
  domainDto: => DomainDTO,
  sessionDto: => SessionDTO,
  now: TimeSource,
  dao: ProgressDao,
  presenceService: PresenceService
) extends LightweightProgressService:
  import LightweightProgressServiceImpl.*

  override def loadProgress(section: CourseSection, user: UserId, gradebook: StudentGradebook): ProgressMap =
    val progressEntity = dao.loadUserProgress(user.id, section.id, forUpdate = false)
    entity2Progress(section, gradebook, progressEntity)

  override def loadProgress(
    section: CourseSection,
    users: List[UserId],
    gradebooks: Map[UserId, StudentGradebook],
  ): Map[UserId, ProgressMap] =
    val progressEntities = dao.loadUserProgresses(users.map(_.value), section.id)

    users.view
      .map(user =>
        // this is only called by the progress web controller; in monster courses it runs way over the
        // 60s response timeoutz
        ThreadTerminator.check()
        val progressEntity = progressEntities.get(user)
        val gradebook      = gradebooks(user)
        user -> entity2Progress(section, gradebook, progressEntity)
      )
      .toMap
  end loadProgress

  override def loadVisitationBasedProgress(section: CourseSection, users: List[UserId]): Map[UserId, ProgressMap] =
    val defaultGradebook = StudentGradebook(Map.empty)(GradeStructure(section.contents))
    val gradebooks       = Map.empty[UserId, StudentGradebook].withDefaultValue(defaultGradebook)
    loadProgress(section, users, gradebooks)

  private def entity2Progress(
    section: CourseSection,
    gradebook: StudentGradebook,
    entity: Option[UserProgressEntity],
  ): ProgressMap =

    def fresh(entity: UserProgressEntity): Boolean =
      val sameGeneration          = section.generation =&= entity.generation
      lazy val allAlreadyUpgraded = section.contents.tree.flatten.forall(cc =>
        entity.map
          .get(cc.edgePath)
          .exists(node => node.forCreditGrades.isDefined && node.forCreditGradesPossible.isDefined)
      )
      sameGeneration && allAlreadyUpgraded

    val nodes = entity match
      case Some(entity) if fresh(entity) => entity.map
      case Some(entity)                  => rollUp(section, gradebook, entity.map)
      case _                             => rollUp(section, gradebook, Map.empty)

    toProgressMap(nodes, entity.flatMap(_.lastModified))
  end entity2Progress

  override def updateProgress(
    section: CourseSection,
    user: UserId,
    gradebook: StudentGradebook,
    changes: List[ProgressChange]
  ): NonLeafProgressRequest \/ ProgressMap =

    assertLeafPaths(section.contents, changes.map(_.path).toSet).map(_ =>
      val oldEntity = getOrInitEntity(user.id, section, gradebook)
      val change    = changes.foldMap(applyChange)
      val newNodes  = rollUp(section, gradebook, change(oldEntity.map))
      val newEntity = dao.saveUserProgress(
        userId = user.id,
        courseId = section.id,
        generation = section.generation,
        map = newNodes,
        lastModified = if changes.isEmpty then oldEntity.lastModified else Some(now.instant)
      )

      emitProgressPutEvent(section, user.id, oldEntity.map, newNodes)
      sendUpdate(section, user, newEntity)

      toProgressMap(newEntity)
    )

  private def emitProgressPutEvent(
    section: CourseSection,
    userId: Long,
    oldNodes: UserProgressNode.Map,
    newNodes: UserProgressNode.Map,
  ): Unit =
    // This one's for you analyticfinder. Taking the difference is _only_ to shrink the event json document.
    val changes = newNodes.toSet.diff(oldNodes.toSet).toList

    def getAssetId(edgePath: EdgePath): Option[Long] =
      if edgePath == EdgePath.Root then Some(section.asset.info.id)
      else section.contents.get(edgePath).map(_.asset.info.id)

    val pValues = for
      (edgePath, node) <- changes
      assetId          <- getAssetId(edgePath)
    yield
      // for removed content, assetId will be None, and no PValue2 yielded. This is consistent with the platform.
      // The progress for that removed content remains in postgres, and remains in redshift.
      // What will change are the ancestors: rollup will omit the removed descendant from summation.
      // And those ancestors will appear in `changes` and the contents still exist in `section.contents`
      ProgressPutEvent.PValue2(
        edgePath.toString,
        assetId,
        node.completions,
        node.total,
        node.incrementTypes.visited,
        node.incrementTypes.testedOut,
        node.incrementTypes.skipped,
        node.forCreditGrades,
        node.forCreditGradesPossible,
      )

    if pValues.nonEmpty then
      val event =
        ProgressPutEvent2(
          UUID.randomUUID(),
          now.date,
          domainDto.hostName,
          sessionDto.id,
          section.id,
          userId,
          pValues,
          None
        )

      analyticsService.emitEvent(event)
    end if
  end emitProgressPutEvent

  override def scheduleProgressUpdate(sectionId: Long): Unit =
    if courseConfigurationService.getGroupConfig(CoursePreferences, ContextId(sectionId)).eagerProgressRecalculation
    then appEventService.fireEvent(Ids.of(sectionId), RecalcProgressAppEvent(sectionId))

  private def toProgressMap(entity: UserProgressEntity): ProgressMap =
    toProgressMap(entity.map, entity.lastModified)

  private def toProgressMap(
    nodes: Map[EdgePath, UserProgressNode],
    lastModified: Option[Instant]
  ): ProgressMap =
    ProgressMap(nodes.view.mapValues(_.toWebProgress).toMap.asJava, lastModified)

  private def getOrInitEntity(userId: Long, section: CourseSection, gradebook: StudentGradebook): UserProgressEntity =
    dao.loadUserProgress(userId, section.id, forUpdate = true).getOrElse {
      UserProgressEntity(rollUp(section, gradebook, Map.empty), None, section.generation)
    }

  override def deleteProgress(
    section: CourseSection,
    user: UserId,
  ): Unit =
    dao.deleteUserProgress(user.id, section.id)

  override def transferProgress(
    srcSection: CourseSection,
    dstSection: CourseSection,
    user: UserId,
    gradebook: StudentGradebook
  ): Unit =

    def adjust(entity: UserProgressEntity): UserProgressEntity =
      entity.copy(
        map = rollUp(dstSection, gradebook, entity.map),
        generation = dstSection.generation,
      )

    // So what if there's no progress.
    dao.transferUserProgress(user.id, srcSection.id, dstSection.id, Endo(adjust))
  end transferProgress

  private def sendUpdate(
    section: CourseSection,
    user: UserId,
    newProgress: UserProgressEntity,
  ): Unit =
    val report  = newProgress.toWebProgress(user)
    val overall = report.getProgress(EdgePath.Root)
    val message = update.ProgressUpdate(section.id, section.id, overall, report)
    presenceService.deliverToUsers(message)(user.id)
end LightweightProgressServiceImpl

object LightweightProgressServiceImpl:

  private def rollUp(
    sec: CourseSection,
    gb: StudentGradebook,
    nodes: Map[EdgePath, UserProgressNode]
  ): Map[EdgePath, UserProgressNode] =

    val nodeTree = sec.contents.tree.scanr[(EdgePath, UserProgressNode)] {
      case (cc, _) if excluded(cc) => cc.edgePath -> UserProgressNode.Excluded
      case (cc, Nil)               =>
        // leaf
        lazy val fcg  = gb.get(cc.edgePath).exists(Grade.isGraded) ? 1 | 0
        lazy val fcgp = cc.isForCredit.contains(true) ? 1 | 0
        val leaf      = nodes.get(cc.edgePath).map(_.upgraded(fcg, fcgp)).getOrElse(UserProgressNode.emptyLeaf(fcg, fcgp))
        cc.edgePath -> leaf
      case (cc, children)          => cc.edgePath -> children.map(_.rootLabel._2).fold1Opt.get
    }
    // keep all data in mp (that aren't rolled up) so that progress on hidden
    // content is retained (in case it's later reinstated)
    nodes ++ nodeTree.rflatten.toMap
  end rollUp

  private def excluded(content: CourseContent): Boolean = content.asset.is[Discussion1]

  private def assertLeafPaths(cc: CourseContents, paths: Set[EdgePath]): NonLeafProgressRequest \/ Unit =
    val violations = cc.tree.foldLeftN(Set.empty[EdgePath]) {
      case (cc, children, acc) if children.nonEmpty && paths.contains(cc.edgePath) => acc + cc.edgePath
      case (_, _, acc)                                                             => acc
    }

    violations.nonEmpty.thenLeft(NonLeafProgressRequest(violations))

  private def applyChange(change: ProgressChange): Endo[Map[EdgePath, UserProgressNode]] = Endo { progressNodes =>
    progressNodes
      .get(change.path)
      .filter(_.total == 1)
      .map(node =>
        val UserProgressNode(_, _, types, fcg, fcgp) = node

        val newNode = change match
          case Visited(_) => UserProgressNode(1, 1, types.copy(visited = 1), fcg, fcgp)
          case TestOut(_) => UserProgressNode(1, 1, types.copy(testedOut = 1), fcg, fcgp)
          case Skipped(_) => UserProgressNode(1, 1, types.copy(skipped = 1), fcg, fcgp)
          case Unvisit(_) =>
            val completions = (types `contains` IncrementType.TESTEDOUT) ? 1 | 0
            UserProgressNode(completions, 1, types - IncrementType.VISITED - IncrementType.SKIPPED, fcg, fcgp)

        progressNodes.updated(change.path, newNode)
      )
      .getOrElse(progressNodes)
  }
end LightweightProgressServiceImpl
