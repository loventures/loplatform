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

package loi.cp.content

import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.asset.edge.EdgeData
import loi.cp.customisation.ContentOverlay
import loi.cp.reference.EdgePath
import scalaz.std.option.*
import scalaz.std.tuple.*
import scalaz.syntax.bitraverse.*
import scalaz.syntax.std.option.*
import scaloi.syntax.option.*

import java.math.BigDecimal
import java.util.UUID

/** A view on an item in a course with any course-specific customisation.
  *
  * If you need the full edge instead of just its target, name, and data, then use `Content`, but know that `Content` is
  * too big to cache and cannot represent the root course asset.
  *
  * @param edgeNames
  *   the [[EdgePath]] in List[UUID] form
  * @param originalAsset
  *   the asset in the course
  * @param edgeData
  *   the asset edge data
  * @param overlay
  *   any course specific overlays for this content
  * @param category
  *   the gradebook category
  * @param survey
  *   the associated survey edge name and asset name
  */
final case class CourseContent(
  edgeNames: List[UUID],
  originalAsset: Asset[?],
  edgeData: EdgeData,
  overlay: ContentOverlay,
  accessRight: Option[String] = None,
  category: Option[UUID] = None,
  survey: Option[(UUID, UUID)] = None,
  hyperlinks: Map[UUID, UUID] = Map.empty,
  testsOut: Map[UUID, Double] = Map.empty,
  bannerImage: Option[UUID] = None, // hateful but expedient. the course banner image name.
):

  import CourseContent.*
  import loi.asset.util.Assex.*

  lazy val asset: Asset[?] = overlay.instructions.flatMap(instructed(originalAsset, _)) | originalAsset

  private def instructed(asset: Asset[?], html: String): Option[Asset[?]] =
    asset.withInstructions(BlockPart(Seq(HtmlPart(html, html.some)), html.some))

  lazy val edgePath: EdgePath = EdgePath(edgeNames)

  def name = originalAsset.info.name

  def title: String = overlay.title.orElse(originalAsset.title).getOrElse("")

  /** The description of the content. This is either derived from the subtitle field or the description field in the
    * asset, as they are mutually exclusive.
    *
    * @return
    *   description of the content
    */
  def description: Option[String] = originalAsset.subtitle || originalAsset.description

  def duration: Option[Long] = overlay.duration || originalAsset.duration

  def isForCredit: Option[Boolean] = overlay.isForCredit || originalAsset.isForCredit

  def pointsPossible: Option[BigDecimal] = overlay.pointsPossible || originalAsset.pointsPossible

  def gradingPolicy: Option[GradingConfiguration] =
    (isForCredit, pointsPossible).bisequence[Option, Boolean, BigDecimal].map(GradingConfiguration.apply)

  def maxMinutes: Option[Long] = originalAsset.maxMinutes

  lazy val isContainer: Boolean = ContainerAssetTypeIds.contains(originalAsset.info.typeId)
end CourseContent

object CourseContent:
  final case class GradingConfiguration(
    isForCredit: Boolean,
    pointsPossible: BigDecimal,
  )

  val ContainerAssetTypeIds: Set[AssetTypeId] =
    Set(AssetTypeId.Course, AssetTypeId.Unit, AssetTypeId.Module, AssetTypeId.Lesson)
