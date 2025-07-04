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

package loi.authoring.project

import argonaut.Json
import argonaut.JsonIdentity.*
import cats.syntax.either.*
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import loi.argonaut.instances.long.*
import loi.argonaut.instances.uuid.*
import loi.authoring.commit.CommitDecodeException
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate

import java.util.UUID
import scala.annotation.meta.field

@Entity
@Table(
  name = "authoringcommitdoc",
  indexes = Array(
    new Index(name = "authoringcommitdoc_root_idx", columnList = "root_id")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@hibernate.Immutable
class CommitDocEntity(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[ArgonautUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var nodes: Json /* Map[Long, Map[UUID, Long]] */,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[ArgonautUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var edges: Json /* Map[Long, Map[UUID, Long]] */,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[ArgonautUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var deps: Json /* Map[Long, Commit2.Dep] */,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):

  def this() = this(null, null, null, null, null)

  def this(doc: Commit2.Doc, root: DomainFinder) = this(
    null,
    doc.nodes.asJson,
    doc.edges.asJson,
    doc.deps.asJson,
    root
  )

  def decoded: Commit2.Doc = Commit2.Doc(
    nodes.as[Map[Long, Map[UUID, Long]]].result.valueOr(err => throw new CommitDecodeException(id, "nodes", err)),
    edges.as[Map[Long, Map[UUID, Long]]].result.valueOr(err => throw new CommitDecodeException(id, "edges", err)),
    deps.as[Map[Long, Commit2.Dep]].result.valueOr(err => throw new CommitDecodeException(id, "deps", err)),
  )
end CommitDocEntity
