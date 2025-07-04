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

package loi.authoring.exchange.imprt.exception

import com.learningobjects.de.web.{InvalidMediaTypeException, UncheckedMessageException}
import loi.cp.i18n.AuthoringBundle

object MissingManifestException
    extends UncheckedMessageException(
      AuthoringBundle
        .message("import.missingManifest")
    )

case class NoMediaTypeForFilenameException(filename: String, id: String)
    extends UncheckedMessageException(
      AuthoringBundle
        .message("import.noMediaTypeForFilename", filename, id)
    )

// we had a non-empty media type as a String but failed to parse it into
// com.learningobjects.de.web.MediaType
case class MediaTypeParseException(
  mediaType: String,
  id: String,
  cause: InvalidMediaTypeException
) extends UncheckedMessageException(
      AuthoringBundle
        .message("import.mediaTypeParseException", mediaType, id, cause.getMessage)
    )

object InvalidManifestZip extends UncheckedMessageException(AuthoringBundle.message("import.invalidManifestZip"))
