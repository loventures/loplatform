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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.domain.DomainFinder
import com.learningobjects.cpxp.service.user.UserFinder
import com.learningobjects.cpxp.util.ScalaPropertyAccessStrategy
import jakarta.persistence.*
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.service.exception.AssetLoadNoSuchTypeException
import loi.authoring.asset.{Asset, AssetInfo}
import org.hibernate.annotations.{AttributeAccessor, CacheConcurrencyStrategy}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType
import org.hibernate.annotations as hibernate
import scaloi.syntax.localDateTime.*

import java.time.LocalDateTime
import java.util.UUID
import scala.annotation.meta.field

@Entity
@Table(
  name = "authoringnode",
  indexes = Array(
    new Index(name = "authoringnode_root_idx", columnList = "root_id")
  )
)
@hibernate.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@hibernate.Immutable
class NodeEntity2(
  @(Id @field)
  @(GeneratedValue @field)(generator = "cpxp-sequence")
  @(AttributeAccessor @field)(strategy = classOf[ScalaPropertyAccessStrategy])
  var id: java.lang.Long,
  @(Basic @field)(optional = false)
  var name: UUID,
  @(Basic @field)(optional = false)
  var typeId: String,
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
  var title: String,
  var subtitle: String,
  @(Column @field)(columnDefinition = "TEXT")
  var description: String,
  var keywords: String,
  @(Basic @field)(optional = false)
  var archived: java.lang.Boolean,
  @(ManyToOne @field)(fetch = FetchType.LAZY, optional = false)
  var root: DomainFinder,
):

  // zero arg constructor required by Hibernate
  def this() = this(null, null, null, null, null, null, null, null, null, null, null, null, null)

  def toAssetA[A](implicit assetType: AssetType[A]): Asset[A] =
    // can't use finatra because pre-finatra data is invalid
    val dataDeser = JacksonUtils.getMapper.treeToValue(data, assetType.dataClass)
    Asset(toAssetInfo, dataDeser)

  def toAsset: Asset[?] = toAssetA(using assetTypeOrThrow)

  def toAssetInfo: AssetInfo = AssetInfo(
    id,
    name,
    AssetTypeId.withName(typeId),
    created.asDate,
    Option(modifiedBy).filter(_.getDel eq null).map(_.loadDtoNoInit),
    modified.asDate,
    archived
  )

  private def assetTypeOrThrow: AssetType[?] =
    AssetType.types.getOrElse(AssetTypeId.withName(typeId), throw AssetLoadNoSuchTypeException(id, typeId))
end NodeEntity2
