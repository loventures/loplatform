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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate

import java.lang
import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.meta.field

@Entity
@Table(
  name = "authoringcommit",
  indexes = Array(
    new Index(name = "authoringcommit_root_idx", columnList = "root_id")
  )
)
@NamedEntityGraph(
  name = "commit.docs",
  attributeNodes = Array(
    new NamedAttributeNode("kfDoc"),
    new NamedAttributeNode("driftDoc")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@hibernate.Immutable
class CommitEntity2(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  var rootName: UUID,
  var homeName: UUID,
  @(Basic @field)(optional = false)
  var created: LocalDateTime,
  @(ManyToOne @field)(fetch = FetchType.LAZY)
  var createdBy: UserFinder,
  @(ManyToOne @field)(fetch = FetchType.LAZY)
  var parent: CommitEntity2,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var kfDoc: CommitDocEntity,
  @(ManyToOne @field)(fetch = FetchType.LAZY)
  var driftDoc: CommitDocEntity,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[JsonNodeUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var ops: JsonNode, /* Array[DbWriteOp] */
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder
) extends CommitEntityish:

  def this() = this(null, null, null, null, null, null, null, null, null, null)

  // constructor for the first commit of a project
  def this(
    rootName: Option[UUID],
    homeName: Option[UUID],
    kfDoc: CommitDocEntity,
    created: LocalDateTime,
    createdBy: UserFinder,
    root: DomainFinder
  ) = this(
    null,
    rootName.orNull,
    homeName.orNull,
    created,
    createdBy,
    null,
    kfDoc,
    null,
    JsonNodeFactory.instance.arrayNode(),
    root
  )

  override def createdById: lang.Long = createdBy.getId

  override def createdByFfs(is: ItemService): UserFinder = createdBy

  def toCommit: Commit2 =
    Commit2(
      id,
      rootName,
      homeName,
      created,
      Option(createdBy).map(_.getId),
      Option(parent).map(_.id),
      kfDoc.id,
      Option(driftDoc).map(_.id),
      root.getId
    )

  def comboDoc: Commit2.ComboDoc = Commit2.ComboDoc(kfDoc.decoded, Option(driftDoc).map(_.decoded))

  def toBigCommit: BigCommit = BigCommit(toCommit, comboDoc, ops)
end CommitEntity2
