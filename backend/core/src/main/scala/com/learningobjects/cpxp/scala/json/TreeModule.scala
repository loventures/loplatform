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

package com.learningobjects.cpxp.scala.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule
import scalaz.{StrictTree, Tree}
import scaloi.data.ListTree
import scaloi.syntax.ClassTagOps.*

import scala.reflect.ClassTag

/** Jackson module to allow serialization /*and deserialization*/ of Tree values.
  */
trait TreeModule extends TreeSerializerModule

// serialization

trait TreeSerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` TreeSerializerResolver
  }

object TreeSerializerResolver extends Serializers.Base:
  override def findSerializer(config: SerializationConfig, jt: JavaType, beanDesc: BeanDescription): JsonSerializer[?] =
    if classOf[Tree[?]].isAssignableFrom(jt.getRawClass) then TreeSerializer
    else if classOf[StrictTree[?]].isAssignableFrom(jt.getRawClass) then StrictTreeSerializer
    else if classOf[ListTree[?]].isAssignableFrom(jt.getRawClass) then ListTreeSerializer
    else null

// if only there was some ..

sealed abstract class AbstractTreeSerializer[T: ClassTag] extends StdSerializer[T](classTagClass[T]):
  override def serialize(tree: T, jg: JsonGenerator, sp: SerializerProvider): Unit =
    def write(t: T): Unit =
      jg.writeStartObject()
      jg.writeObjectField("value", rootLabel(t))
      val children = subForest(t)
      if children.nonEmpty then
        jg.writeArrayFieldStart("children")
        children foreach write
        jg.writeEndArray()
      jg.writeEndObject()
    write(tree)
  end serialize

  protected def rootLabel(tree: T): Any
  protected def subForest(tree: T): Seq[T]
end AbstractTreeSerializer

object TreeSerializer extends AbstractTreeSerializer[Tree[?]]:
  override protected def rootLabel(tree: Tree[?]): Any          = tree.rootLabel
  override protected def subForest(tree: Tree[?]): Seq[Tree[?]] =
    tree.subForest.toList // shame; everything else should return an estream

object StrictTreeSerializer extends AbstractTreeSerializer[StrictTree[?]]:
  override protected def rootLabel(tree: StrictTree[?]): Any                = tree.rootLabel
  override protected def subForest(tree: StrictTree[?]): Seq[StrictTree[?]] = tree.subForest

object ListTreeSerializer extends AbstractTreeSerializer[ListTree[?]]:
  override protected def rootLabel(tree: ListTree[?]): Any              = tree.rootLabel
  override protected def subForest(tree: ListTree[?]): Seq[ListTree[?]] = tree.subForest
