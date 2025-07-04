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

import java.time.Instant
import java.util.{Date, UUID}

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.item.{FolderFinder, Item}
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.PersistenceIdFactory
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.folder.FolderDao
import loi.cp.item.LightweightItemService
import org.hibernate.{LockMode, Session}
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.GetOrCreate
import scaloi.syntax.option.*

/** A service to store and retrieve data for [[SectionGroup]] and [[Offering]].
  */
@Service
trait GroupDao:
  @VisibleForTesting
  def createOfferingFolder(): FolderFinder

  @VisibleForTesting
  def createSectionFolder(sectionType: SectionType): FolderFinder

  /** Creates a new section in the given folder.
    *
    * @param folder
    *   the folder to put the group in
    * @param folderType
    *   the type of folder this is (sections, offerings, etc)
    * @param offering
    *   the offering to provision from
    * @param componentId
    *   the implementation to use
    * @param groupId
    *   the unique id of the group
    * @param name
    *   the user facing name of the group
    * @param selfStudy
    *   whether the course is considered self study
    * @param subtenantId
    *   the id of the subtenant this group is under, if any
    * @param creator
    *   the user creating the course
    * @param createTime
    *   when the group is created
    * @param startDate
    *   the general start date for the course (enrollments can affect individual availability)
    * @param endDate
    *   the general date when users can no longer interact with the course
    * @param shutdownDate
    *   when everyone is locked out of the course
    * @return
    *   the newly created group for the section
    */
  def createSectionGroup(
    folder: FolderFinder,
    folderType: SectionType,
    offering: Offering,
    componentId: String,
    groupId: String,
    name: String,
    selfStudy: Boolean,
    subtenantId: Option[Long],
    creator: UserDTO,
    createTime: Instant,
    startDate: Option[Instant],
    endDate: Option[Instant],
    shutdownDate: Option[Instant],
  ): NonUniqueGroupId \/ SectionEntityView

  /** Returns the section entity with the given pk.
    *
    * @param id
    *   the pk of the section
    * @return
    *   the section entity, if it exists
    */
  def fetchSectionGroup(id: Long): Option[SectionEntityView]

  /** Returns the section with the given {{groupId}} under the given folder. A groupId must be unique under a folder.
    *
    * @param folder
    *   the folder to search under
    * @param groupId
    *   the groupId of the section
    * @return
    *   the section, if any exists, with the given groupId
    */
  def fetchSectionGroup(folder: FolderFinder, groupId: String): Option[SectionEntityView]

  /** Returns the unique offering for the given {{branch}} and {{root}}. If one does not exist, it will be created.
    *
    * @param branch
    *   the branch for the offering
    * @param root
    *   the content for the offering
    * @param creator
    *   the creator if this creates an offering
    * @param now
    *   the create date of the offering, if it does not already exist
    * @return
    *   the existing or created offering entity
    */
  def getOrCreateOffering(branch: Branch, root: Asset[Course], creator: UserDTO, now: Instant): OfferingEntityView

  /** Returns the offering with the given pk.
    *
    * @param id
    *   the pk of the offering
    * @return
    *   the offering entity, if it exists
    */
  def fetchOffering(id: Long): Option[OfferingEntityView]

  // This is assumed to be created by GroupFolderCreationTask in prod
  /** Returns the offerings folder for the domain.
    *
    * @param lock
    *   whether to lock the folder for updates
    * @return
    *   the offerings folder
    */
  def getOfferingFolder(lock: Boolean): FolderFinder

  /** Returns the folder for the given type of sections.
    *
    * @param sectionType
    *   the section type to find a folder for
    * @param lock
    *   whether to lock the folder for updates
    * @return
    *   the section folder of the given type
    */
  def getSectionFolder(sectionType: SectionType, lock: Boolean): FolderFinder
end GroupDao

@Service
class GroupDaoImpl(
  lightweightItemService: LightweightItemService,
  folderDao: FolderDao,
  idFactory: PersistenceIdFactory,
  session: => Session,
  domain: => DomainDTO
) extends GroupDao:

  override def createOfferingFolder(): FolderFinder =
    val domainItem: Item     = lightweightItemService.getDomainItem(domain)
    val folder: FolderFinder = folderDao.createFolder(Some("group"), domainItem)
    lightweightItemService.setItemName(OfferingFolder.folderName, folder.owner())
    lightweightItemService.bindUniqueUrl(folder.owner(), "", OfferingFolder.url, "")

    folder

  override def createSectionFolder(typeName: SectionType): FolderFinder =
    val domainItem: Item     = lightweightItemService.getDomainItem(domain)
    val folder: FolderFinder = folderDao.createFolder(Some("group"), domainItem)
    lightweightItemService.setItemName(typeName.folderName, folder.owner())
    lightweightItemService.bindUniqueUrl(folder.owner(), "", typeName.url, "")

    folder

  override def createSectionGroup(
    folder: FolderFinder,
    folderType: SectionType,
    offering: Offering,
    componentId: String,
    groupId: String,
    name: String,
    selfStudy: Boolean,
    subtenantId: Option[Long],
    creator: UserDTO,
    createTime: Instant,
    startDate: Option[Instant],
    endDate: Option[Instant],
    shutdownDate: Option[Instant]
  ): NonUniqueGroupId \/ SectionEntityView =
    val folderItem: Item = lightweightItemService.getItemReference(folder)

    val domainItem: Item = lightweightItemService.getDomainItem(domain)

    val offeringEntity: GroupFinder = session.find[GroupFinder](classOf[GroupFinder], offering.id)

    val creatorEntity: UserFinder = session.find[UserFinder](classOf[UserFinder], creator.id)

    val subtenantEntity: Option[SubtenantFinder] =
      subtenantId.map(id => session.find[SubtenantFinder](classOf[SubtenantFinder], id))

    // Lock before attempting to determining uniqueness and creating any new groups
    lockFolder(folder)

    for _ <- validateUniqueGroupId(folder, groupId)
    yield
      val newId: Long       = idFactory.generateId()
      val sectionItem: Item =
        lightweightItemService.createItem(newId, SectionEntityView.itemType, folderItem, domainItem)

      val parentUrl: String = getGroupFolderUrl(folder)
      val url: String       = lightweightItemService.bindUniqueUrl(sectionItem, parentUrl, groupId, "course")

      val section =
        SectionEntityView(
          id = newId,
          componentId = componentId,
          name = name,
          groupId = groupId,
          creator = creatorEntity,
          createTime = Date.from(createTime),
          startDate = startDate.map(Date.from).orNull,
          endDate = endDate.map(Date.from).orNull,
          shutdownDate = shutdownDate.map(Date.from).orNull,
          subtenant = subtenantEntity.orNull,
          masterCourse = offeringEntity,
          xtype = folderType.name,
          project = offering.branch.project.map(p => Long.box(p.id)).orNull,
          branch = offering.branch.id,
          commit = offering.commitId,
          linkedAsset_id = offering.root.info.id,
          linkedAsset = offering.root.info.name.toString,
          generation = 0L,
          archived = false,
          disabled = false,
          selfStudy = selfStudy,
          url = url,
          path = sectionItem.path(),
          owner = sectionItem,
          parent = folderItem,
          root = domainItem
        )

      val entity: GroupFinder = section.toGroupFinder
      session.persist(entity)

      section
    end for
  end createSectionGroup

  private def validateUniqueGroupId(folder: FolderFinder, groupId: String): NonUniqueGroupId \/ Unit =
    // TODO: Eventually apply uniqueness across whole table?  It doesn't work that way right now, but it probably should
    if fetchSectionGroup(folder, groupId).nonEmpty then NonUniqueGroupId(groupId).left
    else ().right

  override def fetchSectionGroup(id: Long): Option[SectionEntityView] =
    lightweightItemService
      .findItem(id)
      .flatMap(validatedGroupItem)
      .map(_ =>
        val section: Option[SectionEntityView] =
          Option(session.find[GroupFinder](classOf[GroupFinder], id))
            .map(SectionEntityView.apply)

        // Violently explode if we somehow have a non-deleted item, but not a group itself for the id
        section.getOrElse(throw new IllegalStateException(s"Group entity does not exist for $id, but an item does."))
      )

  override def fetchSectionGroup(
    groupFolder: FolderFinder,
    groupId: String
  ): Option[SectionEntityView] =
    val domainItem: Item = lightweightItemService.getDomainItem(domain)
    Option(
      session
        .createQuery[GroupFinder](
          s"""
             | FROM ${classOf[GroupFinder].getName}
             | WHERE root = :domain
             | AND del is null
             | AND parent = :folder
             | AND groupId = :groupId
           """.stripMargin,
          classOf[GroupFinder]
        )
        .setParameter("domain", domainItem)
        .setParameter("folder", new Item(groupFolder))
        .setParameter("groupId", groupId)
        .uniqueResult()
    ).map(SectionEntityView.apply)
  end fetchSectionGroup

  override def getOrCreateOffering(
    branch: Branch,
    root: Asset[Course],
    creator: UserDTO,
    now: Instant
  ): OfferingEntityView =
    val offeringsFolder: FolderFinder = getOfferingFolder(lock = false)
    GetOrCreate[OfferingEntityView](
      () => queryActiveOfferings(branch, offeringsFolder),
      () => createOffering(branch, root, offeringsFolder, LightweightCourse.Identifier, creator, now),
      () => lockFolder(offeringsFolder)
    ).result
  end getOrCreateOffering

  private def queryActiveOfferings(
    branch: Branch,
    offeringsFolder: FolderFinder
  ): Option[OfferingEntityView] =
    Option(
      session
        .createQuery[GroupFinder](
          s"""
             | FROM  ${classOf[GroupFinder].getName}
             | WHERE del is null
             | AND branch = :branch
             | AND parent = :parent
             | AND archived = false
           """.stripMargin,
          classOf[GroupFinder]
        )
        .setParameter("branch", branch.id)
        .setParameter("parent", new Item(offeringsFolder))
        .uniqueResult()
    ).map(OfferingEntityView.apply)

  private def createOffering(
    branch: Branch,
    root: Asset[Course],
    offeringsFolder: FolderFinder,
    componentId: String,
    creator: UserDTO,
    createTime: Instant
  ): OfferingEntityView =
    val folderItem: Item = lightweightItemService.getItemReference(offeringsFolder)

    val domainItem: Item =
      lightweightItemService.getDomainItem(domain)

    val creatorEntity: UserFinder = session.find[UserFinder](classOf[UserFinder], creator.id)

    val newId: Long        = idFactory.generateId()
    val offeringItem: Item =
      lightweightItemService.createItem(newId, OfferingEntityView.itemType, folderItem, domainItem)

    val groupId: String = UUID.randomUUID().toString

    val parentPath: String = getGroupFolderUrl(offeringsFolder)
    val url                = lightweightItemService.bindUniqueUrl(offeringItem, parentPath, groupId, "course")

    val offering =
      OfferingEntityView(
        id = newId,
        componentId = componentId,
        name = root.data.title,
        groupId = groupId,
        creator = creatorEntity,
        createTime = Date.from(createTime),
        xtype = OfferingFolder.typeName,
        project = branch.project.map(p => Long.box(p.id)).orNull,
        branch = branch.id,
        commit = branch.head.id,
        linkedAsset_id = root.info.id,
        linkedAsset = root.info.name.toString,
        generation = 0L,
        archived = false,
        disabled = false,
        url = url,
        path = offeringItem.path(),
        owner = offeringItem,
        parent = folderItem,
        root = domainItem
      )

    val entity: GroupFinder = offering.toGroupFinder
    session.persist(entity)

    offering
  end createOffering

  override def fetchOffering(id: Long): Option[OfferingEntityView] =
    lightweightItemService
      .findItem(id)
      .flatMap(validatedGroupItem)
      .map(_ =>
        val entity: Option[OfferingEntityView] =
          Option(session.find[GroupFinder](classOf[GroupFinder], id))
            .map(OfferingEntityView.apply)

        // Violently explode if we somehow have a non-deleted item, but not a group itself for the id
        entity.getOrElse(throw new IllegalStateException("Group entity does not exist for $id, but an item does."))
      )

  override def getOfferingFolder(lock: Boolean): FolderFinder =
    val folderEntity: FolderFinder =
      fetchFolderByName(OfferingFolder.folderName)
        .getOrElse(
          throw new IllegalStateException(s"Cannot find folder for ${OfferingFolder.folderName} in domain ${domain.id}")
        )

    if lock then lockFolder(folderEntity)

    folderEntity
  end getOfferingFolder

  override def getSectionFolder(sectionType: SectionType, lock: Boolean): FolderFinder =
    val folderEntity: FolderFinder =
      fetchFolderByName(sectionType.folderName)
        .getOrElse(throw new IllegalStateException(s"Cannot find folder for $sectionType in domain ${domain.id}"))

    if lock then lockFolder(folderEntity)

    folderEntity

  private def fetchFolderByName(folderName: String): Option[FolderFinder] =
    // TODO: This can be cached
    for
      folderItem   <- lightweightItemService.getNamedItem(folderName)
      folderEntity <- Option(session.find(classOf[FolderFinder], folderItem.getId))
    yield folderEntity

  private def lockFolder(folderEntity: FolderFinder): Unit =
    session.lock(folderEntity, LockMode.PESSIMISTIC_WRITE)

  private def getGroupFolderUrl(groupFolder: FolderFinder): String =
    lightweightItemService
      .getUrl(groupFolder.id())
      .getOrElse(throw new IllegalStateException(s"No URL for folder ${groupFolder.id}"))

  /** Returns the given item if it is for a group. Otherwise it returns [[None]]
    *
    * @param item
    *   the item to validated
    * @return
    *   the item to validate
    */
  private def validatedGroupItem(item: Item): Option[Item] =
    Option(item).when(item.getItemType == SectionEntityView.itemType || item.getItemType == OfferingEntityView.itemType)
end GroupDaoImpl
