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

package loi.authoring.blob

import com.learningobjects.de.web.{MediaType, UncheckedMessageException}
import loi.cp.i18n.AuthoringBundle

object exception:

  case class IllegalBlobName(name: String)
      extends UncheckedMessageException(AuthoringBundle.message("blob.illegalName", name))
  case class IllegalMediaType(mediaType: MediaType)
      extends UncheckedMessageException(AuthoringBundle.message("blob.illegalMediaType", mediaType))
  case class NoSuchProvider(name: String, cause: Throwable)
      extends UncheckedMessageException(AuthoringBundle.message("blob.noSuchProvider", name), cause)
  case class NoSuchBlobRef(nodeId: Long)
      extends UncheckedMessageException(AuthoringBundle.message("blob.noSuchRef", nodeId.toString))
end exception
