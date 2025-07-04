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

package loi.asset.util

import loi.asset.assessment.model.*
import loi.asset.assessment.model.ScoringOption.MostRecentAttemptScore
import loi.asset.contentpart.BlockPart
import loi.asset.course.model.Course
import loi.asset.discussion.model.Discussion1
import loi.asset.external.CourseLink
import loi.asset.file.audio.model.Audio
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.file.image.model.Image
import loi.asset.file.pdf.model.Pdf
import loi.asset.file.video.model.Video
import loi.asset.html.model.*
import loi.asset.lesson.model.Lesson
import loi.asset.lti.Lti
import loi.asset.module.model.Module
import loi.asset.resource.model.Resource1
import loi.asset.syntax.AssetAnyOps
import loi.asset.unit.model.Unit1
import loi.authoring.asset.Asset
import scaloi.=∂>
import scaloi.syntax.option.*

import java.math.BigDecimal
import scala.language.implicitConversions

/** Pimps [[Asset]] with extractors for various properties that are shared by subsets of the data types. */
final class Assex[A](private val self: Asset[A]) extends AnyVal:
  def title: Option[String] = new AssetAnyOps(self.data).title

  def subtitle: Option[String] = matchData {
    case a: Course         => a.subtitle
    case a: Assessment     => a.subtitle
    case a: Audio          => a.subtitle
    case a: Checkpoint     => a.subtitle
    case a: Diagnostic     => a.subtitle
    case a: FileBundle     => a.subtitle
    case a: Image          => a.subtitle
    case a: Pdf            => a.subtitle
    case a: PoolAssessment => a.subtitle
    case a: Video          => a.subtitle
  }

  def description: Option[String] = matchData {
    case a: Unit1  => a.description
    case a: Module => a.description
    case a: Lesson => a.description
  }

  def keywords: Option[String] = matchData {
    case a: Course                 => a.keywords
    case a: Unit1                  => a.keywords
    case a: Module                 => a.keywords
    case a: Lesson                 => a.keywords
    case a: Assessment             => a.keywords
    case a: Assignment1            => a.keywords
    case a: Checkpoint             => a.keywords
    case a: Diagnostic             => a.keywords
    case a: Discussion1            => a.keywords
    case a: FileBundle             => a.keywords
    case a: Html                   => a.keywords
    case a: ObservationAssessment1 => a.keywords
    case a: CourseLink             => a.keywords
    case a: Lti                    => a.keywords
    case a: PoolAssessment         => a.keywords
    case a: Resource1              => a.keywords
    case a: Scorm                  => a.keywords
  }

  def duration: Option[Long] = flatchData {
    case a: Unit1                  => a.duration
    case a: Module                 => a.duration
    // case a: Lesson                      => a.duration
    case a: Assessment             => a.duration
    case a: Assignment1            => a.duration
    case a: Checkpoint             => a.duration
    case a: CourseLink             => a.duration
    case a: Diagnostic             => a.duration
    case a: Discussion1            => a.duration
    case a: FileBundle             => a.duration
    case a: Html                   => a.duration
    case a: ObservationAssessment1 => a.duration
    case a: Lti                    => a.duration
    case a: PoolAssessment         => a.duration
    case a: Resource1              => a.duration
    case a: Scorm                  => a.duration
  }

  /** Normalizes max attempts to [[None]] if unlimited. */
  def maxAttempts: Option[Long] = flatchData {
    case a: Assessment             => a.maxAttempts.filterNot(_ == 0)
    case a: Assignment1            => a.maxAttempts.unless(a.unlimitedAttempts)
    case _: Checkpoint             => None
    case a: Diagnostic             => a.maxAttempts // fixed at 1
    case a: ObservationAssessment1 => a.maxAttempts.unless(a.unlimitedAttempts)
    case a: PoolAssessment         => a.maxAttempts.filterNot(_ == 0)
  }

  def maxMinutes: Option[Long] = flatchData {
    case a: Assessment     => a.maxMinutes
    case a: Diagnostic     => a.maxMinutes
    case a: PoolAssessment => a.maxMinutes
  }

  def scoringOption: Option[ScoringOption] = flatchData {
    case a: Assessment             => a.scoringOption
    case a: Assignment1            => a.scoringOption
    case _: Checkpoint             => Some(MostRecentAttemptScore)
    case a: Diagnostic             => a.scoringOption
    case a: ObservationAssessment1 => a.scoringOption
    case a: PoolAssessment         => a.scoringOption
  }

  def pointsPossible: Option[BigDecimal] = matchData {
    case a: Assessment             => a.pointsPossible
    case a: Assignment1            => a.pointsPossible
    case _: Checkpoint             => BigDecimal.valueOf(100)
    case a: Diagnostic             => a.pointsPossible
    case a: Discussion1            => a.pointsPossible
    case a: ObservationAssessment1 => a.pointsPossible
    case a: Scorm                  => a.pointsPossible
    case a: Lti                    => a.pointsPossible
    case a: PoolAssessment         => a.pointsPossible
    case a: CourseLink             => a.pointsPossible
  }

  // the mix-in if `gradable` and `isGraded` for discussion and lti .. so woe
  def isForCredit: Option[Boolean] = matchData {
    case a: Assessment                                     => a.isForCredit
    case a: Assignment1                                    => a.isForCredit
    case a: Checkpoint                                     => false
    case a: Diagnostic                                     => a.isForCredit
    case a: Discussion1 if a.gradable                      => a.isForCredit
    case a: ObservationAssessment1                         => a.isForCredit
    case a: Scorm                                          => a.isForCredit
    case a: Lti if a.lti.toolConfiguration.isGraded.isTrue => a.isForCredit
    case a: PoolAssessment                                 => a.isForCredit
    case a: CourseLink if a.gradable                       => a.isForCredit
  }

  def instructions: Option[BlockPart] = matchData {
    case data: Assessment             => data.instructions
    case data: Assignment1            => data.instructions
    case data: Checkpoint             => data.instructions
    case data: Diagnostic             => data.instructions
    case data: Discussion1            => data.instructions
    case data: ObservationAssessment1 => data.instructions
    case data: Lti                    => data.instructions
    case data: PoolAssessment         => data.instructions
    case data: Resource1              => data.instructions
    case data: CourseLink             => data.instructions
  }

  def withInstructions(instructions: BlockPart): Option[Asset[A]] = PartialFunction.condOpt(self) {
    case Assessment.Asset(a)             => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Assignment1.Asset(a)            => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Checkpoint.Asset(a)             => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Diagnostic.Asset(a)             => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Discussion1.Asset(a)            => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case ObservationAssessment1.Asset(a) => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Lti.Asset(a)                    => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case PoolAssessment.Asset(a)         => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case Resource1.Asset(a)              => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
    case CourseLink.Asset(a)             => a.map(_.copy(instructions = instructions)).asInstanceOf[Asset[A]]
  }

  def accessRight: Option[String] = self.data match
    case data: Unit1                  => data.accessRight
    case data: Module                 => data.accessRight
    case data: Lesson                 => data.accessRight
    case data: Assessment             => data.accessRight
    case data: Assignment1            => data.accessRight
    case data: Checkpoint             => data.accessRight
    case data: Diagnostic             => data.accessRight
    case data: Discussion1            => data.accessRight
    case data: ObservationAssessment1 => data.accessRight
    case data: Lti                    => data.accessRight
    case data: PoolAssessment         => data.accessRight
    case data: Resource1              => data.accessRight
    case data: FileBundle             => data.accessRight
    case data: Scorm                  => data.accessRight
    case data: Html                   => data.accessRight
    case _                            => None

  private def matchData[B](f: A =∂> B): Option[B] = PartialFunction.condOpt(self.data)(f)

  private def flatchData[B](f: A =∂> Option[B]): Option[B] = flondOpt(self.data)(f)
end Assex

object Assex:
  implicit def asshatOps[A](a: Asset[A]): Assex[A] = new Assex(a)
