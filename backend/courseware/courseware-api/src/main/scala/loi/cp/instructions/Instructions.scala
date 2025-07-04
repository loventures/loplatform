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

package loi.cp.instructions

import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import loi.asset.contentpart.BlockPart

/** The instructions for an assessment. This may be content or a reference to other content.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "instructionType")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[BlockInstructions], name = Instructions.block),
  )
)
sealed trait Instructions

object Instructions:
  final val block = "block"

case class BlockInstructions(block: BlockPart) extends Instructions
