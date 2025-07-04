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

package loi.cp.imports
package importers

import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.user.{UserConstants, UserFacade, UserFolderFacade}
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.syntax.either.*

trait ImporterWithUser:
  importer: ImporterWithIntegration =>

  import errors.*

  /** Finds user by provided Ids. Assumes ids have been validated so that only one is defined.
    */
  def getUser(
    userName: Option[String],
    userExternalId: Option[String],
    userIntegration: Option[IntegrationImportItem]
  )(implicit fs: FacadeService): PersistError \/ UserComponent =
    (userName, userExternalId, userIntegration) match
      case (Some(s), None, None) => findUserByUserName(s)
      case (None, Some(e), None) => findUserByExternalId(e)
      case (None, None, Some(c)) => findUserByConnector(c)
      case _                     => PersistError("Unknown error").left

  /** Finds optional user by provided Ids. Assumes ids have been validated so that at most one is defined.
    */
  def getUserOption(
    userName: Option[String],
    userExternalId: Option[String],
    userIntegration: Option[IntegrationImportItem]
  )(implicit fs: FacadeService, iws: IntegrationWebService): PersistError \/ Option[UserComponent] =
    if userName.isEmpty && userExternalId.isEmpty && userIntegration.isEmpty then Option.empty[UserComponent].right
    else getUser(userName, userExternalId, userIntegration).map(Some.apply)

  def findUserByConnector(c: IntegrationImportItem): PersistError \/ UserComponent =
    findComponentByConnector[UserComponent](c, UserConstants.ITEM_TYPE_USER)

  private def findUserByUserName(userName: String)(implicit fs: FacadeService): PersistError \/ UserComponent =
    fromNullableUserFacade(userFolder.findUserByUsername(userName), s"User with username: $userName doesn't exist")

  private def fromNullableUserFacade(user: UserFacade, errorMsg: String): PersistError \/ UserComponent =
    Option(user) match
      case Some(u) => u.component[UserComponent].right
      case None    => PersistError(errorMsg).left

  private def findUserByExternalId(externalId: String)(implicit fs: FacadeService): PersistError \/ UserComponent =
    fromNullableUserFacade(
      userFolder.findUserByExternalId(externalId),
      s"User with external id: $externalId doesn't exist"
    )

  private def userFolder(implicit fs: FacadeService) = "folder-users".facade[UserFolderFacade]
end ImporterWithUser
