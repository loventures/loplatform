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

package loi.cp.zip

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.site.SiteComponent
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.attachment.AttachmentComponent

import java.lang as jl

@Schema("zip-site")
trait ZipSite extends ComponentInterface with SiteComponent with QueryableId:
  import ZipSite.*

  @Queryable
  @JsonProperty(value = NameProperty)
  def getName: String

  @Queryable
  @JsonProperty(value = PathProperty)
  def getPath: String

  @JsonProperty
  final def getSiteId: Long = getSite.getId

  @Queryable
  @JsonProperty
  def getDisabled: Boolean

  @RequestMapping(path = "revisions", method = Method.GET)
  def getRevisions(aq: ApiQuery): ApiQueryResults[AttachmentComponent]

  @RequestMapping(path = "revisions/{revisionId}", method = Method.GET)
  def getRevision(@PathVariable("revisionId") id: Long): Option[AttachmentComponent]

  @RequestMapping(path = "revisions/{revisionId}", method = Method.DELETE)
  def deleteRevision(@PathVariable("revisionId") id: Long): WebResponse // ErrorResponse \/ NoContentResponse

  @RequestMapping(path = "site", method = Method.GET)
  def getSite: AttachmentComponent

  @RequestMapping(path = "render", method = Method.GET)
  def render(
    @QueryParam(required = false) revision: Option[jl.Long],
  ): WebResponse
end ZipSite

object ZipSite:

  final val DisabledProperty = "disabled"
  final val NameProperty     = "name"
  final val PathProperty     = "path"
