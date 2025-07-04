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

package loi.cp.quiz.question

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.reference.VersionedAssetReference

/** Trait representing any piece of information meant to relay information relating to the correctness or incorrectness
  * of a response to a question.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
  Array(
    new Type(name = "rationale", value = classOf[CorrectRationale]),
    new Type(name = "textRemediation", value = classOf[TextRemediation]),
    new Type(name = "assetRemediation", value = classOf[AssetRemediation])
  )
)
sealed trait Rationale

/** An explanation for why a response to a question is correct.
  *
  * @param reason
  *   The explanation for a question's correctness.
  */
case class CorrectRationale(reason: String) extends Rationale

/** An explanation as to why a response to a question is incorrect.
  */
sealed trait Remediation extends Rationale

/** A textual explanation for why a response to the question is wrong.
  *
  * @param reason
  *   the explanation for a question's incorrectness
  */
case class TextRemediation(reason: String) extends Remediation

/** An asset reference that can be used to explain a question in more detail when a response to a question is wrong.
  *
  * @param title
  *   the asset's title
  * @param reference
  *   a reference to the full asset
  * @param assetType
  *   the type of asset being used as remediation
  */
case class AssetRemediation(title: String, reference: VersionedAssetReference, assetType: AssetTypeId)
    extends Remediation
