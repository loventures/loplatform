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

import cats.instances.list.*
import cats.instances.map.*
import cats.syntax.option.*
import cats.syntax.semigroup.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.asset.Asset
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.content.CourseContent
import loi.cp.course.lightweight.Lwc
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageServiceImpl
import loi.cp.survey.SurveySubmissionReceiptStorage.*

@Service
class SurveySubmissionService(
  coursewareAnalyticsService: CoursewareAnalyticsService,
  courseStorageService: CourseStorageServiceImpl
):

  def submitSurvey(
    section: Lwc,
    content: CourseContent,
    surveyAsset: Asset[?],
    surveyEdgePath: EdgePath,
    responses: List[SurveyQuestionResponseDto],
    userDto: UserDTO
  ): Either[TooManySubmissions, Unit] =

    // TODO concurrency control
    val edgePath = content.edgePath
    val receipts = courseStorageService.get[SurveySubmissionReceipts](section, userDto).get(edgePath).orEmpty
    if receipts.nonEmpty then Left(TooManySubmissions(receipts.size))
    else
      coursewareAnalyticsService.emitSurveySubmissionEvent(section, content, surveyAsset, surveyEdgePath, responses)
      courseStorageService.modify[SurveySubmissionReceipts](section, userDto)(submissions =>
        submissions |+| Map(edgePath -> List(SurveySubmissionReceipt()))
      )
      Right(())
  end submitSurvey
end SurveySubmissionService

case class TooManySubmissions(numSubmissions: Int)
