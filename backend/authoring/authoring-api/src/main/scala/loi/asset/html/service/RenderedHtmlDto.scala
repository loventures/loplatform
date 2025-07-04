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

package loi.asset.html.service

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils

/** Rendered html node for use in the delivery client application
  *
  * currently, the only value is HTML, but ultimately there will be other types of rendered content
  *
  * @param html
  *   the html to show
  * @param safe
  *   whether the content needs to be placed in an iframe or not
  * @param `type`
  *   the type of the
  */
case class RenderedHtmlDto(
  html: String,
  safe: Boolean,
  `type`: String
):

  import RenderedHtmlDto.*

  def toJsonNode: JsonNode =
    om.valueToTree(this)
end RenderedHtmlDto

object RenderedHtmlDto:
  private val om = JacksonUtils.getFinatraMapper
