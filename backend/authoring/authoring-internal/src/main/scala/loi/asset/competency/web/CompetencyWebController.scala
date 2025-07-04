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

package loi.asset.competency.web

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebRequest}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import kantan.csv.HeaderEncoder
import loi.asset.competency.model.{CompetencySet, Level1Competency, Level2Competency, Level3Competency}
import loi.asset.competency.web.CompetencyWebController.questionTypeNameMap
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.factory.AssetTypeId.*
import loi.authoring.edge.Group.*
import loi.authoring.edge.*
import loi.authoring.exchange.exprt.{CompetencyUtil, CourseStructureExportService}
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scaloi.json.ArgoExtras

@Component
@Controller(root = true, value = "assets/competency")
@Secured(Array(classOf[AccessAuthoringAppRight]))
class CompetencyWebController(
  ci: ComponentInstance,
  authoringWebUtils: AuthoringWebUtils,
)(implicit val edgeService: EdgeService)
    extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "assets/{branch}/competencySet.1/{name}/structure", method = Method.GET)
  def exportCompetencySet(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    request: WebRequest,
  ): FileResponse[?] = exportCompetencySetImpl(branchId, name, None, request)

  @RequestMapping(path = "assets/{branch}/commits/{commit}/competencySet.1/{name}/structure", method = Method.GET)
  def exportCompetencySet(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @PathVariable("commit") commitId: Long,
    request: WebRequest,
  ): FileResponse[?] = exportCompetencySetImpl(branchId, name, Some(commitId), request)

  private def exportCompetencySetImpl(
    branchId: Long,
    name: String,
    commitOpt: Option[Long],
    request: WebRequest
  ): FileResponse[?] =
    val workspace     = authoringWebUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val competencySet = authoringWebUtils.nodeOrThrow404Typed[CompetencySet](workspace, name)

    val exportFile = ExportFile.create(s"${competencySet.data.title.trim}.csv", MediaType.CSV_UTF_8, request)

    val graph = edgeService.stravaigeOutGraph(
      TraverseGraph
        .fromSource(competencySet.info.name)
        .traverse(Level1Competencies)
        .traverse(Level2Competencies)
        .traverse(Level3Competencies),
      workspace
    )

    exportFile.file.writeCsvWithBom[CompetencyRow] { csv =>
      graph
        .targetsInGroupOfType[Level1Competency](competencySet, Level1Competencies)
        .filterNot(_.data.archived) foreach { l1 =>
        csv.write(CompetencyRow(l1.data.title.trim, "", ""))
        graph.targetsInGroupOfType[Level2Competency](l1, Level2Competencies).filterNot(_.data.archived) foreach { l2 =>
          csv.write(CompetencyRow("", l2.data.title.trim, ""))
          graph.targetsInGroupOfType[Level3Competency](l2, Level3Competencies).filterNot(_.data.archived) foreach {
            l3 =>
              csv.write(CompetencyRow("", "", l3.data.title.trim))
          }
        }
      }
    }

    FileResponse(exportFile.toFileInfo)
  end exportCompetencySetImpl

  @RequestMapping(path = "assets/{branch}/competencySet.1/{name}/alignment", method = Method.GET)
  def exportContentAlignment(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    request: WebRequest,
  ): FileResponse[?] = exportContentAlignmentImpl(branchId, name, None, request)

  @RequestMapping(path = "assets/{branch}/commits/{commit}/competencySet.1/{name}/alignment", method = Method.GET)
  def exportContentAlignment(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @PathVariable("commit") commitId: Long,
    request: WebRequest,
  ): FileResponse[?] = exportContentAlignmentImpl(branchId, name, Some(commitId), request)

  private def exportContentAlignmentImpl(
    branchId: Long,
    name: String,
    commitOpt: Option[Long],
    request: WebRequest
  ): FileResponse[?] =
    val workspace     = authoringWebUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val competencySet = authoringWebUtils.nodeOrThrow404Typed[CompetencySet](workspace, name)
    val course        = authoringWebUtils.nodeOrThrow422(workspace, workspace.homeName)

    val exportFile =
      ExportFile.create(s"${competencySet.data.title.trim} - Content Alignment.csv", MediaType.CSV_UTF_8, request)

    val graph = edgeService.stravaigeOutGraphs(
      List(
        TraverseGraph
          .fromSource(course.info.name)
          .traverse(Elements)                    // units, modules
          .traverse(Elements, Teaches, Assesses) // modules, content and lessons
          .traverse(Elements, Teaches, Assesses) // content and lessons
          .traverse(Elements, Teaches, Assesses)
          .traverse(Teaches, Assesses),
        CompetencyUtil.csCompetenciesGraph(competencySet)
      ),
      workspace
    )

    val competencies = CompetencyUtil.csCompetencies(competencySet, graph)

    exportFile.file.writeCsvWithBom[CourseStructureExportService.AssetInfo] { csv =>
      def loop(source: Asset[?], level: Int): Unit =
        graph.targetsInGroup(source, Elements) foreach { content =>
          val info = CourseStructureExportService.AssetInfo(content, level, graph, competencies, workspace)
          csv.write(info)
          loop(content, 1 + level)
        }
      loop(course, 1)
    }

    FileResponse(exportFile.toFileInfo)
  end exportContentAlignmentImpl

  @RequestMapping(path = "assets/{branch}/competencySet.1/{name}/alignmentPlus", method = Method.GET)
  def exportContentAndQuestionAlignment(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    request: WebRequest,
  ): FileResponse[?] = exportContentAndQuestionAlignmentImpr(branchId, name, None, request)

  @RequestMapping(path = "assets/{branch}/commits/{commit}/competencySet.1/{name}/alignmentPlus", method = Method.GET)
  def exportContentAndQuestionAlignment(
    @PathVariable("branch") branchId: Long,
    @PathVariable("name") name: String,
    @PathVariable("commit") commitId: Long,
    request: WebRequest,
  ): FileResponse[?] = exportContentAndQuestionAlignmentImpr(branchId, name, Some(commitId), request)

  private def exportContentAndQuestionAlignmentImpr(
    branchId: Long,
    name: String,
    commitOpt: Option[Long],
    request: WebRequest,
  ): FileResponse[?] =
    val workspace     = authoringWebUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)
    val competencySet = authoringWebUtils.nodeOrThrow404Typed[CompetencySet](workspace, name)
    val course        = authoringWebUtils.nodeOrThrow422(workspace, workspace.homeName)

    val exportFile =
      ExportFile.create(
        s"${competencySet.data.title.trim} - Content and Question Alignment.csv",
        MediaType.CSV_UTF_8,
        request
      )

    val graph = edgeService.stravaigeOutGraphs(
      List(
        TraverseGraph
          .fromSource(course.info.name)
          .traverse(Elements) // units, modules
          .traverse(Elements, Questions, Teaches, Assesses)
          .traverse(Elements, Questions, Teaches, Assesses)
          .traverse(Elements, Questions, Teaches, Assesses)
          .traverse(Questions, Teaches, Assesses)
          .traverse(Assesses),
        CompetencyUtil.csCompetenciesGraph(competencySet)
      ),
      workspace
    )

    val competencies = CompetencyUtil.csCompetencies(competencySet, graph)

    exportFile.file.writeCsvWithBom[CompetencyAlignmentRow] { csv =>
      def loop(source: Asset[?], level: Int): Unit =
        graph.targetsInGroup(source, Elements) foreach { content =>
          val info = CourseStructureExportService.AssetInfo(content, level, graph, competencies, workspace)
          csv.write(CompetencyAlignmentRow(info))
          graph.targetsInGroup(content, Questions).zipWithIndex foreach { case (question, index) =>
            val assesses =
              CompetencyUtil
                .assetCompetencies(question, Group.Assesses, graph, competencies)
                .flatMap(_.title)
                .map(_.trim)
                .mkString("\n")
            csv.write(
              CompetencyAlignmentRow(
                level1 = "",
                level2 = "",
                level3 = "",
                question = s"${index + 1}",
                assetType = questionTypeNameMap(question.info.typeId),
                teaches = "",
                assesses = assesses,
              )
            )

          }
          loop(content, 1 + level)
        }

      loop(course, 1)
    }

    FileResponse(exportFile.toFileInfo)
  end exportContentAndQuestionAlignmentImpr
end CompetencyWebController

object CompetencyWebController:
  final val questionTypeNameMap = Map[AssetTypeId, String](
    BinDropQuestion        -> "Bin Drop",
    EssayQuestion          -> "Essay",
    FillInTheBlankQuestion -> "Fill in the Blank",
    HotspotQuestion        -> "Hotspot",
    MatchingQuestion       -> "Matching",
    MultipleChoiceQuestion -> "Multiple Choice",
    MultipleSelectQuestion -> "Multiple Select",
    OrderingQuestion       -> "Ordering",
    ShortAnswerQuestion    -> "Short Answer",
    TrueFalseQuestion      -> "True/False",
  ) withDefaultValue "Unknown"
end CompetencyWebController

private final case class CompetencyRow(
  l1: String,
  l2: String,
  l3: String
)

private object CompetencyRow:
  implicit val competencyRowHeaderEncoder: HeaderEncoder[CompetencyRow] =
    HeaderEncoder.caseEncoder(
      "Level One Competency",
      "Level Two Competency",
      "Level Three Competency"
    )(ArgoExtras.unapply)

private final case class CompetencyAlignmentRow(
  level1: String,
  level2: String,
  level3: String,
  question: String,
  assetType: String,
  teaches: String,
  assesses: String,
)

private object CompetencyAlignmentRow:
  def apply(info: CourseStructureExportService.AssetInfo): CompetencyAlignmentRow =
    CompetencyAlignmentRow(
      level1 = info.level1,
      level2 = info.level2,
      level3 = info.level3,
      question = "",
      assetType = info.assetType,
      teaches = info.teaches,
      assesses = info.assesses
    )

  implicit val assetInfoHeaderEncoder: HeaderEncoder[CompetencyAlignmentRow] =
    HeaderEncoder.caseEncoder(
      "Level One",
      "Level Two",
      "Level Three",
      "Question",
      "LO Asset Type",
      "Teaches",
      "Assesses"
    )(ArgoExtras.unapply)
end CompetencyAlignmentRow
