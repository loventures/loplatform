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

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.scala.cpxp.JlrType
import scaloi.syntax.ClassTagOps.*

import scala.language.implicitConversions
import scala.reflect.ClassTag

final class ObjectMapperOps(val mapper: ObjectMapper) extends AnyVal:

  def treeToMap[K: ClassTag, V: ClassTag](tree: TreeNode): Map[K, V] =

    val valueType = mapper.getTypeFactory.constructMapLikeType(
      classOf[Map[?, ?]],
      classTagClass[K],
      classTagClass[V]
    )

    mapper.readValue[Map[K, V]](mapper.treeAsTokens(tree), valueType)

  def tree2Value[A: JlrType](tree: TreeNode): A =
    val valueType = mapper.getTypeFactory.constructType(JlrType[A].tpe)
    mapper.readValue[A](mapper.treeAsTokens(tree), valueType)

  def value2Tree(value: Any): JsonNode =

    // because .valueToTree's return type is not good enough for 2019, its just
    // a cast. It lets you exception yourself.
    //
    // val x = mapper.valueToTree[TextNode](42) // class cast exception
    //
    // This wrapper, besides having the superior value2Tree name, affixes the
    // return type to JsonNode
    mapper.valueToTree(value)

  def parse[A: JlrType](value: String): A =
    // not using valueToTree because that would make a TextNode if `value`
    // where a String containing a JSON document.
    val valueType = mapper.getTypeFactory.constructType(JlrType[A].tpe)
    mapper.readValue(value, valueType)
end ObjectMapperOps

object ObjectMapperOps extends ToObjectMapperOps

trait ToObjectMapperOps:
  implicit def toObjectMapperOps(mapper: ObjectMapper): ObjectMapperOps =
    new ObjectMapperOps(mapper)
