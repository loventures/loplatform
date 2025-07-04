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

package loi.authoring.attachment.service.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.Asset
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

import java.util.ResourceBundle

object `package`:
  private[exception] implicit val bundle: ResourceBundle = AuthoringBundle.bundle

/** Exception for an asset that doesn't have an attachment id
  */
final case class AssetHasNoAttachment(asset: Asset[?])
    extends UncheckedMessageException(BundleMessage("attachment.fetch.assetHasNoAttachment", asset.info.id.toString))

final case class ZipAttachmentFileNotFound(nodeId: Long, path: String)
    extends UncheckedMessageException(BundleMessage("attachment.fetch.zipFileNotFound", nodeId.toString, path))
