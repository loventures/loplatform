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

import loi.asset.module.model.Module
import loi.asset.unit.model.Unit1
import loi.cp.content.CourseContents
import loi.cp.content.ContentTree
import loi.cp.reference.EdgePath
import scalaz.std.anyVal.*
import scalaz.std.iterable.*
import scalaz.std.list.*
import scalaz.syntax.equal.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.option.*
import scaloi.data.ListTree.Node
import scaloi.json.ArgoExtras
import scaloi.syntax.CollectionOps.*

import java.util.UUID

final case class GradeStructure(categories: Seq[GradeCategory]):
  lazy val totalPoints: Double =
    categories.map(categoryPoints).sum

  lazy val totalGrades: Int =
    categories.foldMap(1 + _.columns.size)

  def findColumnForEdgePath(edgePath: EdgePath): Option[GradeColumn] =
    categories.findMap(_.columns.find(column => column.path === edgePath))

  def findCategoryByEdgePath(edgePath: EdgePath): Option[GradeCategory] =
    categories.find(category => category.path === edgePath)

  def columnCategory(column: GradeColumn): GradeCategory =
    categories.find(_.columns.exists(_.path === column.path)).get

  /** TODO: Ensure coherency, check that category belongs to the structure.
    */
  def categoryWeight(category: GradeCategory): Double =
    categoryPoints(category) / totalPoints

  def categoryPoints(category: GradeCategory): Double =
    category.weight.cata(_.doubleValue, category.columns.filter(_.isForCredit).map(_.pointsPossible.doubleValue).sum)

  def columns: List[GradeColumn] = categories.foldMap(_.columns.toList)
end GradeStructure

object GradeStructure:
  val Uncategory = new UUID(0L, 0L)

  /** Builds the grade structure of the course contents. Modules become categories and descendants with a grade policy
    * are placed in the module's category. All other content is omitted.
    *
    * @return
    *   grade structure for the course contents
    */
  def apply(contents: CourseContents): GradeStructure =
    val categories = if contents.categories.isEmpty then
      def columns(contents: List[ContentTree]): List[GradeColumn] =
        contents.flatMap({ case Node(content, children) =>
          content.gradingPolicy.fold(columns(children)) { policy =>
            GradeColumn(content.edgePath, content.title, policy.isForCredit, policy.pointsPossible) +: columns(children)
          }
        })

      contents.tree.subForest.collect({
        case Node(content, children) if content.asset.is[Module] || content.asset.is[Unit1] =>
          GradeCategory(content.edgePath, content.title, columns(children), None)
      })
    else
      val categorized = contents.tree.flatten.groupBy(_.category | Uncategory)

      def categoryAssignments(category: UUID) =
        for
          content <- categorized.getOrElse(category, Nil)
          policy  <- content.gradingPolicy
        yield GradeColumn(content.edgePath, content.title, policy.isForCredit, policy.pointsPossible)

      contents.categories.map(category =>
        GradeCategory(
          category.path,
          category.asset.data.title,
          categoryAssignments(category.asset.info.name),
          category.asset.data.weight.some
        )
      ) :+ GradeCategory(
        EdgePath(Uncategory),
        "Uncategorized",
        categoryAssignments(Uncategory),
        BigDecimal.valueOf(0).some,
      )
    GradeStructure(categories)
  end apply
end GradeStructure

final case class GradeCategory(
  path: EdgePath,
  title: String,
  columns: Seq[GradeColumn],
  weight: Option[BigDecimal] = None
)

final case class GradeColumn(path: EdgePath, title: String, isForCredit: Boolean, pointsPossible: BigDecimal)

object GradeColumn:
  import argonaut.*
  import Argonaut.*

  implicit val codec: CodecJson[GradeColumn] =
    CodecJson.casecodec4(GradeColumn.apply, ArgoExtras.unapply)("path", "title", "isForCredit", "pointsPossible")
