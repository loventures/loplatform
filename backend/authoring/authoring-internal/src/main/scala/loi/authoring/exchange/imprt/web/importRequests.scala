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

package loi.authoring.exchange.imprt.web

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import enumeratum.EnumEntry.Uncapitalised
import enumeratum.{Enum, EnumEntry}
import loi.authoring.blob.BlobRef

import java.time.LocalDate

// Used for importing already-converted LO zips in one step
// LOAF: Learning Objects Authoring Format
case class LoafImportRequest(
  description: String,
  source: BlobRef
)

case class ProjectLoafImportRequest(
  projectName: String,
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[LocalDate],
  liveVersion: Option[String],
  s3: Option[String],
  projectStatus: Option[String],
  courseStatus: Option[String],
  source: BlobRef,
)

/** Web request for step 2 of Common Cartridge/OpenStax import
  */
private[imprt] case class CcOsImportRequest(
  description: String,
  receiptId: Long
)

/** The ways that we process qti.
  *
  * CAUTION, the toString's enum entries are used in Jackson type info annotations below
  */
private[imprt] sealed trait QtiImportType extends EnumEntry with Uncapitalised
private[imprt] object QtiImportType       extends Enum[QtiImportType]:
  val values: IndexedSeq[QtiImportType] = findValues

  /** The questions are imported into a new assessment root node
    */
  case object AssessmentQuestions extends QtiImportType

  /** The questions are imported as root nodes
    */
  case object PlainQuestions extends QtiImportType
end QtiImportType

/** Web request for step 1 of QTI import
  */
private[imprt] case class QtiValidateRequest(
  unconvertedSource: BlobRef,
  `type`: QtiImportType
)

/** Web request for step 2 of QTI import. QTI imports are full of questions, sometimes we wrap them in one of our
  * assessment types, sometimes we just bring the questions in as roots (for later connection to some existing
  * assessment typically)
  */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[AssessmentQuestions], name = "assessmentQuestions"),
    new JsonSubTypes.Type(value = classOf[PlainQuestions], name = "plainQuestions")
  )
)
private[imprt] sealed trait QtiImportRequest:
  val receiptId: Long
end QtiImportRequest

private[imprt] case class AssessmentQuestions(
  description: String,
  assessmentType: String,
  assessmentTitle: Option[String],
  receiptId: Long
) extends QtiImportRequest

private[imprt] case class PlainQuestions(
  description: String,
  receiptId: Long
) extends QtiImportRequest
