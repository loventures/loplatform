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

package loi.cp.redirect

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.RestfulComponent
import com.learningobjects.cpxp.component.annotation.{ItemMapping, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.site.SiteConstants
import com.learningobjects.cpxp.validation.groups.Writable
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait
import loi.cp.attachment.AttachmentComponent

import javax.validation.groups.Default

@Schema("redirect")
@ItemMapping(value = SiteConstants.ITEM_TYPE_SITE)
trait Redirect extends RestfulComponent[Redirect]:
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE), dataType = DataTypes.DATA_TYPE_NAME)
  @JsonProperty
  def getName: String

  @Queryable
  @JsonProperty
  def isDisabled: Boolean

  @JsonView(Array(classOf[Default]))
  @RequestMapping(path = "csv", method = Method.GET)
  def getCsv: AttachmentComponent

  @JsonView(Array(classOf[Writable]))
  def getCsvUpload: String

  def getRedirects: Map[String, String]
end Redirect
