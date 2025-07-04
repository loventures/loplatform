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

package loi.authoring.edge.store

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.ThreadTerminator
import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, Group}
import loi.cp.asset.edge.EdgeData
import org.hibernate.{CacheMode, Session}
import scaloi.syntax.boxes.*

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import scala.jdk.CollectionConverters.*

@Service
class EdgeDao2(
  session: => Session,
  userDto: => UserDTO,
):

  def create(
    name: UUID,
    source: UUID,
    target: UUID,
    group: Group,
    position: Int,
    traverse: Boolean,
    localId: UUID,
    data: EdgeData,
    created: LocalDateTime,
    modified: LocalDateTime,
    domain: DomainFinder,
  ): EdgeEntity2 =
    val entity = new EdgeEntity2(
      null,
      name,
      source,
      target,
      group.entryName,
      position,
      traverse,
      localId,
      created,
      modified,
      session.getReference(classOf[UserFinder], userDto.id),
      JacksonUtils.getFinatraMapper.valueToTree[JsonNode](data),
      domain
    )
    session.persist(entity)
    entity
  end create

  def load(id: Long): Option[EdgeEntity2] =
    ThreadTerminator.check()
    Option(session.find(classOf[EdgeEntity2], id))

  def load(ids: Iterable[Long]): List[EdgeEntity2] =
    ThreadTerminator.check()
    session
      .byMultipleIds(classOf[EdgeEntity2])
      .enableSessionCheck(true)
      .enableOrderedReturn(false)
      .`with`(CacheMode.NORMAL)
      .multiLoad(ids.boxInsideTo[java.util.List]())
      .asScala
      .toList
  end load
end EdgeDao2

object EdgeDao2:
  def entityToEdge[S, T](
    entity: EdgeEntity2,
    source: Asset[S],
    target: Asset[T]
  ): AssetEdge[S, T] =
    AssetEdge(
      id = entity.id,
      name = entity.name,
      edgeId = entity.localId,
      source = source,
      target = target,
      group = Group.withName(entity.group),
      position = entity.position.toInt,
      traverse = entity.traverse,
      data = JacksonUtils.getFinatraMapper.treeToValue(entity.data, classOf[EdgeData]),
      created = Timestamp.valueOf(entity.created),
      modified = Timestamp.valueOf(entity.modified)
    )
end EdgeDao2
