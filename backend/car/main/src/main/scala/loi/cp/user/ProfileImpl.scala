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

package loi.cp.user

import com.learningobjects.cpxp.component.annotation.{Component, Instance}
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import loi.cp.role.SupportedRoleService
import loi.cp.web.HandleService
import scaloi.syntax.AnyOps.*

import java.lang as jl

@Component
class ProfileImpl(val componentInstance: ComponentInstance)(@Instance user: UserComponent)(implicit
  srs: SupportedRoleService,
  hs: HandleService
) extends Profile
    with ComponentImplementation:

  override def getId: jl.Long = user.getId

  override def getHandle: String = hs.mask(user)

  override def getGivenName: String = user.getGivenName

  override def getFullName: String = user.getFullName

  override def getThumbnailId: Option[Long] =
    Option(user.getImage).map(_.getVersion)

  // I deliberately ignore the version identifier and serve the latest version.
  // The parameter only serves to bust client-side caching.
  override def getThumbnail(id: Long, size: Option[String]): WebResponse =
    Option(user.thumbnail(size.orNull))
      .fold[WebResponse](ErrorResponse.notFound) { attachment =>
        attachment.view(false, false, null) pfTap { case fileResponse: FileResponse[?] =>
          fileResponse.fileInfo.setExpires(
            1000L
          ) // I forget why I do this... especially given that attachment.view says no cache..
        // fileResponse.fileInfo.setDoCache(true)
        // fileResponse.fileInfo.setExpires(60L * 60L * 1000L)
        }
      }
end ProfileImpl
