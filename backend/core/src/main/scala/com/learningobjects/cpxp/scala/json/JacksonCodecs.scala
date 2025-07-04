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

import argonaut.*
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.web.util.JacksonUtils

import scala.reflect.*

object JacksonCodecs:

  implicit final val jsonNodeEnc: EncodeJson[JsonNode] = EncodeJson { n =>
    if n == null || n.isNull then Json.jNull
    else JacksonUtils.getMapper.treeToValue(n, classOf[Json])
  }

  implicit final val jsonNodeDec: DecodeJson[JsonNode] = DecodeJson { c =>
    val json     = c.focus // equiv to c.as[Json]
    val jsonNode = JacksonUtils.getMapper.readValue(json.toString(), classOf[JsonNode])
    DecodeResult.ok(jsonNode)
  }

  def codecFromJackson[A: ClassTag]: CodecJson[A] = CodecJson(
    a =>
      val json = JacksonUtils.getMapper.writeValueAsString(a)
      Parse.parse(json) match
        case Left(err)   =>
          throw new Exception(s"Unable to parse json: $json, cause: $err")
        case Right(json) => json
    ,
    (c: HCursor) =>
      DecodeResult {
        for
          json <- c.as[Json].toEither
          res  <-
            DecodeResult
              .ok(JacksonUtils.getMapper.readValue(json.toString(), classTag[A].runtimeClass.asInstanceOf[Class[A]]))
              .toEither
        yield res
      }
  )

  object universal:

    implicit def encodeJsonForUniverse[A]: EncodeJson[A] =
      jsonNodeEnc.contramap(JacksonUtils.getMapper.valueToTree[JsonNode])
end JacksonCodecs
