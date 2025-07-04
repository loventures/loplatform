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

package loi.authoring

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.de.web.MediaType
import loi.asset.assessment.model.*
import loi.asset.competency.model.{CompetencySet, Level1Competency, Level2Competency, Level3Competency}
import loi.asset.course.model.Course
import loi.asset.discussion.model.Discussion1
import loi.asset.external.CourseLink
import loi.asset.file.audio.model.Audio
import loi.asset.file.file.model.File
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.file.image.model.Image
import loi.asset.file.pdf.model.Pdf
import loi.asset.file.video.model.Video
import loi.asset.file.videoCaption.model.VideoCaption
import loi.asset.gradebook.GradebookCategory1
import loi.asset.html.model.*
import loi.asset.lesson.model.Lesson
import loi.asset.lti.Lti
import loi.asset.module.model.Module
import loi.asset.root.model.Root
import loi.asset.question.*
import loi.asset.resource.model.Resource1
import loi.asset.rubric.model.{Rubric, RubricCriterion}
import loi.asset.survey.{Survey1, SurveyChoiceQuestion1, SurveyEssayQuestion1}
import loi.asset.unit.model.Unit1
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.{AssetTypeId, SpecialPropsConfig}
import loi.authoring.blob.BlobService
import loi.authoring.edge.{EdgeRule, Group}
import loi.authoring.index.AssetDataDocument

import java.util.UUID
import scala.reflect.ClassTag

/** A type class to provide asset type representation.
  */
abstract class AssetType[A](val id: AssetTypeId)(implicit
  val classTag: ClassTag[A],
  val specialProps: SpecialPropsConfig[A]
):

  val allowedAttachmentTypes: Set[MediaType] = Set.empty

  /** The allowed out-edges from this asset type.
    */
  val edgeConfig: Map[Group, Set[AssetTypeId]] = Map.empty

  // strengthen the config with its Group
  final lazy val edgeRules: Map[Group, EdgeRule] = edgeConfig.map({ case (group, typeIds) =>
    group -> EdgeRule(group, typeIds.toSeq)
  })

  /** validates `data` for creating an asset
    */
  def validate(data: A): ValidatedNel[String, Unit] = Valid(())

  /** validates `data` for updating an asset
    */
  def updateValidate(data: A, groupSizes: => Map[Group, Int]): ValidatedNel[String, Unit] =
    validate(data)

  /** @return
    *   the edgeIds referenced inside A
    */
  def edgeIds(data: A): Set[UUID] = Set.empty

  /** Assets can refer to their out-edges inside their data payload by using
    *
    * @return
    *   a new `A` that has replaced its edgeId references in some way with the targets of those referenced edges
    */
  def render(data: A, targets: Map[UUID, Asset[?]]): A = data

  /** @return
    *   a plain title for display and search purposes, extracted from `data`
    */
  def computeTitle(data: A): Option[String] = None

  /** @return
    *   a new A such that applying `computeTitle` to it kinda sorta sometimes returns `title`
    */
  def receiveTitle(data: A, title: String): A = data

  /** @return
    *   a search index entry for `data`
    */
  def index(data: A)(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument

  /** @return
    *   all the html strings in `data`
    */
  def htmls(data: A)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String]

  val dataClass: Class[A] = classTag.runtimeClass.asInstanceOf[Class[A]]

  def allowsAttachmentType(mediaType: MediaType): Boolean =
    allowedAttachmentTypes.exists(_.includes(mediaType))

  override def equals(other: Any): Boolean =
    other match
      case that: AssetType[?] => id == that.id
      case _                  => false

  override def hashCode(): Int =
    val state = Seq(id)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)

  override def toString: String = s"AssetType($id)"
end AssetType

object AssetType:

  /** Access an implicit `AssetType[A]`
    */
  def apply[A](implicit assetType: AssetType[A]): AssetType[A] = assetType

  val types: Map[AssetTypeId, AssetType[?]] = List[AssetType[?]](
    AssetType[Root],
    AssetType[Course],
    AssetType[Unit1],
    AssetType[Module], // RENAME
    AssetType[Lesson], // RENAME
    AssetType[Discussion1],
    AssetType[Lti],
    AssetType[Resource1],
    AssetType[Rubric],
    AssetType[RubricCriterion],
    AssetType[Survey1],
    AssetType[GradebookCategory1],
    AssetType[CourseLink],
    // assessments
    AssetType[Assessment],
    AssetType[Assignment1],
    AssetType[Checkpoint],
    AssetType[Diagnostic],
    AssetType[ObservationAssessment1],
    AssetType[PoolAssessment],
    // questions
    AssetType[BinDropQuestion],
    AssetType[EssayQuestion],
    AssetType[FillInTheBlankQuestion],
    AssetType[HotspotQuestion],
    AssetType[LikertScaleQuestion1],
    AssetType[MatchingQuestion],
    AssetType[MultipleChoiceQuestion],
    AssetType[MultipleSelectQuestion],
    AssetType[OrderingQuestion],
    AssetType[RatingScaleQuestion1],
    AssetType[ShortAnswerQuestion],
    AssetType[TrueFalseQuestion],
    AssetType[SurveyChoiceQuestion1],
    AssetType[SurveyEssayQuestion1],
    // competencies
    AssetType[CompetencySet],
    AssetType[Level1Competency],
    AssetType[Level2Competency],
    AssetType[Level3Competency],
    // web
    AssetType[Html],
    AssetType[Javascript],
    AssetType[Stylesheet],
    AssetType[WebDependency],
    // file
    AssetType[Audio],
    AssetType[File],
    AssetType[FileBundle],
    AssetType[Scorm],
    AssetType[Image],
    AssetType[Pdf],
    AssetType[Video],
    AssetType[VideoCaption],
  ).groupBy(_.id)
    .map[AssetTypeId, AssetType[?]]({
      case (id, types) if types.size > 1 =>
        throw new RuntimeException(
          s"expected 1 AssetType for $id found ${types.size}: [${types.map(_.dataClass.getSimpleName).mkString(",")}]"
        )
      case (id, typ :: Nil)              => (id, typ)
      case (id, _)                       =>
        throw new RuntimeException(s"expected 1 AssetType for $id found _]")
    })
end AssetType
