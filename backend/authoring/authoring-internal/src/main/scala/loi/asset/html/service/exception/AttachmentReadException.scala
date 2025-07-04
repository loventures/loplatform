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

package loi.asset.html.service.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.asset.html.model.Html
import loi.authoring.asset.Asset
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

case class AttachmentReadException(attachmentId: Long, cause: Throwable)
    extends UncheckedMessageException(
      BundleMessage("html.attachment.readFailure", long2Long(attachmentId), cause.getLocalizedMessage)(using
        AuthoringBundle.bundle
      ),
      cause
    )

case class HtmlRenderException(html: Asset[Html], cause: Throwable)
    extends UncheckedMessageException(
      BundleMessage("html.render.failure", long2Long(html.info.id), cause.getLocalizedMessage)(using
        AuthoringBundle.bundle
      ),
      cause
    )
