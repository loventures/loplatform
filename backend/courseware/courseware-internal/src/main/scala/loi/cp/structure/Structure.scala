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

package loi.cp.structure

import argonaut.Argonaut.*
import argonaut.*
import loi.asset.assessment.model.ScoringOption
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.exprt.CourseStructureExportService
import loi.cp.content.CourseContents
import loi.cp.course.CourseComponent
import loi.cp.reference.EdgePath
import scalaz.std.anyVal.*
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.json.ArgoExtras
import scaloi.syntax.option.*

sealed trait Structure

object Structure:
  import loi.asset.util.Assex.*
  import com.learningobjects.cpxp.service.group.GroupConstants.GroupType.*

  def apply(course: CourseComponent, contents: CourseContents): Structure =
    contents.tree.foldTree[Structure]((content, children) =>
      val typeId   = content.asset.info.typeId
      val typeName = CourseStructureExportService.assetTypeNameMap(typeId)

      typeId match
        case AssetTypeId.Course =>
          CourseStructure(
            typeId,
            typeName,
            content.edgePath,
            (course.getGroupType == CourseOffering).option(course.getGroupId),
            course.externalId.when(course.getGroupType == CourseSection),
            course.loadBranch().project.foldZ(_.name),
            content.title,
            content.description.filterNZ,
            content.asset.keywords.filterNZ,
            children
          )

        case AssetTypeId.Module =>
          ModuleStructure(
            typeId,
            typeName,
            content.edgePath,
            content.title,
            content.description.filterNZ,
            content.asset.keywords.filterNZ,
            content.duration.map(_ * 60), // convert minutes to seconds
            children
          )

        case AssetTypeId.Lesson =>
          LessonStructure(
            typeId,
            typeName,
            content.edgePath,
            content.title,
            content.description.filterNZ,
            content.asset.keywords.filterNZ,
            children
          )

        case t if AssetTypeId.CreditableTypes.contains(t) =>
          CreditableStructure(
            typeId,
            typeName,
            content.edgePath,
            content.title,
            content.description.filterNZ,
            content.asset.keywords.filterNZ,
            content.duration.map(_ * 60), // convert minutes to seconds
            content.isForCredit.isTrue,
            content.pointsPossible.map(BigDecimal.apply),
            content.asset.maxAttempts,
            content.asset.scoringOption
          )

        case t if AssetTypeId.LessonElementTypes.contains(t) =>
          ContentStructure(
            typeId,
            typeName,
            content.edgePath,
            content.title,
            content.description.filterNZ,
            content.asset.keywords.filterNZ,
            content.duration.filterNZ.map(_ * 60) // convert minutes to seconds
          )

        case _ => ???
      end match
    )

  implicit val structureCodec: CodecJson[Structure] =
    import scaloi.json.ArgoExtras.*
    def encode(structure: Structure): Json           = structure match
      case course: CourseStructure         => course.jencode
      case module: ModuleStructure         => module.jencode
      case lesson: LessonStructure         => lesson.jencode
      case creditable: CreditableStructure => creditable.jencode
      case content: ContentStructure       => content.jencode
    def decode(hc: HCursor): DecodeResult[Structure] =
      (hc --\ "typeId").as[AssetTypeId] flatMap { typeId =>
        if AssetTypeId.CreditableTypes.contains(typeId) then hc.as[CreditableStructure].widen[Structure]
        if AssetTypeId.LessonElementTypes.contains(typeId) then hc.as[ContentStructure].widen[Structure]
        if typeId == AssetTypeId.Course then hc.as[CourseStructure].widen[Structure]
        if typeId == AssetTypeId.Module then hc.as[ModuleStructure].widen[Structure]
        else hc.as[LessonStructure].widen[Structure]
      }
    CodecJson(encode, decode)
  end structureCodec
end Structure

/** Course. */
final case class CourseStructure(
  typeId: AssetTypeId,
  typeName: String,
  edgePath: EdgePath,
  offeringId: Option[String],
  externalId: Option[String],
  project: String,
  title: String,
  description: Option[String],
  keywords: Option[String],
  children: List[Structure],
) extends Structure

object CourseStructure:
  implicit val courseStructureCodec: CodecJson[CourseStructure] =
    casecodec10(CourseStructure.apply, ArgoExtras.unapply)(
      "typeId",
      "typeName",
      "edgePath",
      "offeringId",
      "externalId",
      "project",
      "title",
      "description",
      "keywords",
      "children"
    )
end CourseStructure

/** Module, lesson. */
final case class ModuleStructure(
  typeId: AssetTypeId,
  typeName: String,
  edgePath: EdgePath,
  title: String,
  description: Option[String],
  keywords: Option[String],
  duration: Option[Long],
  children: List[Structure],
) extends Structure

object ModuleStructure:
  implicit val parentStructureCodec: CodecJson[ModuleStructure] =
    casecodec8(ModuleStructure.apply, ArgoExtras.unapply)(
      "typeId",
      "typeName",
      "edgePath",
      "title",
      "description",
      "keywords",
      "duration",
      "children"
    )
end ModuleStructure

final case class LessonStructure(
  typeId: AssetTypeId,
  typeName: String,
  edgePath: EdgePath,
  title: String,
  description: Option[String],
  keywords: Option[String],
  children: List[Structure],
) extends Structure

object LessonStructure:
  implicit val parentStructureCodec: CodecJson[LessonStructure] =
    casecodec7(LessonStructure.apply, ArgoExtras.unapply)(
      "typeId",
      "typeName",
      "edgePath",
      "title",
      "description",
      "keywords",
      "children"
    )
end LessonStructure

/** Any content type that can potentially count for credit. */
final case class CreditableStructure(
  typeId: AssetTypeId,
  typeName: String,
  edgePath: EdgePath,
  title: String,
  description: Option[String],
  keywords: Option[String],
  duration: Option[Long],
  forCredit: Boolean,
  pointsPossible: Option[BigDecimal],
  maxAttempts: Option[Long],
  scoringOption: Option[ScoringOption]
) extends Structure

object CreditableStructure:
  implicit val creditableStructureCodec: CodecJson[CreditableStructure] =
    casecodec11(CreditableStructure.apply, ArgoExtras.unapply)(
      "typeId",
      "typeName",
      "edgePath",
      "title",
      "description",
      "keywords",
      "duration",
      "forCredit",
      "pointsPossible",
      "maxAttempts",
      "scoringOption"
    )
end CreditableStructure

/** Any other course content. */
final case class ContentStructure(
  typeId: AssetTypeId,
  typeName: String,
  edgePath: EdgePath,
  title: String,
  description: Option[String],
  keywords: Option[String],
  duration: Option[Long],
) extends Structure

object ContentStructure:
  implicit val contentStructureCodec: CodecJson[ContentStructure] =
    casecodec7(ContentStructure.apply, ArgoExtras.unapply)(
      "typeId",
      "typeName",
      "edgePath",
      "title",
      "description",
      "keywords",
      "duration"
    )
end ContentStructure
