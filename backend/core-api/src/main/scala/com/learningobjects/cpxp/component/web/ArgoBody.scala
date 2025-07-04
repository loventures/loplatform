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

package com.learningobjects.cpxp.component.web

import argonaut.*
import com.learningobjects.cpxp.scala.json.ArgoParseException
import loi.cp.web.mediatype.DeEntity
import scalaz.\/

import scala.util.{Failure, Success, Try}

/** This represents a request body of the entity `T` parsed using argonaut. You can only extract an instance of the
  * entity by ensuring an instance of `DecodeJson[T]` is in the implicit scope.
  */
final class ArgoBody[T](val json: Either[String, Json]) extends DeEntity:
  def decode(implicit decoder: DecodeJson[T]): DecodeResult[T] =
    json.fold(DecodeResult.fail(_, CursorHistory(List.empty)), decoder.decodeJson)

  def decode_!(implicit decoder: DecodeJson[T]): Try[T] =
    decode.fold((s, h) => Failure(ArgoParseException(s, h)), Success.apply)

  def decodeOrMessage(implicit decoder: DecodeJson[T]): String \/ T =
    decode.fold((s, h) => \/.left(ArgoParseException.msg(s, h)), \/.right)

object ArgoBody:

  /** Construct an argo body from a JSON encodable value. */
  def apply[T](body: T)(implicit encoder: EncodeJson[T]): ArgoBody[T] =
    new ArgoBody(Right(encoder.encode(body)))

  def parse[T](json: String): ArgoBody[T] = new ArgoBody(Parse.parse(json))
