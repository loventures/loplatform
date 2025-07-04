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

package loi.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.util.ObjectMapperOps.*
import com.learningobjects.cpxp.scala.cpxp.JlrType

import scala.language.implicitConversions

class JacksonJsonNodeOps(val node: JsonNode) extends AnyVal:

  def finatraDecoded[A: JlrType]: A = JacksonUtils.getFinatraMapper.tree2Value(node)

  def toObjNode: Option[ObjectNode] = PartialFunction.condOpt(node) { case o: ObjectNode => o }

trait JacksonJsonNodeSyntax:
  implicit def jacksonJsonNodeSyntax(node: JsonNode): JacksonJsonNodeOps = new JacksonJsonNodeOps(node)
