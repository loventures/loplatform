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

package loi.authoring.render

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.Asset
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

import java.util.ResourceBundle

object RenderFailures:

  implicit private val bundleMessage: ResourceBundle = AuthoringBundle.bundle

  final case class RenderFailure(assetId: Long)
      extends UncheckedMessageException(BundleMessage("asset.render.failure", long2Long(assetId)))

  final case class UnrenderableAssetType(asset: Asset[?])
      extends UncheckedMessageException(
        BundleMessage("asset.render.unrenderableType", Long box asset.info.id, asset.info.typeId)
      )

  final case class AssetCannotBePrinted(asset: Asset[?])
      extends UncheckedMessageException(
        BundleMessage("print.assetCannotBePrinted", long2Long(asset.info.id), asset.info.typeId.toString)(using
          AuthoringBundle.bundle
        )
      )
end RenderFailures
