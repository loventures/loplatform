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

package loi.authoring.gradebook

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, FileResponse, Method, WebRequest}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import kantan.csv.HeaderEncoder
import loi.asset.course.model.Course
import loi.asset.gradebook.GradebookCategory1
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.edge.Group.*
import loi.authoring.edge.*
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scalaz.std.list.*
import scalaz.std.math.bigDecimal.*
import scalaz.std.option.*
import scalaz.syntax.foldable.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util.UUID

@Component
@Controller(root = true, value = "authoring/gradebook")
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[gradebook] class GradebookWebController(
  val componentInstance: ComponentInstance,
  authoringWebUtils: AuthoringWebUtils,
)(implicit val edgeService: EdgeService)
    extends ApiRootComponent
    with ComponentImplementation:
  import GradebookWebController.*

  @RequestMapping(path = "authoring/branches/{branch}/gradebook/export", method = Method.GET)
  def exportGradebook(
    @PathVariable("branch") branchId: Long,
    request: WebRequest,
  ): FileResponse[?] = exportGradebookImpl(branchId, None, request)

  @RequestMapping(path = "authoring/branches/{branch}/commits/{commit}/gradebook/export", method = Method.GET)
  def exportGradebook(
    @PathVariable("branch") branchId: Long,
    @PathVariable("commit") commitId: Long,
    request: WebRequest,
  ): FileResponse[?] = exportGradebookImpl(branchId, Some(commitId), request)

  private def exportGradebookImpl(
    branchId: Long,
    commitOpt: Option[Long],
    request: WebRequest,
  ): FileResponse[?] =
    val workspace = authoringWebUtils.workspaceOptionallyAtCommitOrThrow404(branchId, commitOpt, cache = false)

    val graph = edgeService.stravaigeOutGraphs(
      List(
        FromSources(Seq(workspace.homeName))
          .traverse(GradebookCategories)
          .noFurther,
        FromSources(Seq(workspace.homeName))
          .traverse(Elements)
          .traverse(Elements, GradebookCategory)
          .traverse(Elements, GradebookCategory)
          .traverse(Elements, GradebookCategory)
          .traverse(GradebookCategory),
      ),
      workspace
    )

    val course              = graph.node[Course](workspace.homeName).get
    val categories          = graph.targetsInGroupOfType[GradebookCategory1](course, GradebookCategories)
    val supportedCategories = categories.map(_.info.name).toSet

    val assignments =
      val builder = List.newBuilder[GradebookAssignment]

      def loop(asset: Asset[?], module: Option[Asset[Module]], lesson: Option[Asset[Lesson]]): Unit =
        // the "definition" of hasGradeBookEntry is that both of these are defined
        // see CourseContent#gradingPolicy, ContentDtoUtilsImpl#toDto
        for
          isForCredit    <- asset.isForCredit
          pointsPossible <- asset.pointsPossible
          edgedCategory   = graph.targetsInGroupOfType[GradebookCategory1](asset, GradebookCategory).map(_.info.name)
          category        = edgedCategory.find(supportedCategories.contains)
        do builder += GradebookAssignment(module, lesson, asset, isForCredit, pointsPossible, category)
        graph
          .targetsInGroup(asset, Elements)
          .foreach(asset => loop(asset, module || asset.filter[Module], lesson || asset.filter[Lesson]))
      end loop
      loop(course, None, None)

      builder.result()
    end assignments

    val exportFile = ExportFile.create(s"${course.data.title.trim} - Gradebook.csv", MediaType.CSV_UTF_8, request)

    exportFile.file.writeCsvWithBom[GradebookRow] { csv =>
      if categories.nonEmpty then
        val categoryWeightTotal = categories.toList.foldMap(_.data.weight)

        val categoryPointTotals =
          assignments.filter(_.isForCredit).flatMap(ass => ass.category.strengthR(ass.pointsPossible)).sumToMap

        def loop(category: Option[Asset[GradebookCategory1]]): Unit =
          val categoryPoints = category.flatMap(cat => categoryPointTotals.get(cat.info.name))
          assignments.filter(_.category == category.map(_.info.name)) foreach { assignment =>
            val percentInCategory =
              categoryPoints.map(points => assignment.pointsPossible / points).when(assignment.isForCredit)
            val percentInCourse   =
              category.flatMap(cat =>
                if categoryWeightTotal.intValue == 0 then None
                else percentInCategory.map(_ * cat.data.weight / categoryWeightTotal)
              )

            csv.write(
              GradebookRow(
                category = category.map(_.data.title) || "Uncategorized".some,
                categoryWeight = category.map(_.data.weight.autoscaled),
                categoryPoints = categoryPoints.map(_.autoscaled),
                module = assignment.module.flatMap(_.title),
                lesson = assignment.lesson.flatMap(_.title),
                assignment = assignment.asset.title | "Untitled",
                countsForCredit = assignment.isForCredit,
                assignmentPoints = assignment.pointsPossible.autoscaled,
                assignmentCategoryPercent = percentInCategory.map(_.percent),
                assignmentCoursePercent = percentInCourse.map(_.percent),
              )
            )
          }
        end loop
        (categories.map(_.some) :+ None).foreach(loop)
      else
        val coursePointsTotal = assignments.filter(_.isForCredit).foldMap(_.pointsPossible)
        assignments foreach { assignment =>
          val percentInCourse =
            assignment.isForCredit.option(assignment.pointsPossible / coursePointsTotal)
          csv.write(
            GradebookRow(
              category = None,
              categoryWeight = None,
              categoryPoints = None,
              module = assignment.module.map(_.data.title),
              lesson = assignment.lesson.map(_.data.title),
              assignment = assignment.asset.title | "Untitled",
              countsForCredit = assignment.isForCredit,
              assignmentPoints = assignment.pointsPossible.autoscaled,
              assignmentCategoryPercent = None,
              assignmentCoursePercent = percentInCourse.map(_.percent),
            )
          )
        }
    }

    FileResponse(exportFile.toFileInfo)
  end exportGradebookImpl
end GradebookWebController

private[gradebook] object GradebookWebController:
  implicit class JavaBigDecimalOps(val self: java.math.BigDecimal) extends AnyVal:
    def asScala: BigDecimal = BigDecimal(self)

  implicit class BigDecimalOps(val self: BigDecimal) extends AnyVal:
    def percent: String = (self * 100).autoscaled.toString + "%"
    def autoscaled: BigDecimal =
      // Seems unreasonably hard to get 1.125 to format as 1.13 with 100 not as 1E2, and 1.5 not as 1.50...
      // First scale it to two decimal places, then strip trailing zeros, then remove scientific notation.
      val bigDec = self.setScale(2, BigDecimal.RoundingMode.HALF_UP).bigDecimal.stripTrailingZeros.asScala
      if bigDec.scale <= 0 then bigDec.setScale(0) else bigDec
end GradebookWebController

private[gradebook] final case class GradebookAssignment(
  module: Option[Asset[Module]],
  lesson: Option[Asset[Lesson]],
  asset: Asset[?],
  isForCredit: Boolean,
  pointsPossible: BigDecimal,
  category: Option[UUID],
)

private[gradebook] final case class GradebookRow(
  category: Option[String],
  categoryWeight: Option[BigDecimal],
  categoryPoints: Option[BigDecimal],
  module: Option[String],
  lesson: Option[String],
  assignment: String,
  countsForCredit: Boolean,
  assignmentPoints: BigDecimal,
  assignmentCategoryPercent: Option[String],
  assignmentCoursePercent: Option[String],
)

private[gradebook] object GradebookRow:
  implicit val competencyRowHeaderEncoder: HeaderEncoder[GradebookRow] =
    HeaderEncoder.caseEncoder(
      "Category",
      "Category Weight",
      "Category Points",
      "Module",
      "Lesson",
      "Assignment",
      "For Credit",
      "Assignment Points",
      "Category Percentage",
      "Course Percentage",
    )(ArgoExtras.unapply)
end GradebookRow
