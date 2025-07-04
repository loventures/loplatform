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

package loi.asset.syntax

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
import loi.asset.survey.Survey1
import loi.asset.unit.model.Unit1

import scala.language.implicitConversions

trait AssetAnySyntax:

  implicit def toAssetAnyOps[A](a: A): AssetAnyOps[A] = new AssetAnyOps(a)

final class AssetAnyOps[A](private val self: A) extends AnyVal:

  /** Gets the title from the asset data directly, ignoring overlays. */
  def title: Option[String] = PartialFunction.condOpt(self) {
    case data: Assessment             => data.title
    case data: Assignment1            => data.title
    case data: Audio                  => data.title
    case data: BinDropQuestion        => data.title
    case data: Checkpoint             => data.title
    case data: CompetencySet          => data.title
    case data: Course                 => data.title
    case data: Diagnostic             => data.title
    case data: Discussion1            => data.title
    case data: EssayQuestion          => data.title
    case data: File                   => data.title
    case data: FileBundle             => data.title
    case data: FillInTheBlankQuestion => data.title
    case data: GradebookCategory1     => data.title
    case data: HotspotQuestion        => data.title
    case data: Html                   => data.title
    case data: Image                  => data.title
    case data: ObservationAssessment1 => data.title
    case data: CourseLink             => data.title
    case data: Javascript             => data.title
    case data: Lesson                 => data.title
    case data: Level1Competency       => data.title
    case data: Level2Competency       => data.title
    case data: Level3Competency       => data.title
    case data: LikertScaleQuestion1   => data.title
    case data: Lti                    => data.title
    case data: MatchingQuestion       => data.title
    case data: Module                 => data.title
    case data: MultipleChoiceQuestion => data.title
    case data: MultipleSelectQuestion => data.title
    case data: OrderingQuestion       => data.title
    case data: Pdf                    => data.title
    case data: Root                   => data.title
    case data: PoolAssessment         => data.title
    case data: RatingScaleQuestion1   => data.title
    case data: Resource1              => data.title
    case data: Rubric                 => data.title
    case data: RubricCriterion        => data.title
    case data: Scorm                  => data.title
    case data: Stylesheet             => data.title
    case data: Survey1                => data.title
    case data: TrueFalseQuestion      => data.title
    case data: Unit1                  => data.title
    case data: VideoCaption           => data.title
    case data: Video                  => data.title
    case data: WebDependency          => data.title
  }
end AssetAnyOps
