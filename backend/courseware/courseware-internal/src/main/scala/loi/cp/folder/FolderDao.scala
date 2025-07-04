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
import com.learningobjects.cpxp.util.PersistenceIdFactory
import loi.cp.domain.LightweightDomainService
import loi.cp.item.LightweightItemService
import org.hibernate.{LockMode, Session}

@Service
trait FolderDao:
  def createFolder(folderType: Option[String], parent: Item): FolderFinder

  def findFolder(id: Long): Option[FolderFinder]

  def getFolderReference(id: Long): FolderFinder

  def lock(folderRef: FolderFinder): Unit

@Service
class FolderDaoImpl(
  idFactory: PersistenceIdFactory,
  lightweightDomainService: LightweightDomainService,
  lightweightItemService: LightweightItemService,
  session: => Session,
  domain: => DomainDTO
) extends FolderDao:
  def createFolder(
    folderType: Option[String],
    parent: Item,
  ): FolderFinder =
    // See GroupFolderCreationTask
    val domainItem: Item = lightweightItemService.getDomainItem(domain)

    val newId: Long      = idFactory.generateId()
    val folderItem: Item =
      lightweightItemService.createItem(newId, FolderFinder.ITEM_TYPE_FOLDER, domainItem, domainItem)

    val entity: FolderFinder = new FolderFinder
    entity.setId(newId)
    folderType.foreach(typ => entity.xtype = typ)

    entity.setPath(folderItem.path())
    entity.setOwner(folderItem)
    entity.setParent(parent)
    entity.setRoot(domainItem)

    session.persist(entity)
    session.flush()

    entity
  end createFolder

  def findFolder(id: Long): Option[FolderFinder] =
    Option(session.find(classOf[FolderFinder], id))

  def getFolderReference(id: Long): FolderFinder =
    session.getReference(classOf[FolderFinder], id)

  def lock(folderRef: FolderFinder): Unit =
    session.lock(folderRef, LockMode.PESSIMISTIC_WRITE)
end FolderDaoImpl
