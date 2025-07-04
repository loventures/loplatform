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

package com.learningobjects.cpxp.scala.util

import argonaut.*
import argonaut.Argonaut.*
import scalaz.\/

import java.net.{URI, URL}

object URLCodec:

  implicit def urlCodec: CodecJson[URL] =
    CodecJson.apply(
      url => jString(url.toString),
      cursor =>
        for
          str <- cursor.as[String]
          url <- parseUrl(cursor, str)
        yield url
    )

  private def parseUrl(cursor: HCursor, str: String): DecodeResult[URL] =
    \/.attempt(DecodeResult.ok(URI.create(str).toURL))(t => DecodeResult.fail[URL](t.getMessage, cursor.history)).merge
end URLCodec
