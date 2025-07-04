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

package loi.cp.asset.assessmenttype

import com.learningobjects.cpxp.exception.NoTypeClassMemberException
import loi.asset.assessment.model.*
import loi.asset.discussion.model.Discussion1
import loi.authoring.asset.Asset
import scaloi.syntax.OptionOps.*

trait WithAssetAssessmentType[A]:

  def assessmentType(value: A): AssetAssessmentType

object WithAssetAssessmentType:

  // these are not good names

  implicit object AssessmentAssetAssessmentType extends WithAssetAssessmentType[Assessment]:
    override def assessmentType(value: Assessment): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  implicit object CheckpointAssetAssessmentType extends WithAssetAssessmentType[Checkpoint]:
    override def assessmentType(value: Checkpoint): AssetAssessmentType =
      AssetAssessmentType.Formative

  implicit object PoolAssessmentAssetAssessmentType extends WithAssetAssessmentType[PoolAssessment]:
    override def assessmentType(value: PoolAssessment): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  implicit object Assignment1AssetAssessmentType extends WithAssetAssessmentType[Assignment1]:
    override def assessmentType(value: Assignment1): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  implicit object Discussion1AssetAssessmentType extends WithAssetAssessmentType[Discussion1]:
    override def assessmentType(value: Discussion1): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  implicit object ObservationAssessment1AssessmentType extends WithAssetAssessmentType[ObservationAssessment1]:
    override def assessmentType(value: ObservationAssessment1): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  implicit object DiagnosticAssessmentType extends WithAssetAssessmentType[Diagnostic]:
    override def assessmentType(value: Diagnostic): AssetAssessmentType =
      AssetAssessmentType.withName(value.assessmentType.entryName)

  /** Type classes are statically dispatched, so this is the closest we can get to the dynamic dispatch of subtype
    * polymorphism. This closes the system for extension which is unfortunate, but 1) it is better than putting
    * non-authoring traits in the type hierarchy of asset types and 2) the system wasn't open anyway (front-end apps and
    * provisioning can't be), so having islands of open extensibility is pointless. This means that this class must have
    * a compile-time dependency on all asset types that are WithAssetAssessmentType.
    */
  def maybeFromAsset(asset: Asset[?]): Option[AssetAssessmentType] =
    asset match
      case Assessment.Asset(a)             => Option(AssessmentAssetAssessmentType.assessmentType(a.data))
      case Checkpoint.Asset(a)             => Option(CheckpointAssetAssessmentType.assessmentType(a.data))
      case PoolAssessment.Asset(a)         => Option(PoolAssessmentAssetAssessmentType.assessmentType(a.data))
      case Assignment1.Asset(a)            => Option(Assignment1AssetAssessmentType.assessmentType(a.data))
      case Discussion1.Asset(a)            => Option(Discussion1AssetAssessmentType.assessmentType(a.data))
      case ObservationAssessment1.Asset(a) =>
        Option(ObservationAssessment1AssessmentType.assessmentType(a.data))
      case Diagnostic.Asset(a)             => Option(DiagnosticAssessmentType.assessmentType(a.data))
      case _                               => None

  def requireFromAsset[A](asset: Asset[A]): AssetAssessmentType =
    maybeFromAsset(asset)
      .toTry(NoTypeClassMemberException[A, WithAssetAssessmentType](asset.getClass))
      .get
end WithAssetAssessmentType
