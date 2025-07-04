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

package com.learningobjects.cpxp.postgresql

import argonaut.*
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode, TextNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.json.ArgoParseException
import doobie.*
import doobie.util.invariant.{NullableCellRead, NullableCellUpdate}
import org.postgresql.util.PGobject
import scaloi.syntax.AnyOps.*
import scaloi.syntax.ClassTagOps.*

import scala.reflect.ClassTag

trait DoobieMetas:

  implicit val jsonNodeMeta: Meta[JsonNode]     = jacksonMeta[JsonNode]
  implicit val objectNodeMeta: Meta[ObjectNode] = jacksonMeta[ObjectNode]
  implicit val arrayNodeMeta: Meta[ArrayNode]   = jacksonMeta[ArrayNode]
  implicit val textNodeMeta: Meta[TextNode]     = jacksonMeta[TextNode]
  // you get the picture, add ones as you need them

  implicit val argoJsonMeta: Meta[Json] =
    // super functional doobie awesome greatness
    def unparseable(why: String) =
      throw new ArgoParseException(why, CursorHistory.empty)
    // see below re: jsonb
    Meta.Advanced
      .other[PGobject]("jsonb")
      .timap(pgob => Parse.parse(pgob.getValue).fold(unparseable, identity))(json =>
        new PGobject <| { pgob =>
          pgob.setType("jsonb")
          pgob.setValue(json.nospaces)
        }
      )
  end argoJsonMeta

  // stolen from doobie.postgres.pgtypes
  private def boxedPair[A >: Null <: AnyRef: ClassTag](
    elemType: String,
    arrayType: String,
    arrayTypeT: String*
  ): (Meta[Array[A]], Meta[Array[Option[A]]]) =
    val raw = Meta.Advanced.array[A](elemType, arrayType, arrayTypeT*)

    // Ensure `a`, which may be null, which is ok, contains no null elements.
    def checkNull[B >: Null](a: Array[B], e: Exception): Array[B] =
      if a == null then null else if a.contains(null) then throw e else a

    (
      raw.timap(checkNull(_, NullableCellRead))(checkNull(_, NullableCellUpdate)),
      raw.timap[Array[Option[A]]](_.map(Option(_)))(_.map(_.orNull).toArray)
    )
  end boxedPair

  private val jnArrayTypes = boxedPair[JsonNode]("jsonb", "_jsonb")

  implicit val unliftedJsonNodeArrayType: doobie.Meta[Array[JsonNode]]       = jnArrayTypes._1
  implicit val liftedJsonNodeArrayType: doobie.Meta[Array[Option[JsonNode]]] = jnArrayTypes._2

  private def jacksonMeta[A <: JsonNode: ClassTag]: Meta[A] =
    // this works on columns with "json" type too /shrug
    Meta.Advanced
      .other[PGobject]("jsonb")
      .timap[A](pgObject2JsonNode[A](JacksonUtils.getFinatraMapper))(
        jsonNode2PgObject(JacksonUtils.getFinatraMapper)
      )

  private def pgObject2JsonNode[A <: JsonNode: ClassTag](mapper: ObjectMapper)(
    pgObject: PGobject
  ): A = mapper.readValue[A](pgObject.getValue, classTagClass[A])

  private def jsonNode2PgObject(mapper: ObjectMapper)(
    jsonNode: JsonNode
  ): PGobject =
    val pgObject = new PGobject
    // this works on columns with "json" type too /shrug
    pgObject.setType("jsonb")
    pgObject.setValue(jsonNode.toString)
    pgObject
end DoobieMetas

object DoobieMetas extends DoobieMetas
