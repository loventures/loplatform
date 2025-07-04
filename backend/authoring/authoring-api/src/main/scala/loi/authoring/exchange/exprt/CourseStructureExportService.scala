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

package loi.authoring.exchange.exprt

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.web.MediaType.TEXT_CSV_UTF_8_VALUE
import kantan.csv.HeaderEncoder
import loi.asset.assessment.model.*
import loi.asset.assessment.model.AssessmentType.Formative
import loi.asset.contentpart.BlockPart
import loi.asset.gradebook.GradebookCategory1
import loi.asset.root.model.Root
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group.GradebookCategory
import loi.authoring.edge.{EdgeService, Group, TraverseGraph, TraversedGraph}
import loi.authoring.exchange.exprt.CourseStructureExportService.*
import loi.authoring.node.AssetNodeService
import loi.authoring.render.RenderService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util.UUID

@Service
class CourseStructureExportService(
  renderService: RenderService,
)(implicit edgeService: EdgeService, assetNodeService: AssetNodeService):

  import loi.asset.course.model.Course

  import java.io.File

  private def getPoints(asset: Asset[?]): String =
    asset.pointsPossible.foldZ(_.toString)

  private def getLayout(singlePage: Boolean): String =
    if singlePage then "All questions on 1 page" else "1 question per page"

  private def getShowResults(immediateFeedback: Boolean): String =
    if immediateFeedback then "After each question" else "After assessment completion"

  private def getConfidenceLevel(shouldDisplayConfidenceIndicator: Boolean, singlePage: Boolean): String =
    if singlePage then "" else if shouldDisplayConfidenceIndicator then "Yes" else "No"

  private def getShowAnswersOnResults(shouldHideAnswerIfIncorrect: Boolean): String =
    if shouldHideAnswerIfIncorrect then "For correct responses" else "Always"

  private def getAssessmentType(assessmentType: AssessmentType): String = assessmentType match
    case AssessmentType.Formative => "Formative"
    case AssessmentType.Summative => "Summative"

  private def getNumberOfAttempts(maxAttempts: Long): String = maxAttempts match
    case 0 => "Unlimited"
    case n => n.toString

  private def getScoringOption(scoringOption: ScoringOption, maxAttempts: Long): String =
    if maxAttempts == 1 then ""
    else
      scoringOption match
        case ScoringOption.AverageScore              => "Average Score"
        case ScoringOption.FirstAttemptScore         => "First Attempt Score"
        case ScoringOption.FullCreditOnAnyCompletion => "Full Credit On Any Completion"
        case ScoringOption.HighestScore              => "Highest Score"
        case ScoringOption.MostRecentAttemptScore    => "Most Recent Attempt Score"

  private def getCountForCredit(asset: Asset[?]): String =
    if asset.isForCredit.isTrue then "Yes" else "No"

  private def getInstructions(instructions: BlockPart): String =
    instructions.renderedHtml | ""

  private def getGradebookCategory(
    asset: Asset[?],
    graph: TraversedGraph,
    categories: Map[UUID, Asset[GradebookCategory1]]
  ): String =
    graph
      .targetsInGroup(asset, GradebookCategory)
      .map(_.info.name)
      .flatMap(categories.get)
      .headOption
      .flatMap(_.title)
      .orZ

  private def getStructureRow(
    ws: AttachedReadWorkspace,
    asset: Asset[?],
    info: AssetInfo,
    graph: TraversedGraph,
    categories: Map[UUID, Asset[GradebookCategory1]]
  ): CourseStructureRow =
    asset match
      case Assessment.Asset(assessment)         =>
        val data        = assessment.data
        val maxAttempts = data.maxAttempts | 0L
        CourseStructureRow(
          info.level1,
          info.level2,
          info.level3,
          info.level4,
          info.assetType,
          teaches = info.teaches,
          assesses = info.assesses,
          assessmentTemplate = "",
          points = getPoints(asset),
          layout = getLayout(data.singlePage),
          showResults = getShowResults(data.immediateFeedback),
          confidenceLevel = getConfidenceLevel(data.shouldDisplayConfidenceIndicator.get, data.singlePage),
          showAnswerOnResults = getShowAnswersOnResults(data.shouldHideAnswerIfIncorrect),
          assessmentType = getAssessmentType(data.assessmentType),
          numberOfAttempts = getNumberOfAttempts(maxAttempts),
          timeLimit = data.maxMinutes.cata(_.toString, ""),
          scoringOption = getScoringOption(data.scoringOption | ScoringOption.MostRecentAttemptScore, maxAttempts),
          numberOfQuestions = "",
          countForCredit = getCountForCredit(asset),
          gradebookCategory = getGradebookCategory(asset, graph, categories),
          instructions = getInstructions(renderService.render(ws, assessment).data.instructions)
        )
      case Checkpoint.Asset(checkpoint)         =>
        val data        = checkpoint.data
        val maxAttempts = 0
        CourseStructureRow(
          info.level1,
          info.level2,
          info.level3,
          info.level4,
          info.assetType,
          teaches = info.teaches,
          assesses = info.assesses,
          assessmentTemplate = "",
          points = getPoints(asset),
          layout = getLayout(true),
          showResults = getShowResults(false),
          confidenceLevel = getConfidenceLevel(false, true),
          showAnswerOnResults = getShowAnswersOnResults(true),
          assessmentType = getAssessmentType(Formative),
          numberOfAttempts = getNumberOfAttempts(maxAttempts),
          timeLimit = "",
          scoringOption = getScoringOption(ScoringOption.MostRecentAttemptScore, maxAttempts),
          numberOfQuestions = "",
          countForCredit = getCountForCredit(asset),
          "",
          instructions = getInstructions(renderService.render(ws, checkpoint).data.instructions)
        )
      case Diagnostic.Asset(diagnostic)         =>
        val data       = diagnostic.data
        val singlePage = data.singlePage
        CourseStructureRow(
          info.level1,
          info.level2,
          info.level3,
          info.level4,
          info.assetType,
          teaches = info.teaches,
          assesses = info.assesses,
          assessmentTemplate = "",
          points = getPoints(asset),
          layout = getLayout(singlePage),
          showResults = getShowResults(data.immediateFeedback),
          confidenceLevel = getConfidenceLevel(data.shouldDisplayConfidenceIndicator.get, data.singlePage),
          showAnswerOnResults = getShowAnswersOnResults(data.shouldHideAnswerIfIncorrect),
          assessmentType = "",
          numberOfAttempts = "",
          timeLimit = data.maxMinutes.cata(_.toString, ""),
          scoringOption = "",
          numberOfQuestions = "",
          countForCredit = getCountForCredit(asset),
          gradebookCategory = getGradebookCategory(asset, graph, categories),
          instructions = getInstructions(renderService.render(ws, diagnostic).data.instructions)
        )
      case PoolAssessment.Asset(poolAssessment) =>
        val data        = poolAssessment.data
        val singlePage  = data.singlePage
        val maxAttempts = data.maxAttempts | 0L
        CourseStructureRow(
          info.level1,
          info.level2,
          info.level3,
          info.level4,
          info.assetType,
          teaches = info.teaches,
          assesses = info.assesses,
          assessmentTemplate = "",
          points = getPoints(asset),
          layout = getLayout(singlePage),
          showResults = getShowResults(data.immediateFeedback),
          confidenceLevel = getConfidenceLevel(data.shouldDisplayConfidenceIndicator.get, data.singlePage),
          showAnswerOnResults = getShowAnswersOnResults(data.shouldHideAnswerIfIncorrect),
          assessmentType = getAssessmentType(data.assessmentType),
          numberOfAttempts = getNumberOfAttempts(maxAttempts),
          timeLimit = data.maxMinutes.cata(_.toString, ""),
          scoringOption = getScoringOption(data.scoringOption | ScoringOption.MostRecentAttemptScore, maxAttempts),
          numberOfQuestions = s"${data.numberOfQuestionsForAssessment}",
          countForCredit = getCountForCredit(asset),
          gradebookCategory = getGradebookCategory(asset, graph, categories),
          instructions = getInstructions(renderService.render(ws, poolAssessment).data.instructions)
        )
      case _                                    =>
        CourseStructureRow(
          info.level1,
          info.level2,
          info.level3,
          info.level4,
          info.assetType,
          teaches = info.teaches,
          assesses = info.assesses,
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          gradebookCategory = getGradebookCategory(asset, graph, categories),
          "" // TODO: We ought to be able to include instructions/instructionsBlock here
        )

  def exportStructure(ws: AttachedReadWorkspace): UploadInfo =

    val root   = assetNodeService.loadA[Root](ws).byName(ws.rootName).get
    val course = assetNodeService.loadA[Course](ws).byName(ws.homeName).get

    val graph = edgeService.stravaigeOutGraphs(
      List(
        TraverseGraph
          .fromSource(course.info.name)                                                     // units, modules
          .traverse(Group.Elements)                                                         // modules, content and lessons
          .traverse(Group.Elements, Group.Teaches, Group.Assesses, Group.GradebookCategory) // content and lessons
          .traverse(Group.Elements, Group.Teaches, Group.Assesses, Group.GradebookCategory)
          .traverse(Group.Elements, Group.Teaches, Group.Assesses, Group.GradebookCategory)
          .traverse(Group.Teaches, Group.Assesses, Group.GradebookCategory),
        TraverseGraph
          .fromSource(course.info.name)
          .traverse(Group.GradebookCategories)
          .noFurther,
        CompetencyUtil.rootCompetenciesGraph(root)
      ),
      ws
    )

    val rootCompetencies    = CompetencyUtil.rootCompetencies(root, graph)
    val gradebookCategories =
      graph.targetsInGroupOfType[GradebookCategory1](course, Group.GradebookCategories).groupUniqBy(_.info.name)

    val file = File.createTempFile("CourseStructure", ".csv")

    file.writeCsvWithBom[CourseStructureRow] { csv =>
      def loop(asset: Asset[?], level: Int): Unit =
        if level > 0 then
          val assetInfo    = AssetInfo(asset, level, graph, rootCompetencies, ws)
          val structureRow = getStructureRow(ws, asset, assetInfo, graph, gradebookCategories)
          csv.write(structureRow)
        graph.targetsInGroup(asset, Group.Elements).foreach(loop(_, level + 1))

      loop(course, 0)
    }

    new UploadInfo(ExportFile.cleanFilename(s"${ws.projectInfo.name.trim}.csv"), TEXT_CSV_UTF_8_VALUE, file, true)
  end exportStructure
end CourseStructureExportService

object CourseStructureExportService:

  import AssetTypeId.*

  final case class AssetInfo(
    level1: String,
    level2: String,
    level3: String,
    level4: String,
    assetType: String,
    teaches: String,
    assesses: String,
  )

  object AssetInfo:
    def apply(
      asset: Asset[?],
      level: Int,
      graph: TraversedGraph,
      competencies: Map[UUID, Asset[?]],
      workspace: ReadWorkspace
    ): AssetInfo =
      def getCompetencies(group: Group): String =
        CompetencyUtil
          .assetCompetencies(asset, group, graph, competencies)
          .flatMap(_.title)
          .map(_.trim)
          .mkString("\n")

      val assetTitle = (asset.title | "Untitled").trim
      AssetInfo(
        level1 = (level == 1) ?? assetTitle,
        level2 = (level == 2) ?? assetTitle,
        level3 = (level == 3) ?? assetTitle,
        level4 = (level == 4) ?? assetTitle,
        assetType = assetTypeNameMap(asset.info.typeId),
        teaches = getCompetencies(Group.Teaches),
        assesses = getCompetencies(Group.Assesses),
      )
    end apply

    implicit val assetInfoHeaderEncoder: HeaderEncoder[AssetInfo] =
      HeaderEncoder.caseEncoder(
        "Level One",
        "Level Two",
        "Level Three",
        "Level Four",
        "LO Asset Type",
        "Teaches",
        "Assesses"
      )(
        ArgoExtras.unapply
      )
  end AssetInfo

  private final case class CourseStructureRow(
    level1: String,
    level2: String,
    level3: String,
    level4: String,
    assetType: String,
    teaches: String,
    assesses: String,
    assessmentTemplate: String,
    points: String,
    layout: String,
    showResults: String,
    confidenceLevel: String,
    showAnswerOnResults: String,
    assessmentType: String,
    numberOfAttempts: String,
    timeLimit: String,
    scoringOption: String,
    numberOfQuestions: String,
    countForCredit: String,
    gradebookCategory: String,
    instructions: String
  )

  private object CourseStructureRow:
    implicit val assessmentSettingsHeaderEncoder: HeaderEncoder[CourseStructureRow] =
      HeaderEncoder.caseEncoder(
        "Level One",
        "Level Two",
        "Level Three",
        "Level Four",
        "LO Asset Type",
        "Teaches",
        "Assesses",
        "Assessment Template",
        "Points",
        "Layout",
        "Show results",
        "Confidence Level",
        "Show answer on results",
        "Type",
        "Number of attempts",
        "Time Limit",
        "Scoring Option",
        "Number of questions (Pool only)",
        "Count for Credit",
        "Gradebook Category",
        "Instructions"
      )(ArgoExtras.unapply)
  end CourseStructureRow

  final val assetTypeNameMap = Map[AssetTypeId, String](
    Assessment            -> "Assessment",
    Assignment            -> "Assignment",
    Checkpoint            -> "Checkpoint",
    Course                -> "Course",
    Diagnostic            -> "Diagnostic",
    Discussion            -> "Discussion Board",
    FileBundle            -> "File Bundle",
    Html                  -> "HTML Page",
    ObservationAssessment -> "Observation Assessment",
    CourseLink            -> "Course Link",
    Lesson                -> "Lesson",
    Lti                   -> "LTI Activity",
    Module                -> "Module",
    PoolAssessment        -> "Pool Assessment",
    Resource1             -> "Legacy Activity",
    Scorm                 -> "SCORM Activity",
    Unit                  -> "Unit",
  ) withDefaultValue "Unknown"
end CourseStructureExportService
