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

package loi.authoring.asset.factory

import argonaut.DecodeJson
import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import enumeratum.*

/** Marks the asset type of the assetnode row.
  */
@JsonDeserialize(`using` = classOf[AssetTypeIdDeserializer])
sealed abstract class AssetTypeId(override val entryName: String) extends EnumEntry:
  override def toString: String = entryName

object AssetTypeId extends Enum[AssetTypeId] with ArgonautEnum[AssetTypeId]:

  val values = findValues

  case object Assessment             extends AssetTypeId("assessment.1")
  case object Assignment             extends AssetTypeId("assignment.1")
  case object Audio                  extends AssetTypeId("audio.1")
  case object BinDropQuestion        extends AssetTypeId("binDropQuestion.1")
  case object Checkpoint             extends AssetTypeId("checkpoint.1")
  case object CompetencySet          extends AssetTypeId("competencySet.1")
  case object Course                 extends AssetTypeId("course.1")
  case object Diagnostic             extends AssetTypeId("diagnostic.1")
  case object Discussion             extends AssetTypeId("discussion.1")
  case object EssayQuestion          extends AssetTypeId("essayQuestion.1")
  case object CourseLink             extends AssetTypeId("courseLink.1")
  case object File                   extends AssetTypeId("file.1")
  case object FileBundle             extends AssetTypeId("fileBundle.1")
  case object FillInTheBlankQuestion extends AssetTypeId("fillInTheBlankQuestion.1")
  case object GradebookCategory      extends AssetTypeId("gradebookCategory.1")
  case object HotspotQuestion        extends AssetTypeId("hotspotQuestion.1")
  case object Html                   extends AssetTypeId("html.1")
  case object Scorm                  extends AssetTypeId("scorm.1")
  case object ObservationAssessment  extends AssetTypeId("observationAssessment.1")
  case object Image                  extends AssetTypeId("image.1")
  case object Javascript             extends AssetTypeId("js.1")
  case object Lesson                 extends AssetTypeId("lesson.1")
  case object Level1Competency       extends AssetTypeId("level1Competency.1")
  case object Level2Competency       extends AssetTypeId("level2Competency.1")
  case object Level3Competency       extends AssetTypeId("level3Competency.1")
  case object LikertScaleQuestion1   extends AssetTypeId("likertScaleQuestion.1")
  case object Lti                    extends AssetTypeId("lti.1")
  case object MatchingQuestion       extends AssetTypeId("matchingQuestion.1")
  case object Module                 extends AssetTypeId("module.1")
  case object MultipleChoiceQuestion extends AssetTypeId("multipleChoiceQuestion.1")
  case object MultipleSelectQuestion extends AssetTypeId("multipleSelectQuestion.1")
  case object OrderingQuestion       extends AssetTypeId("orderingQuestion.1")
  case object Pdf                    extends AssetTypeId("pdf.1")
  case object PoolAssessment         extends AssetTypeId("poolAssessment.1")
  case object Root                   extends AssetTypeId("root.1")
  case object RatingScaleQuestion1   extends AssetTypeId("ratingScaleQuestion.1")
  case object Resource1              extends AssetTypeId("resource.1")
  case object Rubric                 extends AssetTypeId("rubric.1")
  case object RubricCriterion        extends AssetTypeId("rubricCriterion.1")
  case object ShortAnswerQuestion    extends AssetTypeId("shortAnswerQuestion.1")
  case object Stylesheet             extends AssetTypeId("css.1")
  case object Survey1                extends AssetTypeId("survey.1")
  case object SurveyChoiceQuestion1  extends AssetTypeId("surveyChoiceQuestion.1")
  case object SurveyEssayQuestion1   extends AssetTypeId("surveyEssayQuestion.1")
  case object TrueFalseQuestion      extends AssetTypeId("trueFalseQuestion.1")
  case object Unit                   extends AssetTypeId("unit.1")
  case object Video                  extends AssetTypeId("video.1")
  case object VideoCaption           extends AssetTypeId("videoCaption.1")
  case object WebDependency          extends AssetTypeId("webDependency.1")

  case class Unknown(entryName0: String) extends AssetTypeId(entryName0)

  override def withName(str: String): AssetTypeId = AssetTypeId.withNameOption(str).getOrElse(Unknown(str))

  // not using `ArgonautEnum`'s `DecodeJson` because it won't handle the fallback-to-`Unknown` desire
  implicit val decodeJsonForAssetTypeId: DecodeJson[AssetTypeId] = DecodeJson(_.as[String].map(AssetTypeId.withName))

  val CompetencyTypes: Set[AssetTypeId] = Set(Level1Competency, Level2Competency, Level3Competency)

  val CompetencyAndSetTypes: Set[AssetTypeId] = CompetencyTypes + CompetencySet

  val FileTypes: Set[AssetTypeId] = Set(Audio, File, Image, Pdf, Video, VideoCaption)

  val CreditableTypes: Set[AssetTypeId] = Set(
    Assessment,
    Assignment,
    Checkpoint,
    Diagnostic,
    Discussion,
    ObservationAssessment,
    CourseLink,
    Lti,
    PoolAssessment,
    Scorm
  )

  val LessonElementTypes: Set[AssetTypeId] = Set(
    Assessment,
    Assignment,
    Checkpoint,
    Diagnostic,
    Discussion,
    FileBundle,
    Scorm,
    Html,
    ObservationAssessment,
    CourseLink,
    Lti,
    PoolAssessment,
    Resource1
  )

  val QuestionTypes: Set[AssetTypeId] = Set(
    BinDropQuestion,
    EssayQuestion,
    FillInTheBlankQuestion,
    HotspotQuestion,
    MatchingQuestion,
    MultipleChoiceQuestion,
    MultipleSelectQuestion,
    OrderingQuestion,
    ShortAnswerQuestion,
    TrueFalseQuestion
  )

  val SurveyQuestionTypes: Set[AssetTypeId] = Set(
    EssayQuestion,
    LikertScaleQuestion1,
    MultipleChoiceQuestion,
    RatingScaleQuestion1,
    SurveyChoiceQuestion1,
    SurveyEssayQuestion1,
  )

  val RemediationTypes: Set[AssetTypeId] = Set(
    FileBundle,
    Scorm,
    Html,
    Resource1
  )

  val CourseContentTypes: Set[AssetTypeId] = LessonElementTypes + Unit + Module + Lesson

  val ContainerTypes: Set[AssetTypeId] = Set(Course, Unit, Module, Lesson)

  val GatedTypes: Set[AssetTypeId] = CourseContentTypes

  val HyperlinkTypes: Set[AssetTypeId] = CourseContentTypes

  val AssessesTypes: Set[AssetTypeId] =
    QuestionTypes + Assignment + ObservationAssessment + Lti + Discussion + RubricCriterion

  val TeachesTypes: Set[AssetTypeId] = Set(Lesson, Resource1, Lti, Scorm, Html, FileBundle)
end AssetTypeId

private class AssetTypeIdDeserializer extends JsonDeserializer[AssetTypeId]:
  override def deserialize(
    jp: JsonParser,
    ctxt: DeserializationContext
  ): AssetTypeId =
    if jp.getCurrentToken == JsonToken.VALUE_STRING then AssetTypeId.withName(jp.getText)
    else
      throw ctxt.wrongTokenException(jp, classOf[AssetTypeId], JsonToken.VALUE_STRING, "cannot deserialize AssetTypeId")
