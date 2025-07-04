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

package loi.cp.domain
import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.{UserConstants, UserFolderFacade, UserType}
import loi.cp.user.{UserComponent, UserParentFacade}
import cats.effect.IO

/** Functions for sudoing into a tenant domain from overlord.
  */
final class OverlordDomainSudo(implicit fs: FacadeService, qs: QueryService):
  import OverlordDomainSudo.*

  /** An overlord user
    */
  type Overlord = UserComponent

  /** A lowly domain user.
    */
  type Peon    = UserComponent
  type Domain  = DomainDTO
  type Session = Unit

  /** A root to store domain users.
    */
  type OverlordParent = UserParentFacade

  type Clone   = (Overlord, Domain) => IO[Peon]
  /* An effect that persists a given DomainUser
   */
  type Persist = Peon => IO[OverlordParent]
  type Login   = Peon => IO[Session]
  type Sudo    = (Overlord, Domain) => IO[Session]

  type FetchParent = Domain => IO[OverlordParent]
  type AddUser     = (OverlordParent, Peon) => IO[OverlordParent]

  /** Create a domain user for a overlord user.
    *
    * @return
    *   the new domain user
    */
  val replicate: Clone = (overlordUser, domain) =>
    for
      folder           <- overlordUserFolder(domain)
      newUser          <- IO {
                            val init = new UserComponent.Init
                            init.userName = overlordUser.getUserName
                            init.emailAddress = overlordUser.getEmailAddress
                            init.givenName = overlordUser.getGivenName
                            init.middleName = overlordUser.getMiddleName
                            init.familyName = overlordUser.getFamilyName
                            init.userType = UserType.Overlord
                            init.state = overlordUser.getUserState
                            init.title = overlordUser.getTitle
                            init.url = null
                            folder.getOrCreateUserByUsername(overlordUser.getUserName, init).result
                          }
      newUserComponent <- IO { newUser.component[UserComponent] }
      _                <- IO { newUserComponent.setGivenName(overlordUser.getGivenName) }
      _                <- IO { newUserComponent.setMiddleName(overlordUser.getMiddleName) }
      _                <- IO { newUserComponent.setFamilyName(overlordUser.getFamilyName) }
      _                <- IO { newUserComponent.setEmailAddress(overlordUser.getEmailAddress) }
    yield newUserComponent

  val overlordUserFolder: FetchParent = domain =>
    for
      item   <- IO(Option(domain.getFolderById(FOLDER_NAME_OVERLORD_USERS)))
      facade <- item match
                  case None    => createNewOverlordFolder(domain)
                  case Some(i) => IO(i.facade[UserParentFacade])
    yield facade

  def createNewOverlordFolder(domain: Domain): IO[OverlordParent] = IO {
    val uff = fs.addFacade(domain.id, classOf[UserFolderFacade])
    uff.setIdStr(FOLDER_NAME_OVERLORD_USERS)
    uff.setName(FOLDER_NAME_OVERLORD_USERS)
    uff.facade[UserParentFacade]
  }

  /** Have a overlord user login into a domain, shadow version of the overlord user.
    * @param clone
    *   to clone the overlord user into a domain user
    * @param impersonate
    *   logs in as the given domain user
    * @param persist
    *   persists the given domain user, if necessary.
    * @return
    */
  def infiltrate(clone: Clone, impersonate: Login, persist: Persist): Sudo =
    (overlordUser: Overlord, domain: Domain) =>
      for
        doppelganger <- clone(overlordUser, domain)
        _            <- persist(doppelganger)
        session      <- impersonate(doppelganger)
      yield session
end OverlordDomainSudo
object OverlordDomainSudo:
  final val FOLDER_NAME_OVERLORD_USERS                          = "folder-overlord-users"
  implicit val userComponentDataModel: DataModel[UserComponent] = DataModel(
    itemType = UserConstants.ITEM_TYPE_USER,
    singleton = false,
    schemaMapped = false,
    dataTypes = Map()
  )
