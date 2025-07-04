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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentDecorator
import com.learningobjects.cpxp.component.annotation.{MatrixParam, PathVariable, RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.{Method, WebResponse}
import com.learningobjects.cpxp.service.user.UserConstants
import com.learningobjects.de.web.{Queryable, QueryableProperties}

/** This is a profile view of a user appropriate for return to another user who has the right to see this user.
  * Returning a restricted view of user information to avoid accidental PII spillage.
  */
@Schema("profile")
@QueryableProperties(
  Array(
    new Queryable(
      name = Profile.FullNameProperty,
      handler = classOf[UserFullNameHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE)
    )
  )
)
trait Profile extends ComponentDecorator with Id:
  import Profile.*

  @JsonProperty(HandleProperty)
  def getHandle: String

  @JsonProperty(GivenNameProperty)
  @Queryable(dataType = UserConstants.DATA_TYPE_GIVEN_NAME, traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getGivenName: String

  @JsonProperty(FullNameProperty)
  def getFullName: String

  @JsonProperty
  def getThumbnailId: Option[Long]

  @RequestMapping(path = "thumbnail/{id}", method = Method.GET)
  def getThumbnail(@PathVariable("id") id: Long, @MatrixParam("size") size: Option[String]): WebResponse
end Profile

object Profile:
  final val GivenNameProperty = "givenName"
  final val FullNameProperty  = "fullName"
  final val HandleProperty    = "handle"
