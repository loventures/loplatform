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

package loi.cp.platform

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.learningobjects.cpxp.component.annotation.{Controller, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.service.attachment.ResourceDTO
import com.learningobjects.de.authorization.Secured
import loi.cp.session.SessionComponent
import loi.cp.user.{UserComponent, UserPreferences}

@Controller(value = "lo_platform", category = Controller.Category.CORE, root = true)
trait PlatformInfoApi extends ApiRootComponent:

  /** Collect various data about the current user, session, domain, &c. */
  @RequestMapping(path = "lo_platform", method = Method.GET)
  @Secured(allowAnonymous = true)
  def getPlatformInfo: PlatformInfo

final case class PlatformInfo(
  user: Option[UserInfo],
  session: Option[SessionComponent],
  loggedOut: Boolean,
//  enrollments: Set[EnrollmentComponent],
  domain: DomainInfo,
  adminLink: Option[String],
  authoringLink: Option[String],
  isProduction: Boolean,
  isProdLike: Boolean,
  isOverlord: Boolean,
  clusterType: String,
  clusterName: String,
)

final case class UserInfo(
  @JsonUnwrapped
  component: UserComponent,
  rights: Set[String],
  roles: Set[String],
  preferences: UserPreferences,
)

/* tutorials: {
 *   "assessment.1-instructions: {
 *     "steps": [
 *       { ... the Step type from react-Joyride ... }
 *     ]
 *   }
 * }
 */
final case class DomainInfo(
  id: Long,
  name: String,
  shortName: String,
  hostName: String,
  locale: String,
  timeZone: String,
  favicon: ResourceDTO,
  image: ResourceDTO,
  logo: ResourceDTO,
  logo2: ResourceDTO,
  css: ResourceDTO,
  appearance: Map[String, String],
)
