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

import argonaut.Argonaut.*
import argonaut.DecodeJsonCats.*
import argonaut.*
import cats.data.{NonEmptyList, ValidatedNel}
import cats.effect.unsafe.implicits.global
import cats.instances.list.*
import cats.syntax.either.*
import cats.syntax.list.*
import cats.syntax.option.*
import cats.syntax.traverse.*
import cats.syntax.validated.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.exception.{BusinessRuleViolationException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.Secured
import doobie.implicits.*
import loi.asset.question.{EssayQuestion, LikertScaleQuestion1, MultipleChoiceQuestion, RatingScaleQuestion1}
import loi.asset.survey.{SurveyChoiceQuestion1, SurveyEssayQuestion1}
import loi.authoring.asset.Asset
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringApiWebUtils
import loi.cp.analytics.redshift.{QuestionStats, RedshiftSchemaService, RsSurveyService}
import loi.cp.content.{ContentAccessService, CourseContent, CourseContentService}
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.offering.ProjectOfferingService
import loi.cp.reference.EdgePath
import loi.cp.survey.SurveyDto.MaxResponseLength
import loi.db.Redshift
import loi.doobie.log.*
import scaloi.syntax.string.*

import java.util.UUID

@Component
@Controller(root = true, value = "survey-web-controller")
class SurveyWebController(
  authoringApiWebUtils: AuthoringApiWebUtils,
  ci: ComponentInstance,
  contentAccessService: ContentAccessService,
  courseContentService: CourseContentService,
  projectOfferingService: ProjectOfferingService,
  surveyContentService: SurveyContentService,
  surveySubmissionService: SurveySubmissionService,
  redshiftSchemaService: RedshiftSchemaService,
  rsSurveyService: RsSurveyService,
  userDto: => UserDTO,
) extends BaseComponent(ci)
    with ApiRootComponent:
  import SurveyWebController.*

  @RequestMapping(path = "lwc/{section}/contents/{edgePath}/survey", method = Method.GET)
  def loadSurvey(
    @PathVariable("section") sectionId: Long,
    @PathVariable("edgePath") edgePath: EdgePath
  ): ArgoBody[SurveyDto] =

    val (_, _, surveyTree) = loadSurveyData(sectionId, edgePath)
    ArgoBody(SurveyDto(surveyTree))

  @RequestMapping(path = "lwc/{section}/contents/{edgePath}/survey", method = Method.POST)
  def submitSurvey(
    @PathVariable("section") sectionId: Long,
    @PathVariable("edgePath") edgePath: EdgePath,
    @RequestBody responseDtoJson: ArgoBody[SurveyResponseDto]
  ): Unit =

    val (section, content, surveyTree) = loadSurveyData(sectionId, edgePath)
    val responseDto                    = responseDtoJson.decode_!.get

    val responses = responseDto.responses
      .traverse(validate(surveyTree))
      .valueOr(errors => throw new BusinessRuleViolationException(errors.toList.mkString("\n")))

    surveySubmissionService
      .submitSurvey(section, content, surveyTree.surveyAsset, surveyTree.surveyEdgePath, responses, userDto)
      .valueOr(err => throw new BusinessRuleViolationException(s"too many survey submissions: ${err.numSubmissions}"))
  end submitSurvey

  // in courseware instead of authoring because of the looking up of sections and edgepaths
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/{branch}/survey/stats", method = Method.GET)
  def loadSurveyStats(
    @PathVariable("branch") branchId: Long,
  ): ArgoBody[SurveyResponseStats] =
    val branch = authoringApiWebUtils.branchOrFakeBranchOrThrow404(branchId)

    // For a course in development, where we would expect to see the highest level
    // of authoring traffic, there will be no offering/sections and so this will
    // never hit Redshift.
    val surveyStats = for
      schemaName <- redshiftSchemaService.queryEtlSchemaNames().headOption
      offering   <- projectOfferingService.getOfferingComponentForBranch(branch)
      sectionIds <- projectOfferingService.getCourseSections(offering).map(_.id).toNel
      contents    = courseContentService.getCourseContents(offering).get
    yield

      val xa            = Redshift.buildTransactor(schemaName)
      val io            = rsSurveyService.loadResponseStats(sectionIds)
      val responseStats = io.transact(xa).unsafeRunSync()
      val assetStats    = for
        stat    <- responseStats
        content <- contents.get(EdgePath.parse(stat.edgePath))
      yield content.asset.info.name -> stat.responseCounts

      SurveyResponseStats(sectionIds.toList, assetStats.toMap)

    // surveyStats is None if there is no schema name, no offering or no sections
    ArgoBody(surveyStats.getOrElse(SurveyResponseStats(Nil, Map.empty)))
  end loadSurveyStats

  // in courseware instead of authoring because of the looking up of sections and edgepaths
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/{branch}/nodes/{name}/survey/stats", method = Method.GET)
  def loadQuestionStats(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") assetName: UUID,
  ): ArgoBody[SurveyQuestionStats] =

    val branch = authoringApiWebUtils.branchOrFakeBranchOrThrow404(branchId)

    val surveyStats = for
      schemaName <- redshiftSchemaService.queryEtlSchemaNames().headOption
      offering   <- projectOfferingService.getOfferingComponentForBranch(branch)
      sectionIds <- projectOfferingService.getCourseSections(offering).map(_.id).toNel
      contents    = courseContentService.getCourseContents(offering).get
      edgePaths  <- contents.get(assetName).map(_.edgePath.toString).toList.toNel
    yield

      val xa            = Redshift.buildTransactor(schemaName)
      val io            = rsSurveyService.loadQuestionStats(sectionIds, edgePaths, Some(10))
      val questionStats = io.transact(xa).unsafeRunSync()

      SurveyQuestionStats(sectionIds.toList, edgePaths.toList, questionStats)

    // surveyStats is None if there is no schema name, no offering, no sections or no edgepaths
    ArgoBody(surveyStats.getOrElse(SurveyQuestionStats(Nil, Nil, Nil)))
  end loadQuestionStats

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/surveyEssayResponses", method = Method.GET)
  def loadEssayResponses(
    @QueryParam("questionId") questionIdJson: ArgoBody[SurveyQuestionId],
    @QueryParam("offset") offset: Int,
    @QueryParam("limit") limit: Int
  ): ArgoBody[List[String]] =

    val questionId = questionIdJson.decode_!.get

    val responses =
      for schemaName <- redshiftSchemaService.queryEtlSchemaNames().headOption
      yield

        val xa = Redshift.buildTransactor(schemaName)
        val io = rsSurveyService.loadEssayResponses(
          questionId.questionName,
          questionId.sectionIds,
          questionId.edgePaths.map(_.toString),
          offset,
          Some(limit)
        )
        io.transact(xa).unsafeRunSync()

    // responses is None if there is no schema
    ArgoBody(responses.getOrElse(Nil))
  end loadEssayResponses

  private def loadSurveyData(
    sectionId: Long,
    edgePath: EdgePath
  ): (LightweightCourse, CourseContent, SurveyTree) =

    // the .get because we are soooooo good that the service is making exceptions in SRS terms
    // and mapping them to ErrorResponse is for another day
    val (section, content) = contentAccessService.readContent(sectionId, edgePath, userDto).get
    val ws                 = section.getWorkspace

    val surveyTree = surveyContentService
      .loadSurveyTrees(ws, content :: Nil)
      .headOption
      .getOrElse(throw new ResourceNotFoundException(s"content ${content.edgePath} has no survey"))

    (section, content, surveyTree)
  end loadSurveyData

  private def validate(surveyTree: SurveyTree)(
    responseDto: SurveyQuestionResponseDto
  ): ValidatedNel[String, SurveyQuestionResponseDto] =

    surveyTree.questions
      .map(_.asset)
      .find(_.info.id == responseDto.questionAssetId)
      .toValidNel(
        s"question ${responseDto.questionAssetId} is not a question on survey ${surveyTree.surveyAsset.info.id}"
      )
      .andThen {
        case MultipleChoiceQuestion.Asset(asset) =>
          responseDto.response.toLong_?
            .flatMap(index =>
              asset.data.questionContent.choices
                .find(choice => choice.index == index)
                .map(choice => choice.choiceContent.map(_.html).getOrElse("").take(MaxResponseLength))
            )
            .toValidNel(s"response for ${asset.info.typeId} ${responseDto.questionAssetId} must be a choice index")
            .map(choiceText => responseDto.copy(response = choiceText))
        case LikertScaleQuestion1.Asset(asset)   =>
          responseDto.response.toLong_?
            .filter(rating => rating >= 0 && rating <= 4)
            .toValidNel(s"response for ${asset.info.typeId} ${asset.info.id} must be in [0, 4]")
            .map(_ => responseDto)
        case RatingScaleQuestion1.Asset(asset)   =>
          responseDto.response.toLong_?
            .filter(rating => rating >= 1 && rating <= asset.data.max)
            .toValidNel(s"response for ${asset.info.typeId} ${asset.info.id} must be in [1, ${asset.data.max}]")
            .map(_ => responseDto)
        case EssayQuestion.Asset(_)              =>
          if responseDto.response.length > MaxResponseLength then
            "Essay responses must be 4096 characters or less".invalidNel
          else responseDto.validNel
        case SurveyChoiceQuestion1.Asset(asset)  =>
          asset.data.choices
            .find(choice => choice.value == responseDto.response)
            .toValidNel(s"response for ${asset.info.typeId} ${responseDto.questionAssetId} must be a choice value")
            .map(_ => responseDto)
        case SurveyEssayQuestion1.Asset(_)       =>
          if responseDto.response.length > MaxResponseLength then
            "Essay responses must be 4096 characters or less".invalidNel
          else responseDto.validNel
        case a                                   =>
          throw new RuntimeException(
            s"question ${a.info.id} (${a.info.typeId}) is not a supported survey question asset type"
          )
      }
end SurveyWebController

object SurveyWebController:
  private final implicit val log: org.log4s.Logger = org.log4s.getLogger

final case class SurveyDto(
  title: String,
  questions: List[Asset[?]],
  disabled: Boolean,
  inline: Boolean,
  programmatic: Boolean,
)

object SurveyDto:

  val MaxResponseLength = 4096

  def apply(surveyTree: SurveyTree): SurveyDto =
    SurveyDto(
      surveyTree.surveyAsset.data.title,
      surveyTree.questions.map(_.asset),
      surveyTree.surveyAsset.data.disabled,
      surveyTree.surveyAsset.data.inline,
      surveyTree.surveyAsset.data.programmatic,
    )

  import com.learningobjects.cpxp.scala.json.JacksonCodecs.universal.*

  implicit val encodeJsonForSurveyDto: EncodeJson[SurveyDto] = EncodeJson(dto =>
    Json(
      "title"        := dto.title,
      "questions"    := dto.questions,
      "disabled"     := dto.disabled,
      "inline"       := dto.inline,
      "programmatic" := dto.programmatic
    )
  )
end SurveyDto

final case class SurveyResponseStats(
  sectionIds: List[Long],
  responseStats: Map[UUID, Long],
)

object SurveyResponseStats:
  import scaloi.json.ArgoExtras.encodeJsonKeyForUuid

  implicit val encodeJsonForSurveyResponseStats: EncodeJson[SurveyResponseStats] = EncodeJson(a =>
    Json(
      "sectionIds"    := a.sectionIds,
      "responseStats" := a.responseStats,
    )
  )

final case class SurveyQuestionStats(
  sectionIds: List[Long],
  edgePaths: List[String],
  questionStats: List[QuestionStats]
)

object SurveyQuestionStats:
  implicit val encodeJsonForSurveyQuestionStats: EncodeJson[SurveyQuestionStats] = EncodeJson(a =>
    Json(
      "sectionIds"    := a.sectionIds,
      "edgePaths"     := a.edgePaths,
      "questionStats" := a.questionStats,
    )
  )

// due to shallow branch copy, an asset name is not unique enough to identify
// one set of assetnodes on a domain. The pair of asset name and branch id is.
// But the analytics are not stored under asset name and branch id, they are
// stored under section id and edgepath. One can obtain the (sectionIds,
// edgepaths) from an (assetName, branchId) by using `.loadQuestionStats`
final case class SurveyQuestionId(
  questionName: UUID,
  sectionIds: NonEmptyList[Long],
  edgePaths: NonEmptyList[EdgePath],
)

object SurveyQuestionId:
  implicit val decodeJsonForSurveyQuestionId: DecodeJson[SurveyQuestionId] =
    DecodeJson.jdecode3L(SurveyQuestionId.apply)(
      "questionName",
      "sectionIds",
      "edgePaths",
    )
