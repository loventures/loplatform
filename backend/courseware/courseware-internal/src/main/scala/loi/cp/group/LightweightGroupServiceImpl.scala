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

package loi.cp.group

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.item.FolderFinder
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.workspace.service.ReadWorkspaceService
import scalaz.\/
import scaloi.misc.TimeSource
import scaloi.syntax.option.*

import java.time.Instant
import java.util.UUID

@Service
class LightweightGroupServiceImpl(
  groupDao: GroupDao,
  workspaceService: ReadWorkspaceService,
  assetNodeService: AssetNodeService,
  projectService: ProjectService,
  timeSource: TimeSource
) extends LightweightGroupService:
  @VisibleForTesting
  def createOfferingFolder(): OfferingFolder =
    val entity: FolderFinder = groupDao.createOfferingFolder()
    OfferingFolder(entity.id())

  @VisibleForTesting
  def getOfferingFolder: OfferingFolder =
    val folder: FolderFinder = groupDao.getOfferingFolder(lock = false)
    OfferingFolder(folder.id())

  @VisibleForTesting
  def createSectionFolder(): SectionFolder =
    val entity: FolderFinder = groupDao.createSectionFolder(SectionType.Sections)
    SectionFolder(entity.id(), SectionType.Sections)

  @VisibleForTesting
  def getSectionFolder(sectionType: SectionType): SectionFolder =
    val entity: FolderFinder = groupDao.getSectionFolder(sectionType, lock = false)
    SectionFolder(entity.id(), sectionType)

  override def fetchSectionGroup(id: Long): Option[SectionGroup] =
    val possibleEntity: Option[SectionEntityView] = groupDao.fetchSectionGroup(id)
    possibleEntity
      .filter(e => SectionGroupImplementation.values.map(_.componentId).contains(e.componentId)) // only allow LWC impls
      .map(entity =>
        val branch = projectService
          .loadBronch(entity.branch, AccessRestriction.none)
          .getOrElse(throw new IllegalStateException(s"Missing branch ${entity.branch} for group ${entity.id}"))

        val (commitId, root) =
          if entity.isPreview then
            // use the head commit of the branch for preview section
            val workspace = workspaceService.requireReadWorkspace(branch.id, AccessRestriction.none)
            val root      =
              assetNodeService
                .loadA[Course](workspace)
                .byName(UUID.fromString(entity.linkedAsset))
                .getOrElse(throw new IllegalStateException(s"Cannot load course asset ${entity.linkedAsset}"))

            (branch.head.id, root)
          else
            val commitId = Option(entity.commit)
              .map(_.toLong)
              .getOrElse(throw new IllegalStateException(s"Missing commit ${entity.commit} for group ${entity.id}"))

            val workspace =
              workspaceService.requireReadWorkspaceAtCommit(branch.id, commitId, AccessRestriction.none)

            val root =
              assetNodeService
                .loadA[Course](workspace)
                .byName(UUID.fromString(entity.linkedAsset))
                .getOrElse(
                  throw new IllegalStateException(
                    s"Missing root content ${entity.linkedAsset_id} for group ${entity.id}"
                  )
                )

            (commitId, root)

        buildSection(entity, root, branch, commitId)
      )
  end fetchSectionGroup

  override def createSectionGroup(
    offering: Offering,
    implementation: SectionGroupImplementation,
    groupId: String,
    name: String,
    rollingEnrollment: Boolean,
    subtenantId: Option[Long],
    creator: UserDTO,
    startDate: Option[Instant],
    endDate: Option[Instant],
    shutdownDate: Option[Instant]
  ): NonUniqueGroupId \/ SectionGroup =
    val folder: FolderFinder = groupDao.getSectionFolder(SectionType.Sections, lock = true)
    groupDao
      .createSectionGroup(
        folder,
        SectionType.Sections,
        offering,
        implementation.componentId,
        groupId,
        name,
        rollingEnrollment,
        subtenantId,
        creator,
        timeSource.instant,
        startDate,
        endDate,
        shutdownDate
      )
      .map(entity => buildSection(entity, offering.root, offering.branch, offering.commitId))
  end createSectionGroup

  override def fetchOffering(id: Long): Option[Offering] =
    val possibleEntity: Option[OfferingEntityView] = groupDao.fetchOffering(id)
    possibleEntity.map(entity =>
      val branch = projectService
        .loadBronch(entity.branch, AccessRestriction.none)
        .getOrElse(throw new IllegalStateException(s"Missing branch ${entity.branch} for group ${entity.id}"))

      val commitId = Option(entity.commit)
        .map(_.toLong)
        .getOrElse(throw new IllegalStateException(s"Missing commit ${entity.commit} for group ${entity.id}"))

      val workspace = workspaceService.requireReadWorkspaceAtCommit(branch.id, commitId, AccessRestriction.none)

      val root =
        assetNodeService
          .loadA[Course](workspace)
          .byName(UUID.fromString(entity.linkedAsset))
          .getOrElse(
            throw new IllegalStateException(s"Missing root content ${entity.linkedAsset_id} for group ${entity.id}")
          )

      Offering(entity.id, branch, commitId, root, entity.archived)
    )
  end fetchOffering

  override def getOrCreateOffering(branch: Branch, root: Asset[Course], creator: UserDTO): Offering =
    val entity: OfferingEntityView =
      groupDao.getOrCreateOffering(branch, root, creator, timeSource.instant)
    Offering(entity.id, branch, branch.head.id, root, entity.archived)

  private def buildSection(
    entity: SectionEntityView,
    root: Asset[Course],
    branch: Branch,
    commitId: Long
  ): SectionGroup =
    // Only have generations for non-preview courses.  If you have a generation, things will get cached.  If you don't
    // things don't get cached.  Caching is good for regular sections.  Caching is bad if an author wants to preview a
    // change and get cached content.
    val generation: Option[Long] =
      Option(entity.generation).map(_.longValue()).when(!entity.selfStudy)

    val offeringId = Option(entity.masterCourse).map(_.getId.longValue)

    SectionGroup(
      entity.id,
      entity.groupId,
      None,
      rollingEnrollment = entity.selfStudy,
      Option(entity.startDate).map(_.toInstant),
      entity.createTime.toInstant,
      Option(entity.endDate).map(_.toInstant),
      Option(entity.shutdownDate).map(_.toInstant),
      root,
      branch,
      commitId,
      offeringId,
      generation,
      GroupType.forName(entity.xtype)
    )
  end buildSection
end LightweightGroupServiceImpl
