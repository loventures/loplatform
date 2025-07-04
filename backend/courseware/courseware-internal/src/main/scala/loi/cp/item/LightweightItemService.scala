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

package loi.cp.item

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.entity.{LeafEntity, PeerEntity}
import com.learningobjects.cpxp.service.BasicServiceBean
import com.learningobjects.cpxp.service.data.Data
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainDTO}
import com.learningobjects.cpxp.service.item.Item
import loi.cp.path.Path
import org.hibernate.Session

import scala.jdk.CollectionConverters.*

/** An intentionally abbreviated form of [[com.learningobjects.cpxp.service.item.ItemService]] to shunt places where
  * interaction with the [[Item]] hierarchy is needed for interoperability with existing administration of entities.
  * This class should only be used for interoperability for with entities that are managed by heavyweight methods. Any
  * usage of this class should be deprecated and migrated away from when heavyweight is no longer actively being used.
  */
@Service
trait LightweightItemService:

  /** Creates an [[Item]] for a peered entity (see [[com.learningobjects.cpxp.entity.PeerEntity]]). This method takes
    * shortcuts that are only appropriate for lightweight code paths and should not be widely used.
    *
    * @param id
    *   the id of the peered entity, and thus the returned Item
    * @param itemType
    *   the type of the item as would be dictated by the component environment
    * @param parent
    *   the parent of the entity in the item tree
    * @param domainItem
    *   the peered item for the domain entity
    * @return
    *   the create [[Item]]
    */
  def createItem(id: Long, itemType: String, parent: Item, domainItem: Item): Item

  /** Returns a non-deleted item for the given id. Non-existent items also will be mapped to [[None]].
    *
    * @param id
    *   the id of the peered entity/item
    * @return
    *   the item for the given id, or [[None]] if deleted or non-existent
    */
  def findItem(id: Long): Option[Item]

  /** Returns the item for the given entity as a reference. This is invalid if the entity is deleted.
    *
    * @param entity
    *   the entity to retrieve an item reference for
    * @return
    *   a proxy for the [[Item]] with the given id
    */
  def getItemReference(entity: PeerEntity): Item

  /** Returns the item for the given domain. It is an illegal state if the item for the domain is deleted or
    * non-existent.
    *
    * @param domain
    *   the domain service item
    * @return
    *   the item for the domain
    */
  final def getDomainItem(domain: DomainDTO): Item =
    findItem(domain.id).getOrElse(throw new IllegalStateException(s"No Domain for ${domain.id}"))

  /** Returns the bound URL for the item with the given id.
    *
    * @param id
    *   the id of the item to find the URL for
    * @return
    *   the url for the item, if it exists
    */
  def getUrl(id: Long): Option[String]

  /** Creates a unique url entry for the given {{item}} under a given parent path. This method will make efforts to
    * deduplicate the calculated URL for the input.
    *
    * @param item
    *   the item to bind
    * @param parentUrl
    *   the url of the parent item
    * @param name
    *   the name to bind under
    * @param alternate
    *   an alternate name if the first given name cannot be encoded into a url
    * @return
    *   the url that the item was bound under
    */
  def bindUniqueUrl(item: Item, parentUrl: String, name: String, alternate: String): String

  /** Gets the unique name of an item, if a unique name is bound for the item.
    *
    * @param id
    *   the id of the item
    * @return
    *   the unique name of the item, if it exists
    */
  def getName(id: Long): Option[String]

  /** Returns an item for a given unique name in the system (such as 'folder-courses'). This name is often referred to
    * as 'id' despite the implication of being a PK.
    *
    * @param name
    *   the name of the item to fetch (often referred to as 'id')
    * @return
    *   the item, if it exists
    */
  def getNamedItem(name: String): Option[Item] =
    getNamedItems(Seq(name)).get(name)

  /** Returns items by a unique names in the system (such as 'folder-courses').
    *
    * @param names
    *   the names of the items to fetch
    * @return
    *   the items mapped by name, if they exist
    */
  def getNamedItems(names: Seq[String]): Map[String, Item]

  /** Binds a unique name to the given item. If an item already exists with the given name in the domain, then that is
    * an illegal argument.
    *
    * @param name
    *   the name to bind under
    * @param item
    *   the item to map by the name
    */
  def setItemName(name: String, item: Item): Unit
end LightweightItemService

object LightweightItemService:

  /** A utility for setting [[Item]] properties on a given peer {{entity}}.
    *
    * @param entity
    *   the entity to set properties on
    * @param entityItem
    *   the item that is peered with the entity
    * @param parentItem
    *   the parent item of the item (potentially the domain)
    * @param domainItem
    *   the item for the domain
    */
  def setItemTreeProperties(entity: PeerEntity, entityItem: Item, parentItem: Item, domainItem: Item): Unit =
    entity.setOwner(entityItem)
    entity.setPath(entityItem.path())
    entity.setParent(parentItem)
    entity.setRoot(domainItem)

  /** A utility for setting [[Item]] properties on a given leaf {{entity}}.
    *
    * @param entity
    *   the entity to set properties on
    * @param parentItem
    *   the parent item of the item (potentially the domain)
    * @param domainItem
    *   the item for the domain
    */
  def setItemTreeProperties(entity: LeafEntity, parentItem: Item, domainItem: Item): Unit =
    val parentPath = new Path(parentItem.path())
    entity.setPath(parentPath.append(entity.id().toString).toString)
    entity.setParent(parentItem)
    entity.setRoot(domainItem)
end LightweightItemService

@Service
class LightweightItemServiceImpl(session: => Session, domain: => DomainDTO) extends LightweightItemService:
  @VisibleForTesting
  def createDomainItem(id: Long): Item =
    val domainItem = new Item()
    domainItem.setId(id)
    domainItem.setType(DomainConstants.ITEM_TYPE_DOMAIN)
    domainItem.setRoot(domainItem)

    session.persist(domainItem)
    domainItem

  override def createItem(
    id: Long,
    itemType: String,
    parent: Item,
    domainItem: Item
  ): Item =
    val itemEntity = new Item()
    itemEntity.setId(id)
    itemEntity.setType(itemType)
    itemEntity.setRoot(domainItem)

    itemEntity.setParent(parent)

    val parentPath: String = parent.path()
    itemEntity.setPath(parentPath + id + "/")

    session.persist(itemEntity)

    itemEntity
  end createItem

  override def findItem(id: Long): Option[Item] =
    Option(session.find[Item](classOf[Item], id)).filter(_.getDeleted == null)

  override def getItemReference(entity: PeerEntity): Item =
    Option(entity)
      .filter(_.getDel == null)
      .map(nonDeletedEntity => session.getReference(classOf[Item], nonDeletedEntity.getId))
      .getOrElse(throw new IllegalArgumentException(s"Cannot get item of deleted entity ${entity.id()}."))

  override def getUrl(id: Long): Option[String] =
    getUrlData(id).map(_.getString)

  override def bindUniqueUrl(item: Item, parentUrl: String, name: String, alternate: String): String =
    val baseUrl: String =
      if name != "/" then BasicServiceBean.getBindingPattern(parentUrl, name, alternate).stripSuffix("$_%$")
      else
        // Binding domain root
        "/"

    val domainItem: Item = getDomainItem(domain)

    val data: Data = getUrlData(item.getId).getOrElse(newUrlData(item, domainItem))

    val uniqueUrl: String =
      if !urlExists(baseUrl) then baseUrl
      else
        val index: Int =
          (1 to 10000)
            .find(idx => !urlExists(baseUrl + idx))
            .getOrElse(throw new RuntimeException(s"Cannot find suitable index to make $baseUrl a unique URL."))

        baseUrl + index

    data.setString(uniqueUrl)

    session.persist(data)

    uniqueUrl
  end bindUniqueUrl

  private def getUrlData(id: Long): Option[Data] =
    session
      .createQuery[Data](
        s"""
           | FROM ${classOf[Data].getName}
           | WHERE root.id = :domain
           | AND type = 'url'
           | AND owner.id = :owner
         """.stripMargin,
        classOf[Data]
      )
      .setParameter("domain", domain.id)
      .setParameter("owner", id)
      .getResultList
      .asScala
      .headOption

  private def newUrlData(item: Item, domainItem: Item): Data =
    val urlData = new Data

    urlData.setType("url")
    urlData.setOwner(item)
    urlData.setRoot(domainItem)

    urlData

  private def urlExists(url: String): Boolean =
    session
      .createQuery[Data](
        s"""
           | FROM ${classOf[Data].getName}
           | WHERE root.id = :domain
           | AND type = 'url'
           | AND string = :url
         """.stripMargin,
        classOf[Data]
      )
      .setParameter("domain", domain.id)
      .setParameter("url", url)
      .getResultList
      .asScala
      .nonEmpty

  override def getName(id: Long): Option[String] =
    Option(
      session
        .createQuery[Data](
          s"""
             | FROM ${classOf[Data].getName}
             | WHERE root.id = :domain
             | AND type = 'id'
             | AND owner.id = :id
           """.stripMargin,
          classOf[Data]
        )
        .setParameter("domain", domain.id)
        .setParameter("id", Long.box(id))
        .uniqueResult()
    ).map(_.getString)

  override def getNamedItems(names: Seq[String]): Map[String, Item] =
    val entries: Seq[Data] =
      session
        .createQuery[Data](
          s"""
             | FROM ${classOf[Data].getName}
             | WHERE root.id = :domain
             | AND type = 'id'
             | AND string in (:names)
           """.stripMargin,
          classOf[Data]
        )
        .setParameter("domain", domain.id)
        .setParameter("names", names.asJava)
        .getResultList
        .asScala
        .toSeq

    entries.map(data => data.getString -> data.getOwner).toMap
  end getNamedItems

  override def setItemName(name: String, item: Item): Unit =
    val existing: Option[Item] = getNamedItem(name)

    if existing.nonEmpty then
      // Named items are effectively singletons, we shouldn't ever create a second one
      throw new IllegalArgumentException(s"Item with name $name already exists: ${existing.get.getId}")
    else
      val domainItem: Item = getDomainItem(domain)

      val idData = new Data

      idData.setString(name)
      idData.setType("id")
      idData.setOwner(item)
      idData.setRoot(domainItem)

      session.persist(idData)
    end if
  end setItemName
end LightweightItemServiceImpl
