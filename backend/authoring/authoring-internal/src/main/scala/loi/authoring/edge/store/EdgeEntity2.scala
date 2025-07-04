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
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import loi.authoring.edge.{EdgeAttrs, EdgeElem, Group}
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate

import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.meta.field

@Entity
@Table(
  name = "authoringedge",
  indexes = Array(
    new Index(name = "authoringedge_root_idx", columnList = "root_id"),
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@hibernate.Immutable
class EdgeEntity2(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(Basic @field)(optional = false)
  var name: UUID,
  @(Basic @field)(optional = false)
  var sourceName: UUID,
  @(Basic @field)(optional = false)
  var targetName: UUID,
  @(Basic @field)(optional = false)
  @(Column @field)(name = "`group`")
  var group: String,
  @(Basic @field)(optional = false)
  var position: java.lang.Integer,
  @(Basic @field)(optional = false)
  var traverse: java.lang.Boolean,
  @(Basic @field)(optional = false)
  var localId: UUID,
  @(Basic @field)(optional = false)
  var created: LocalDateTime,
  @(Basic @field)(optional = false)
  var modified: LocalDateTime,
  @(ManyToOne @field)(fetch = FetchType.LAZY)
  var modifiedBy: UserFinder,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[JsonNodeUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var data: JsonNode,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):

  // zero arg constructor required by Hibernate
  def this() = this(null, null, null, null, null, null, null, null, null, null, null, null, null)

  def toEdgeAttrs: EdgeAttrs =
    EdgeAttrs(name, sourceName, targetName, localId, Group.withName(group), position, traverse)

  def toEdgeElem: EdgeElem = EdgeElem(id, toEdgeAttrs)
end EdgeEntity2
