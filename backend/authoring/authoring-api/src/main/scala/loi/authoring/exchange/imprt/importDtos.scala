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

package loi.authoring.exchange.imprt

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.authoring.branch.Branch
import org.apache.commons.io.FilenameUtils

// Validated and ready to bake,
// or pre-baked with Some(convertedSource)
case class ValidatedImportDto(
  importName: String,
  dataJson: JsonNode,
  taskReport: TaskReport,
  convertedSource: Option[BlobRef],
  unconvertedSource: BlobRef,
  importType: ThirdPartyImportType
):
  lazy val downloadFilename: String = importType match
    case ThirdPartyImportType.CommonCartridge => s"${FilenameUtils.removeExtension(unconvertedSource.filename)}.imscc"
    case _                                    => unconvertedSource.filename
end ValidatedImportDto

/** @param target
  *   import destination, sometimes rootless
  */
// We already have a LOAF;
// we're just updating some stuff to it and then importing
case class ConvertedImportDto(
  target: Branch,
  importName: String,
  dataJson: JsonNode,
  convertedSource: BlobRef
)

import enumeratum.{Enum, EnumEntry}

sealed trait ThirdPartyImportType extends EnumEntry

object ThirdPartyImportType extends Enum[ThirdPartyImportType]:

  val values = findValues

  case object CommonCartridge extends ThirdPartyImportType
  case object Qti             extends ThirdPartyImportType
  case object OpenStax        extends ThirdPartyImportType
