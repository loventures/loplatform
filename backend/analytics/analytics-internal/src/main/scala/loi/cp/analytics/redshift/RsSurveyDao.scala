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

import argonaut.Argonaut.*
import argonaut.*
import cats.data.{Kleisli, NonEmptyList}
import cats.syntax.functor.*
import cats.syntax.option.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

import java.sql.Timestamp

@Service
class RsSurveyDao(
  insertService: RedshiftInsertService
):

  def insertResponses(responses: NonEmptyList[RsSurveyQuestionResponse]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("surveyquestionresponse", responses.map(_.asJson)).void

  def countResponses(
    sectionIds: NonEmptyList[Long],
  ): ConnectionIO[List[RsResponseCount]] =
    (fr"""select r.contentedgepath, count(distinct r.attemptid)
         |from surveyquestionresponse r""".stripMargin ++
      Fragments.whereAnd(
        Fragments.in(fr"r.sectionid", sectionIds),
      ) ++
      fr"group by r.contentedgepath" ++
      fr"order by r.contentedgepath")
      .query[RsResponseCount]
      .to[List]

  def countChoiceResponses(
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
  ): ConnectionIO[List[RsChoiceCount]] =
    (fr"""select q.name, r.response, count(*)
         |from surveyquestionresponse r
         |join asset q on r.questionassetid = q.id""".stripMargin ++
      Fragments.whereAnd(
        Fragments.in(fr"r.contentedgepath", contentEdgePaths),
        Fragments.in(fr"r.sectionid", sectionIds),
        fr"q.typeid != 'surveyEssayQuestion.1'"
      ) ++
      fr"group by q.name, r.response" ++
      fr"order by q.name, r.response")
      .query[RsChoiceCount]
      .to[List]

  def countEssayResponses(
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
  ): ConnectionIO[List[RsEssayCount]] =

    (fr"""select q.name, count(*)
        |from surveyquestionresponse r
        |join asset q on r.questionassetid = q.id""".stripMargin ++
      Fragments.whereAnd(
        Fragments.in(fr"r.contentedgepath", contentEdgePaths),
        Fragments.in(fr"r.sectionid", sectionIds),
        fr"q.typeid = 'surveyEssayQuestion.1'"
      ) ++
      fr"group by q.name")
      .query[RsEssayCount]
      .to[List]

  // a ROW_NUMBER window function could obtain pages of responses for multiple questions
  // but window functions run almost last (after WHERE). In the window function style
  // we would not be using OFFSET and LIMIT but a WHERE clause on the result of the
  // ROW_NUMBER. This requires an inner query with no offsets and limits and an outer
  // query with a WHERE expression on the ROW_NUMBER result. I did not experiment with how
  // smart Redshift is in this situation. Would it really compute the entire inner query?
  // It would for the last page(s). To be fair, people probably ignore text questions on
  // surveys so the inner query results would grow very slowly. But instead, I chose to
  // query per question. It doesn't matter because they  won't use this until 2024 and
  // we'll be gone.
  def loadEssayResponses(
    questionAssetName: String,
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
    offset: Int,
    limit: Option[Int],
  ): ConnectionIO[List[String]] =
    (fr"""select r.response
         |from surveyquestionresponse r
         |join asset q on r.questionassetid = q.id""".stripMargin ++
      Fragments.whereAnd(
        Fragments.in(fr"r.contentedgepath", contentEdgePaths),
        Fragments.in(fr"r.sectionid", sectionIds),
        fr"q.typeid = 'surveyEssayQuestion.1'",
        fr"q.name = $questionAssetName"
      ) ++
      fr"""order by "time", userid""" ++
      fr"offset $offset" ++
      limit.map(l => fr"limit $l").orEmpty)
      .query[String]
      .to[List]
end RsSurveyDao

final case class RsSurveyQuestionResponse(
  userId: Long,
  sectionId: Long,
  attemptId: String,
  time: Timestamp,
  contentAssetId: Long,
  contentEdgePath: String,
  questionAssetId: Long,
  response: String,
  surveyAssetId: Option[Long],
  surveyEdgePath: Option[String],
)

object RsSurveyQuestionResponse:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE surveyquestionresponse(
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  attemptId VARCHAR(36) NOT NULL,
         |  time TIMESTAMP NOT NULL,
         |  contentassetid INT8 NOT NULL REFERENCES asset(id),
         |  contentedgepath VARCHAR(32) NOT NULL,
         |  questionassetid INT8 NOT NULL REFERENCES asset(id),
         |  response VARCHAR(4096) NOT NULL,
         |  surveyassetid INT8 REFERENCES asset(id),
         |  surveyedgepath VARCHAR(32)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsSurveyQuestionResponse: EncodeJson[RsSurveyQuestionResponse] = EncodeJson(a =>
    Json(
      "userid"          := a.userId,
      "sectionid"       := a.sectionId,
      "attemptid"       := a.attemptId,
      "time"            := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "contentassetid"  := a.contentAssetId,
      "contentedgepath" := a.contentEdgePath,
      "questionassetid" := a.questionAssetId,
      "response"        := a.response,
      "surveyassetid"   := a.surveyAssetId,
      "surveyedgepath"  := a.surveyEdgePath
    )
  )
end RsSurveyQuestionResponse

final case class RsResponseCount(
  contentEdgePath: String,
  count: Long,
)

final case class RsChoiceCount(
  questionName: String,
  choice: String,
  count: Long,
)

final case class RsEssayCount(
  questionName: String,
  count: Long,
)
