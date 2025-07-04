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

import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.annotation.JsonProperty

import java.util as ju
import scala.jdk.CollectionConverters.*
import cats.syntax.option.*

/** An object joining a user's progress with the total possible progress for a <code>Path</code>.
  *
  * This is a web DTO.
  *
  * Progress is submitted for leaf content only. A non-leaf, such as a module, might have 2/9 progress, but that means 2
  * of its descendant leaves are completed, and a total of 9 could be completed.
  *
  * @param completions
  *   the number of completed leaves
  * @param total
  *   the number of all leaves
  * @param progressTypes
  *   extra detail for `completions`, e.g. how many completions are from test-outs, how many are from visitation
  */
final case class Progress(
  completions: Long,
  total: Long,
  progressTypes: ju.Set[IncrementType], // serialization horror
  forCreditGrades: Option[Int],
  forCreditGradesPossible: Option[Int],
):
  def isComplete: Boolean = completions >= total

  @JsonProperty
  def weightedPercentage: Double =
    100 * (if total == 0 then 0d else completions / total.toDouble)
end Progress

object Progress:
  def missing: Progress = Progress(0, 0, new ju.HashSet, 0.some, 0.some)

  implicit val encodeJsonForProgress: EncodeJson[Progress] = (p: Progress) =>
    jObjectFields(
      "completions"             := p.completions,
      "total"                   := p.total,
      "progressTypes"           := p.progressTypes.asScala.toList,
      "forCreditGrades"         := p.forCreditGrades,
      "forCreditGradesPossible" := p.forCreditGradesPossible
    )

  implicit val setDecoder: DecodeJson[ju.Set[IncrementType]] = DecodeJson.of[List[IncrementType]].map(_.toSet.asJava)
  implicit val decode: DecodeJson[Progress]                  = DecodeJson.derive[Progress]
end Progress
