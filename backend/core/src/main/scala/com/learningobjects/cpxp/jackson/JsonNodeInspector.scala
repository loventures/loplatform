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

package com.learningobjects.cpxp.jackson

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}

import scala.jdk.CollectionConverters.*

/** Inspects `JsonNode`s. Given 23 nodes that look like this:
  *
  * <pre> { "questionID": null "AssetQuestion.complexQuestionText": { "html": "" } } </pre>
  *
  * And one that looks like this <pre> { "questionID": "42" } </pre>
  *
  * If you inspect all 24 nodes, you will get a report like this: <pre> { "_count": "24 objects", "questionID": "23
  * nulls, 1 string" "AssetQuestion.complexQuestionText": { "_count": "23 objects" "html": "23 empty strings" } } </pre>
  */
class JsonNodeInspector:

  /** Gets aggregate statistics about the nodes
    *
    * @param nodes
    *   the nodes to inspect
    * @return
    *   statistics about the nodes
    */
  def inspect(nodes: Seq[JsonNode]): StatNode =

    val stats = nodes.map(inspect)
    stats.reduceOption(_ + _).getOrElse(ValueStatNode())

  /** Get statistics about the node.
    *
    * @param node
    *   the node to inspect
    * @return
    *   statistics about the node
    */
  def inspect(node: JsonNode): StatNode =

    val valueStat = ValueStatNode(node)

    node match
      case objNode: ObjectNode =>
        val children = objNode.properties.asScala
          .map(entry => entry.getKey -> inspect(entry.getValue))
          .toMap
        ObjectStatNode(valueStat, children)

      case arrNode: ArrayNode =>
        val elements  = arrNode.asScala.toSeq.map(inspect)
        val itemStats = elements.reduceOption(_ + _)
        ArrayStatNode(valueStat, itemStats)

      case valueNode => valueStat
    end match
  end inspect
end JsonNodeInspector

sealed trait StatNode:

  def +(that: StatNode): StatNode

case class ObjectStatNode(
  count: StatNode = ValueStatNode(),
  children: Map[String, StatNode] = Map.empty
) extends StatNode:

  override def +(that: StatNode): StatNode =

    that match
      case ObjectStatNode(thatCount, thatChildren) =>
        val mergedChildren = children ++ thatChildren.map { case (thatKey, thatValue) =>
          val thisValue   = children.get(thatKey)
          val mergedValue = thisValue.map(_ + thatValue)
          thatKey -> mergedValue.getOrElse(thatValue)
        }
        ObjectStatNode(count + thatCount, mergedChildren)
      case ArrayStatNode(thatCount, items)         =>
        val mergedChildren = items.foldLeft(children)(_.updated("_items", _))
        ObjectStatNode(count + thatCount, mergedChildren)
      case v: ValueStatNode                        => ObjectStatNode(count + v, children)

  @JsonValue
  def jsonValue: Map[String, StatNode] = children.updated("_count", count)
end ObjectStatNode

case class ArrayStatNode(
  count: StatNode = ValueStatNode(),
  items: Option[StatNode] = None
) extends StatNode:

  override def +(that: StatNode): StatNode =

    that match
      case ObjectStatNode(thatCount, children) =>
        val mergedChildren = items.foldLeft(children)(_.updated("_items", _))
        ObjectStatNode(count + thatCount, mergedChildren)
      case ArrayStatNode(thatCount, thatItems) =>
        val mergedItems = (items, thatItems) match
          case (Some(a), Some(b)) => Some(a + b)
          case (Some(a), None)    => Some(a)
          case (None, Some(b))    => Some(b)
          case (None, None)       => None
        ArrayStatNode(count + thatCount, mergedItems)
      case v: ValueStatNode                    => ArrayStatNode(count + v, items)
end ArrayStatNode

case class ValueStatNode(
  numEmptyArray: Long = 0,
  numArray: Long = 0,
  numBoolean: Long = 0,
  numNull: Long = 0,
  numNumber: Long = 0,
  numEmptyString: Long = 0,
  numString: Long = 0,
  numObject: Long = 0,
  numOther: Long = 0
) extends StatNode:

  override def +(that: StatNode): StatNode =

    that match
      case ObjectStatNode(thatCount, children) => ObjectStatNode(this + thatCount, children)
      case ArrayStatNode(thatCount, items)     => ArrayStatNode(this + thatCount, items)
      case v: ValueStatNode                    =>
        ValueStatNode(
          numEmptyArray + v.numEmptyArray,
          numArray + v.numArray,
          numBoolean + v.numBoolean,
          numNull + v.numNull,
          numNumber + v.numNumber,
          numEmptyString + v.numEmptyString,
          numString + v.numString,
          numObject + v.numObject,
          numOther + v.numOther
        )

  @JsonValue
  override def toString: String =

    def text(value: Long, propName: String): Option[String] =
      val name = if value == 1 then propName else propName + "s"
      if value == 0 then None else Some(s"$value $name")

    Seq(
      text(numEmptyArray, "empty array"),
      text(numArray, "array"),
      text(numBoolean, "boolean"),
      text(numNull, "null"),
      text(numNumber, "number"),
      text(numEmptyString, "empty string"),
      text(numString, "string"),
      text(numObject, "object"),
      text(numOther, "other")
    ).flatten.mkString(", ")
  end toString
end ValueStatNode

object ValueStatNode:
  def apply(node: JsonNode): ValueStatNode =

    if node.isArray then
      if node.size() == 0 then ValueStatNode(numEmptyArray = 1)
      else ValueStatNode(numArray = 1)
    else if node.isBoolean then ValueStatNode(numBoolean = 1)
    else if node.isNull then ValueStatNode(numNull = 1)
    else if node.isNumber then ValueStatNode(numNumber = 1)
    else if node.isTextual then
      if node.asText.isEmpty then ValueStatNode(numEmptyString = 1)
      else ValueStatNode(numString = 1)
    else if node.isObject then ValueStatNode(numObject = 1)
    else ValueStatNode(numOther = 1)
end ValueStatNode
