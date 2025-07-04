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

package loi.cp.assessment
import argonaut.Argonaut.*
import argonaut.*
import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import loi.asset.assessment.model.*
import loi.asset.assessment.model as authoring
import loi.authoring.asset.Asset
import loi.cp.assessment.settings.{AttemptLimit, Limited, Unlimited}
import loi.cp.storage.CourseStoreable
import scaloi.json.ArgoExtras

case class CourseAssessmentPolicy(
  assessmentGradingPolicy: AssessmentGradingPolicy,
  attemptLimit: AttemptLimit,
  // In the immediate future, this will correspond to a title that is consistently used in courses
  // The tech debt to pay off will be adding an actual field to the asset data definitions
  assessmentType: RegexBasedAssessmentType
)

object CourseAssessmentPolicy:

  import RegexBasedAssessmentType.*

  implicit val coursePolicyAssessmentSetting: CodecJson[CourseAssessmentPolicy] = casecodec3(
    CourseAssessmentPolicy.apply,
    ArgoExtras.unapply
  )("assessmentGradingPolicy", "attemptLimit", "assessmentType")

  val defaults = List(
    CourseAssessmentPolicy(AssessmentGradingPolicy.FirstAttempt, Limited(1), RegexBasedAssessmentType.ShowWhatYouKnow),
    CourseAssessmentPolicy(AssessmentGradingPolicy.Highest, Unlimited, RegexBasedAssessmentType.ConceptCheck),
    CourseAssessmentPolicy(AssessmentGradingPolicy.MostRecent, Limited(1), RegexBasedAssessmentType.ModuleQuiz),
    CourseAssessmentPolicy(AssessmentGradingPolicy.Highest, Limited(1), RegexBasedAssessmentType.Assignment),
  )

  private def empty = List.empty[CourseAssessmentPolicy]

  implicit val storageable: CourseStoreable[List[CourseAssessmentPolicy]] =
    CourseStoreable("courseAssessmentPolicies")(empty)
end CourseAssessmentPolicy

sealed trait RegexBasedAssessmentType extends EnumEntry:
  def regex: String

  def doesAssessmentMatch(asset: Asset[?]): Boolean = asset.data match
    case p: PoolAssessment                   => p.title.matches(regex)
    case a: authoring.Assessment             => a.title.matches(regex)
    case d: Diagnostic                       => d.title.matches(regex)
    case assignment: Assignment1             => assignment.title.matches(regex)
    case instrDriven: ObservationAssessment1 => instrDriven.title.matches(regex)
    case _                                   => false
end RegexBasedAssessmentType

object RegexBasedAssessmentType extends Enum[RegexBasedAssessmentType] with ArgonautEnum[RegexBasedAssessmentType]:

  val values = findValues

  case object ConceptCheck extends RegexBasedAssessmentType:
    override def regex: String = "(?i).*(concept check).*"

  case object ModuleQuiz extends RegexBasedAssessmentType:
    override def regex: String = "(?i).*(module).*(quiz).*"

  case object ShowWhatYouKnow extends RegexBasedAssessmentType:
    override def regex: String = "(?i).*(show what you know).*"

  case object Assignment extends RegexBasedAssessmentType:
    override def regex: String = "(?i).*(assignment|essay).*"
end RegexBasedAssessmentType
