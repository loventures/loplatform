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

import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import loi.authoring.project.ProjectEntity2
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.annotations as hibernate

import java.util.UUID
import scala.annotation.meta.field

// That `project` is present on durable edge is a great shame.
// Shallow-project-copy creates a new project whose head commit is the head of the origin project.
// It produces two diverge-able projects with the same node and edge names.
// For many years we did not track durable edge, so a drop-add in the origin project and/or a drop-add in the
// copy would produce two new edge names between the same two source and target names.
// Thus the project_id has to be present, forever, on the durable edge entity.
@Entity
@Table(
  name = "authoringdurableedge",
  uniqueConstraints = Array(
    new UniqueConstraint(
      name = "authoringdurableedge_key_uc",
      columnNames = Array("root_id", "project_id", "sourcename", "targetname", "`group`")
    )
  ),
  indexes = Array(
    new Index(name = "authoringdurableedge_key_idx", columnList = "root_id,project_id,sourcename,targetname,`group`")
  )
)
// WARNING. Upon flush, entity may not INSERT by design, but the entity still exists in the session.
// And it would also exist in L2 if it were not for my CacheConcurrencyStrategy.NONE configuration.
// The App never fetches DurableEdgeEntity2 by id, so I am safe?
@hibernate.SQLInsert(sql =
  """INSERT INTO authoringdurableedge ("group",name,project_id,root_id,sourceName,targetName,id) VALUES (?,?,?,?,?,?,?)
     ON CONFLICT ON CONSTRAINT authoringdurableedge_key_uc DO NOTHING
     """
)
// App reads this entity by JPA or native query - not by id - so L2 cache never hit anyway.
@hibernate.Cache(usage = CacheConcurrencyStrategy.NONE)
@hibernate.Immutable
class DurableEdgeEntity2(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(Basic @field)(optional = false)
  var sourceName: UUID,
  @(Basic @field)(optional = false)
  var targetName: UUID,
  @(Basic @field)(optional = false)
  @(Column @field)(name = "`group`")
  var group: String,
  @(Basic @field)(optional = false)
  var name: UUID,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var project: ProjectEntity2,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):
  // zero arg constructor required by Hibernate
  def this() = this(null, null, null, null, null, null, null)
end DurableEdgeEntity2
