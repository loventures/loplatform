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
import com.learningobjects.cpxp.postgresql.ArgonautUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate

import java.time.LocalDateTime
import scala.annotation.meta.field

@Entity
@Table(
  name = "authoringsyncreport",
  indexes = Array(
    new Index(name = "authoringsyncreport_root_idx", columnList = "root_id")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@hibernate.Immutable
class SyncReportEntity(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(hibernate.AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var project: ProjectEntity2,
  @(Basic @field)(optional = false)
  var created: LocalDateTime,
  @(Basic @field)(optional = false)
  @(Column @field)(columnDefinition = "JSONB")
  @(hibernate.Type @field)(classOf[ArgonautUserType])
  @(hibernate.JdbcType @field)(classOf[PostgreSQLJsonPGObjectJsonbType])
  var body: Json,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):
  def this() = this(null, null, null, null, null)

  def this(project: ProjectEntity2, created: LocalDateTime, body: Json, root: DomainFinder) =
    this(null, project, created, body, root)
end SyncReportEntity
