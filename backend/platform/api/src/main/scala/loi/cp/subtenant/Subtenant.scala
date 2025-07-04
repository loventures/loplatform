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

package loi.cp.subtenant

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.Schema
import com.learningobjects.cpxp.validation.groups.Writable
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.attachment.AttachmentComponent
import loi.cp.subtenant.Subtenant.SubtenantDTO

@Schema("subtenant")
trait Subtenant extends ComponentInterface with QueryableId:

  /** The subtenant identifier. */
  @JsonProperty
  @Queryable(traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getTenantId: String

  /** The human-readable subtenant name. */
  @JsonProperty
  @Queryable(traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getName: String

  /** The human-readable subtenant short name. */
  @JsonProperty
  @Queryable(traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getShortName: String

  /** On submit, the GUID of an uploaded logo file.
    */
  @JsonView(Array(classOf[Writable]))
  def getLogoUpload: Option[String]

  @JsonProperty
  def getLogo: Option[AttachmentComponent]

  /** Delete this subtenant . */
  def delete(): Unit

  def setName(name: String): Unit

  def setTenantId(tenantId: String): Unit

  /** Update this subtenant. */
  def update(subtenant: SubtenantDTO): Unit
end Subtenant

/** Subtenant companion.
  */
object Subtenant:
  /* The various JSON property names. */
  final val TenantIdProperty  = "tenantId"
  final val NameProperty      = "name"
  final val ShortNameProperty = "shortName"

  final case class SubtenantDTO(tenantId: String, name: String, shortName: String, logoUpload: Option[String])

  object SubtenantDTO:
    def apply(subtenant: Subtenant): SubtenantDTO =
      SubtenantDTO(subtenant.getTenantId, subtenant.getName, subtenant.getShortName, subtenant.getLogoUpload)
end Subtenant
