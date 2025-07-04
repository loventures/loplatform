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

package loi.cp.survey

import java.time.Instant
import argonaut.CodecJson
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStoreable
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

case class SurveySubmissionReceipt(time: Instant = Instant.now)

object SurveySubmissionReceipt:
  implicit val codecJsonForSurveySubmissionReceipt: CodecJson[SurveySubmissionReceipt] =
    CodecJson.casecodec1(SurveySubmissionReceipt.apply, ArgoExtras.unapply1)("time")

object SurveySubmissionReceiptStorage:

  type SurveySubmissionReceipts = Map[EdgePath, List[SurveySubmissionReceipt]]

  implicit val courseStorableForSurveySubmissionReceipts: CourseStoreable[SurveySubmissionReceipts] =
    CourseStoreable[SurveySubmissionReceipts]("surveySubmissionReceipts")(Map.empty)
