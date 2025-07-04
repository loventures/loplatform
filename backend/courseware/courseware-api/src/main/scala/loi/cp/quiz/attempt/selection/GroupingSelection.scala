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

package loi.cp.quiz.attempt.selection

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.scala.util.JTypes.{JDouble, JLong}
import org.apache.commons.collections4.IteratorUtils

import scala.jdk.CollectionConverters.*

/** A selection representing selecting one or more answers/distractors for one or more groups.
  *
  * Example: [ American Presidents -> [George H Bush, Bill Clinton, George H W Bush, George Clinton], Musicians -> [...]
  * ]
  */
case class GroupingSelection(
  skip: Boolean,
  @JsonDeserialize(contentAs = classOf[JDouble]) confidence: Option[Double],
  @JsonDeserialize(contentAs = classOf[JLong]) timeElapsed: Option[Long],
  @JsonDeserialize(`using` = classOf[ElementIndexesConverter]) elementIndexesByGroupIndex: Map[Int, Seq[Int]]
) extends QuestionResponseSelection

private class ElementIndexesConverter extends JsonDeserializer[Map[Int, Seq[Int]]]:
  override def deserialize(
    jsonParser: JsonParser,
    deserializationContext: DeserializationContext
  ): Map[Int, Seq[Int]] =
    val mapper = ComponentUtils.getObjectMapper

    val json: JsonNode               = mapper.readTree(jsonParser)
    val tuples: Seq[(Int, Seq[Int])] = for
      key      <- IteratorUtils.toList(json.fieldNames()).asScala.toSeq
      valueNode = json.get(key)
    yield
      val values: Seq[Int] =
        IteratorUtils.toList(valueNode.asInstanceOf[ArrayNode].elements()).asScala.toSeq.map(_.asInt())
      key.toInt -> values

    tuples.toMap
  end deserialize
end ElementIndexesConverter

object GroupingSelection:
  def apply(elementIndexByGroupIndex: Map[Int, Seq[Int]]): GroupingSelection =
    apply(false, None, None, elementIndexByGroupIndex)

  def matching(elementIndexByGroupIndex: Map[Int, Int]): GroupingSelection =
    matching(false, None, None, elementIndexByGroupIndex)

  def matching(
    skip: Boolean,
    confidence: Option[Double],
    timeElapsed: Option[Long],
    elementIndexByGroupIndex: Map[Int, Int]
  ): GroupingSelection =
    GroupingSelection(skip, confidence, timeElapsed, elementIndexByGroupIndex.map(entry => entry._1 -> Seq(entry._2)))
end GroupingSelection
