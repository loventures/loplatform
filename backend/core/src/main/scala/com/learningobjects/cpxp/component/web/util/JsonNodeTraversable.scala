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

package com.learningobjects.cpxp.component.web.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}

import scala.jdk.CollectionConverters.*

object JsonNodeTraversable:

  val factory = JsonNodeFactory.instance

  /** Traverses the elements of `node` transforming each element with `f`.
    * @param node
    *   node to traverse
    * @param f
    *   mapping function, return `Some` to stop traversal
    * @return
    *   a copy of `node` after applying `f` to each of its elements
    */
  def traverse(node: JsonNode, f: JsonNode => Option[JsonNode]): JsonNode =

    val applied = f(node)

    applied match
      case Some(x) => x                     // we're done
      case None    => traverseNext(node, f) // keep traversing

  private def traverseNext(node: JsonNode, f: JsonNode => Option[JsonNode]) =
    node match

      case o: ObjectNode =>
        val traversed = for
          entry     <- o.properties.asScala
          fieldName  = entry.getKey
          fieldValue = entry.getValue
        yield (fieldName, traverse(fieldValue, f))
        factory.objectNode().setAll(traversed.toMap.asJava)

      case a: ArrayNode =>
        val traversed = a.asScala.map(traverse(_, f))
        factory.arrayNode().addAll(traversed.asJavaCollection)

      case x => x

  /** Traverses the elements of `node` collecting stuff with `f`
    * @param node
    *   node to traverse
    * @param f
    *   collector, return `Some` to stop traversal
    * @tparam T
    *   type of collected object
    * @return
    *   collected objects
    */
  def collect[T](node: JsonNode, f: JsonNode => Option[Seq[T]]): Seq[T] =

    val collected = f(node)
    collected match
      case Some(x) => x // we have collected the node, stop
      case None    => node.asScala.toSeq.flatMap(collect(_, f))
end JsonNodeTraversable
