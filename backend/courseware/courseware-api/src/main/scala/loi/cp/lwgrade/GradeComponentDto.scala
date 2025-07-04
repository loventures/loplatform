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

package loi.cp.lwgrade

import java.time.Instant
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.reference.EdgePath
import argonaut.CodecJson
import argonaut.Argonaut.*
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

final case class GradeComponentDto(
  grade: Option[Double],
  max: Double,
  info: GradeInfoDto,
  column_id: EdgePath,
  id: EdgePath,
  user_id: UserId,
  raw_grade: Option[Double],
  gradeSyncHistory: SingleGradeHistory
)

object GradeComponentDto:
  final val Type                                   = "grade"
  val empty                                        = GradeComponentDto(
    grade = None,
    max = 0.0,
    info = GradeInfoDto.empty,
    id = EdgePath.Root,
    column_id = EdgePath.Root,
    user_id = UserId(0L),
    raw_grade = None,
    gradeSyncHistory = SingleGradeHistory.empty
  )
  import SingleGradeHistory.singleCodec
  implicit val codec: CodecJson[GradeComponentDto] =
    val codec =
      CodecJson.casecodec8(GradeComponentDto.apply, ArgoExtras.unapply)(
        "grade",
        "max",
        "info",
        "column_id",
        "id",
        "user_id",
        "raw_grade",
        "gradeSyncHistory"
      )

    val encoder = codec.mapJson(_.withObject(("_type" := "grade") +: _))
    CodecJson.derived(using encoder, codec.Decoder)
  end codec
end GradeComponentDto

final case class GradeOverrideDto(
  columnId: EdgePath,
  studentId: Long,
  grade: Double,
  `override`: Boolean,
)
object GradeOverrideDto:
  implicit val codec: CodecJson[GradeOverrideDto] =
    CodecJson.casecodec4(GradeOverrideDto.apply, ArgoExtras.unapply)(
      "columnId",
      "studentId",
      "grade",
      "override"
    )

final case class GradeInfoDto(
  grade: Option[Double],
  score: ScoreDto,
  rawScore: ScoreDto,
  submissionDate: Instant,
  unscaledScore: ScoreDto,
  lti: LtiOutcomeInfoDto,
  status: String,
  returnUrl: String,
  items: String,
  history: List[HistoryDto],
  isOverridden: Boolean,
)

object GradeInfoDto:

  val empty = GradeInfoDto(
    grade = None,
    score = ScoreDto.empty,
    rawScore = ScoreDto.empty,
    submissionDate = Instant.EPOCH,
    unscaledScore = ScoreDto.empty,
    lti = LtiOutcomeInfoDto.empty,
    status = "",
    returnUrl = "",
    items = "",
    history = List.empty,
    isOverridden = false
  )

  implicit val codec: CodecJson[GradeInfoDto] = CodecJson.casecodec11(GradeInfoDto.apply, ArgoExtras.unapply)(
    "grade",
    "score",
    "rawScore",
    "submissionDate",
    "unscaledScore",
    "lti",
    "status",
    "returnUrl",
    "items",
    "history",
    "isOverridden"
  )
end GradeInfoDto

final case class ScoreDto(awarded: Option[Double], possible: Double)

object ScoreDto:
  val empty = ScoreDto(awarded = None, possible = 0.0)

  implicit val codec: CodecJson[ScoreDto] =
    CodecJson.casecodec2(ScoreDto.apply, ArgoExtras.unapply)("awarded", "possible")

final case class LtiOutcomeInfoDto(system: Long, url: String, id: String)

object LtiOutcomeInfoDto:

  val empty = LtiOutcomeInfoDto(system = 0L, url = "", id = "")

  implicit val codec: CodecJson[LtiOutcomeInfoDto] =
    CodecJson.casecodec3(LtiOutcomeInfoDto.apply, ArgoExtras.unapply)(
      "system",
      "url",
      "id"
    )
end LtiOutcomeInfoDto
final case class HistoryDto(
  user: Long,
  date: Instant,
  grade: Option[Double],
  score: ScoreDto,
  isOverridden: Boolean,
  attemptedGradeSet: Double,
)
object HistoryDto:
  val empty = HistoryDto(
    user = 0L,
    date = Instant.EPOCH,
    grade = None,
    score = ScoreDto.empty,
    isOverridden = false,
    attemptedGradeSet = 0.0
  )

  implicit val codec: CodecJson[HistoryDto] = CodecJson.casecodec6(HistoryDto.apply, ArgoExtras.unapply)(
    "user",
    "date",
    "grade",
    "score",
    "isOverridden",
    "attemptedGradeSet"
  )
end HistoryDto
