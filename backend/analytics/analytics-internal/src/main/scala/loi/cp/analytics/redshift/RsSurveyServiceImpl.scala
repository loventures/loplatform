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

package loi.cp.analytics.redshift

import cats.data.NonEmptyList
import cats.instances.list.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.learningobjects.cpxp.component.annotation.Service
import doobie.*
import doobie.implicits.*

import java.util.UUID

@Service
class RsSurveyServiceImpl(
  rsSurveyDao: RsSurveyDao
) extends RsSurveyService:

  override def loadResponseStats(
    sectionIds: NonEmptyList[Long],
  ): ConnectionIO[List[ResponseStats]] =
    for responseCounts <- rsSurveyDao.countResponses(sectionIds)
    yield responseCounts.map(r => ResponseStats(r.contentEdgePath, r.count))

  override def loadQuestionStats(
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
    essayResponsesLimit: Option[Int]
  ): ConnectionIO[List[QuestionStats]] =

    for
      choiceResponseCounts <- rsSurveyDao.countChoiceResponses(sectionIds, contentEdgePaths)
      essayResponseCounts  <- rsSurveyDao.countEssayResponses(sectionIds, contentEdgePaths)
      essayResponses       <- essayResponseCounts
                                .traverse(r =>
                                  rsSurveyDao
                                    .loadEssayResponses(r.questionName, sectionIds, contentEdgePaths, 0, essayResponsesLimit)
                                    .tupleLeft(r.questionName)
                                )
                                .map(_.toMap)
    yield

      val choiceStats = choiceResponseCounts
        .groupBy(_.questionName)
        .map({ case (questionName, responses) =>
          ChoiceQuestionStats(UUID.fromString(questionName), responses.map(r => ChoiceCount(r.choice, r.count)))
        })
        .toList

      val essayStats = essayResponseCounts.map(erc =>
        // 1 page of responses
        val responses = essayResponses.getOrElse(erc.questionName, Nil)
        EssayQuestionStats(UUID.fromString(erc.questionName), erc.count, responses)
      )

      choiceStats ++ essayStats

  override def loadEssayResponses(
    questionAssetName: UUID,
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
    offset: Int,
    limit: Option[Int],
  ): ConnectionIO[List[String]] =

    rsSurveyDao.loadEssayResponses(questionAssetName.toString, sectionIds, contentEdgePaths, offset, limit)
end RsSurveyServiceImpl
