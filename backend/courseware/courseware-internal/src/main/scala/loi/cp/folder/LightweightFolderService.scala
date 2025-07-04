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

package loi.cp.folder

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.{FolderFinder, Item}
import loi.cp.domain.LightweightDomainService
import loi.cp.item.LightweightItemService
import scaloi.GetOrCreate

@Service
class LightweightFolderService(
  folderDao: FolderDao,
  lightweightDomainService: LightweightDomainService,
  lightweightItemService: LightweightItemService,
  domain: => DomainDTO
):
  def getFolder(id: Long): Option[Folder] =
    folderDao
      .findFolder(id)
      .map(entity => Folder(entity))

  def getFolder(name: String): Option[Folder] =
    getNamedFolderEntity(name).map(Folder(_))

  def createFolder(
    name: Option[String],
    url: Option[String],
    folderType: Option[String],
    parent: Item
  ): Folder =
    val entity: FolderFinder = createFolderEntity(name, url, folderType, parent)
    Folder(entity)

  def getOrCreateNamedDomainFolder(
    name: String,
    url: Option[String],
    folderType: Option[String],
  ): Folder =
    val entity: FolderFinder =
      GetOrCreate[FolderFinder](
        () => getNamedFolderEntity(name),
        () =>
          val domainItem: Item = lightweightItemService.getDomainItem(domain)
          createFolderEntity(Some(name), url, folderType, domainItem)
        ,
        () => lightweightDomainService.acquirePessimisticDomainLock(domain)
      ).result

    Folder(entity)
  end getOrCreateNamedDomainFolder

  def acquirePessimisticLock(folder: Folder) =
    val folderRef: FolderFinder = folderDao.getFolderReference(folder.id)
    folderDao.lock(folderRef)

  private def getNamedFolderEntity(name: String): Option[FolderFinder] =
    lightweightItemService
      .getNamedItem(name)
      .map(folderItem => folderDao.getFolderReference(folderItem.getId))

  private def createFolderEntity(
    name: Option[String],
    url: Option[String],
    folderType: Option[String],
    parent: Item
  ): FolderFinder =
    val entity: FolderFinder = folderDao.createFolder(folderType, parent)
    name.foreach(nameFolder(entity, _))
    url.foreach(bindUrl(parent, entity, _))

    entity
  end createFolderEntity

  private def nameFolder(folderFinder: FolderFinder, name: String): Unit =
    val itemRef: Item = lightweightItemService.getItemReference(folderFinder)
    lightweightItemService.setItemName(name, itemRef)

  private def bindUrl(parent: Item, folderFinder: FolderFinder, url: String): Unit =
    val folderItemRef: Item = lightweightItemService.getItemReference(folderFinder)
    val parentPath: String  = lightweightItemService.getUrl(parent.getId).getOrElse("")
    lightweightItemService.bindUniqueUrl(folderItemRef, parentPath, url, "folder")
end LightweightFolderService

case class Folder(id: Long, folderType: Option[String])

object Folder:
  def apply(entity: FolderFinder): Folder =
    Folder(entity.getId, Option(entity.xtype))
