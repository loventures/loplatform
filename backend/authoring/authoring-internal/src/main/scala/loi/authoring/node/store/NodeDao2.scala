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

package loi.authoring.node.store

import cats.syntax.option.*
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.{StringUtils, ThreadTerminator}
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.service.exception.AssetLoadNoSuchTypeException
import loi.authoring.asset.store.JsonAndSpecialProps
import loi.authoring.asset.{Asset, AssetInfo, NodeInfo}
import org.hibernate.query.NativeQuery
import org.hibernate.{CacheMode, Session}
import scaloi.syntax.boxes.*

import java.time.{LocalDateTime, ZoneId}
import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.*

@Service
class NodeDao2(
  session: => Session,
  userDto: => UserDTO,
):

  def create[A](
    name: UUID,
    data: A,
    assetType: AssetType[A],
    created: LocalDateTime,
    modified: LocalDateTime,
    domain: DomainFinder,
  ): NodeEntity2 =
    ThreadTerminator.check()

    val JsonAndSpecialProps(jsonNode, specialProps) = JsonAndSpecialProps.extract(data)

    val entity = new NodeEntity2(
      null,
      name,
      assetType.id.entryName,
      created,
      modified,
      session.getReference(classOf[UserFinder], userDto.id),
      jsonNode,
      specialProps.title,
      specialProps.subtitle,
      specialProps.description,
      specialProps.keywords,
      specialProps.archived,
      domain,
    )

    assetType
      .computeTitle(data)
      .map(StringUtils.truncate255AndAppendEllipses)
      .foreach(truncatedTitle =>
        entity.title = truncatedTitle
        entity.data match
          case objNode: ObjectNode if assetType.specialProps.title => objNode.put("title", truncatedTitle)
          case _                                                   =>
      )

    session.persist(entity)
    entity
  end create

  def load(id: Long): Option[NodeEntity2] =
    ThreadTerminator.check()
    Option(session.find(classOf[NodeEntity2], id))

  def load(ids: Iterable[Long]): List[NodeEntity2] =
    ThreadTerminator.check()
    session
      .byMultipleIds(classOf[NodeEntity2])
      .enableSessionCheck(true)
      .enableOrderedReturn(false)
      .`with`(CacheMode.NORMAL)
      .multiLoad(ids.boxInsideTo[java.util.List]())
      .asScala
      .toList
  end load

  def loadAllByTypeIds(
    nodeIds: Iterable[Long],
    typeIds: Iterable[AssetTypeId],
    limit: Option[Int]
  ): List[NodeEntity2] =
    ThreadTerminator.check()
    session
      .createNativeQuery[NodeEntity2](
        s"""SELECT n.*
           |FROM authoringnode n
           |WHERE n.id = ANY(CAST(:ids AS BIGINT[]))
           |  AND n.typeId = ANY(CAST(:typeIds AS VARCHAR[]))
           |ORDER BY created DESC
           |${limit.map(l => "LIMIT " + l).orEmpty}
           |""".stripMargin,
        classOf[NodeEntity2]
      )
      .unwrap(classOf[NativeQuery[NodeEntity2]])
      .addSynchronizedEntityClass(classOf[NodeEntity2])
      .setParameter("ids", nodeIds.mkString("{", ",", "}"))
      .setParameter("typeIds", typeIds.mkString("{", ",", "}"))
      .getResultList
      .asScala
      .toList
  end loadAllByTypeIds
end NodeDao2

object NodeDao2:

  def entityToAsset[A](entity: NodeEntity2)(implicit assetType: AssetType[A]): Asset[A] =
    val om   = JacksonUtils.getMapper // Can't use Finatra because pre-Finatra data is invalid
    val data = om.treeToValue(entity.data, assetType.dataClass)
    val info = entityToInfo(entity)
    Asset(info, data)

  def entityToAssetE(entity: NodeEntity2): Asset[?] =
    val assetType = assetTypeOrThrow(entity)
    entityToAsset(entity)(using assetType)

  private def ldt2Date(ldt: LocalDateTime): Date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant)

  def entityToInfo(entity: NodeEntity2): AssetInfo =
    AssetInfo(
      id = entity.id,
      name = entity.name,
      typeId = AssetTypeId.withName(entity.typeId),
      created = ldt2Date(entity.created),
      createdBy = Option(entity.modifiedBy).map(_.loadDtoNoInit),
      modified = ldt2Date(entity.modified),
      archived = entity.archived,
    )

  def entityToNodeInfo(entity: NodeEntity2): NodeInfo =
    NodeInfo(
      id = entity.id,
      name = entity.name,
      typeId = AssetTypeId.withName(entity.typeId),
      created = ldt2Date(entity.created),
      title = entity.title,
      modified = ldt2Date(entity.modified)
    )

  def assetTypeOrThrow(entity: NodeEntity2): AssetType[?] =
    AssetType.types.getOrElse(
      AssetTypeId.withName(entity.typeId),
      throw AssetLoadNoSuchTypeException(entity.id, entity.typeId)
    )
end NodeDao2
