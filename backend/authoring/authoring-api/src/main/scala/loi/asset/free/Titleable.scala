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

package loi.asset.free

import loi.asset.assessment.model.*
import loi.asset.competency.model.*
import loi.asset.course.model.Course
import loi.asset.discussion.model.Discussion1
import loi.asset.external.CourseLink
import loi.asset.file.image.model.Image
import loi.asset.file.video.model.Video
import loi.asset.gradebook.GradebookCategory1
import loi.asset.html.model.{Html, Scorm, WebDependency}
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.asset.root.model.Root
import loi.asset.question.*
import loi.asset.rubric.model.{Rubric, RubricCriterion}
import loi.asset.survey.Survey1
import loi.asset.unit.model.Unit1

/** This determines what the [[PlantUmlExecutor]] displays when rendering an asset
  */
trait Titleable[A]:
  def title(a: A): String

object Titleable:
  def apply[A: Titleable]: Titleable[A] = implicitly

  // Titleable is a single-abstract-method trait, so definitions can just be functions
  implicit val assessmentData: Titleable[Assessment]                         = _.title
  implicit val assignment1Data: Titleable[Assignment1]                       = _.title
  implicit val binDropQuestionData: Titleable[BinDropQuestion]               = _.title
  implicit val checkpointData: Titleable[Checkpoint]                         = _.title
  implicit val courseData: Titleable[Course]                                 = _.title
  implicit val competencySetData: Titleable[CompetencySet]                   = _.title
  implicit val diagnosticData: Titleable[Diagnostic]                         = _.title
  implicit val discussion1Data: Titleable[Discussion1]                       = _.title
  implicit val essayQuestionData: Titleable[EssayQuestion]                   = _.title
  implicit val fillInTheBankQuestionData: Titleable[FillInTheBlankQuestion]  = _.title
  implicit val gradebookCategory1Data: Titleable[GradebookCategory1]         = _.title
  implicit val htmlData: Titleable[Html]                                     = _.title
  implicit val scormData: Titleable[Scorm]                                   = _.title
  implicit val hotSpotQuestionData: Titleable[HotspotQuestion]               = _.title
  implicit val imageData: Titleable[Image]                                   = _.title
  implicit val lessonData: Titleable[Lesson]                                 = _.title
  implicit val likertScaleQuestion1: Titleable[LikertScaleQuestion1]         = _.title
  implicit val lvl1CompData: Titleable[Level1Competency]                     = _.title
  implicit val lvl2CompData: Titleable[Level2Competency]                     = _.title
  implicit val lvl3CompData: Titleable[Level3Competency]                     = _.title
  implicit val matchingQuestionData: Titleable[MatchingQuestion]             = _.title
  implicit val unitData: Titleable[Unit1]                                    = _.title
  implicit val moduleData: Titleable[Module]                                 = _.title
  implicit val multipleChoiceQuestion: Titleable[MultipleChoiceQuestion]     = _.title
  implicit val multipleSelectQuestionData: Titleable[MultipleSelectQuestion] = _.title
  implicit val orderingQuestionData: Titleable[OrderingQuestion]             = _.title
  implicit val shortAnswerQuestionData: Titleable[ShortAnswerQuestion]       = _ => ""
  implicit val survey1: Titleable[Survey1]                                   = _.title
  implicit val poolAssessmentData: Titleable[PoolAssessment]                 = _.title
  implicit val root: Titleable[Root]                                         = _.title
  implicit val ratingScaleQuestion1: Titleable[RatingScaleQuestion1]         = _.title
  implicit val rubricData: Titleable[Rubric]                                 = _.title
  implicit val rubricCriterionData: Titleable[RubricCriterion]               = _.title
  implicit val trueFalseQuestion: Titleable[TrueFalseQuestion]               = _.title
  implicit val videoData: Titleable[Video]                                   = _.title
  implicit val webDependency: Titleable[WebDependency]                       = _.title
  implicit val courseLink: Titleable[CourseLink]                             = _.title
end Titleable
