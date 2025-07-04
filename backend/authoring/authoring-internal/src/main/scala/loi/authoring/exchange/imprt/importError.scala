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

import loi.cp.i18n.{AuthoringBundle, BundleMessage}

// Todo: don't know use BundleMessage, it's too much.
object ImportError:
  val PackageNotAvailable: BundleMessage                                                        = AuthoringBundle.message("import.packageNotAvailable")
  val ImportBlobRequired: BundleMessage                                                         = AuthoringBundle.message("import.required.blobRef")
  def NoSuchImportReceipt(id: Long): BundleMessage                                              = AuthoringBundle.message("import.noSuchReceipt", long2Long(id))
  val ImportReceiptRequired: BundleMessage                                                      = AuthoringBundle.message("import.required.receipt")
  def ImsccFatal(errorGuid: String, e: Exception): BundleMessage                                =
    AuthoringBundle.message("imscc.import.fatalError", errorGuid, e.getMessage)
  def OpenStaxFatal(errorGuid: String, e: Exception): BundleMessage                             =
    AuthoringBundle.message("openstax.import.fatalError", errorGuid, e.getMessage)
  def QtiFatal(errorGuid: String, e: Exception): BundleMessage                                  =
    AuthoringBundle.message("qti.import.fatalError", errorGuid, e.getMessage)
  val UploadExpired: BundleMessage                                                              = AuthoringBundle.message("qti.import.expiredUpload")
  def IllegalAssetFilename(filename: String, nodeId: String): BundleMessage                     =
    AuthoringBundle.message("import.node.illegalFilename", filename, nodeId)
  def MissingAssetFile(filename: String, nodeId: String): BundleMessage                         =
    AuthoringBundle.message("import.node.missingFile", filename, nodeId)
  def CannotReadZip(typ: ThirdPartyImportType, e: Throwable): BundleMessage                     =
    AuthoringBundle.message("import.cannotReadZip", typ.toString, e.getMessage)
  def InvalidManifest(filename: String, typ: ThirdPartyImportType, e: Throwable): BundleMessage =
    AuthoringBundle.message("import.invalidManifest", typ, filename, e.getMessage)
  val QtiImportNotSupported: BundleMessage                                                      = AuthoringBundle.message("qti.import.notSupported")
  val QtiAssessmentFileNotDefined: BundleMessage                                                = AuthoringBundle.message("qti.import.assessmentFileNotDefined")
  def QtiAssessmentFileNotFound(assessmentFileName: String): BundleMessage                      =
    AuthoringBundle.message("qti.import.assessmentFileNotFound", assessmentFileName)
  def QtiAssessmentRefFileNotFound(assessmentFileName: String, id: String): BundleMessage       =
    AuthoringBundle.message("qti.import.assessmentItemRefNotFound", assessmentFileName, id)
  // Because it's too much work to make other specific ones for now
  def OtherError(e: String): BundleMessage                                                      = AuthoringBundle.message("otherError", e)
end ImportError
