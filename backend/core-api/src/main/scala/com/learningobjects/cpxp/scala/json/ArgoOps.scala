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
import com.learningobjects.cpxp.component.web.HttpResponseException
import org.apache.http.HttpStatus

import scala.util.{Failure, Success, Try}

final class JsonOps(private val self: Json) extends AnyVal:
  def as_![T](msg: => String)(implicit T: DecodeJson[T]): Try[T] =
    self.as(using T).fold((s, h) => Failure(ArgoParseException(msg + "; " + s, h)), Success.apply)

final class DecodeResultOps[A](private val self: DecodeResult[A]) extends AnyVal:
  def withMessage(msg: => String): DecodeResult[A] =
    DecodeResult[A](self.result.left.map(_.copy(_1 = msg)))

  def valueOr[AA >: A](failure: String => AA): AA = self.fold(
    (s, h) => failure(s"$s at ${h.toList.mkString(", ")}"),
    identity
  )

object ArgoOps extends ToArgoSyntax

trait ToArgoSyntax:
  import language.implicitConversions

  @inline implicit final def ToArgoSyntax(j: Json): JsonOps =
    new JsonOps(j)

  @inline implicit final def ToDecodeResultSyntax[A](dr: DecodeResult[A]): DecodeResultOps[A] =
    new DecodeResultOps[A](dr)

/** A slightly resty parsing error because this is web tier. I am jackson. */
final case class ArgoParseException(error: String, history: CursorHistory)
    extends HttpResponseException(
      HttpStatus.SC_UNPROCESSABLE_ENTITY,
      ArgoParseException.msg(error, history)
    )

object ArgoParseException:
  /* We include the history so that the error message isn't totally useless.
   * It is somewhat opaque if you don't know how Argonaut works.
   * Could be improved. */
  def msg(error: String, history: CursorHistory) =
    s"$error (at ${history.toList.mkString(", ")})"
