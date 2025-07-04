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

package loi.cp.progress
package report

import java.time.Instant
import java.util as ju
import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import scalaz.syntax.std.map.*
import scaloi.json.ArgoExtras.instantCodec

import scala.jdk.CollectionConverters.*

/** An object containing [[Progress]] information for multiple [[ProgressPath]] s.
  */
final case class ProgressReport(
  userId: Long,
  lastModified: Option[Instant],
  @JsonSerialize(typing = JsonSerialize.Typing.STATIC) // omgwtfjackson
  progress: ju.Map[ProgressPath, Progress],
):
  def getProgress(pp: ProgressPath): Progress = progress.get(pp)

object ProgressReport:

  implicit val codec: CodecJson[ProgressReport] =
    implicit val mapEncode: EncodeJson[ju.Map[ProgressPath, Progress]] =
      EncodeJson
        .of[Map[String, Progress]]
        .contramap[ju.Map[ProgressPath, Progress]](_.asScala.toMap.mapKeys(_.toString))
    implicit val mapDecode: DecodeJson[ju.Map[ProgressPath, Progress]] =
      DecodeJson
        .of[Map[String, Progress]]
        .map(m => m.mapKeys(ProgressPath(_)).asJava)

    CodecJson.derive[ProgressReport]
  end codec
end ProgressReport
