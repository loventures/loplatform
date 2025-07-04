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

package loi.cp.notification

import java.lang.Long as JLong

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, Mode}
import com.learningobjects.de.authorization.Secured

/** Root for viewing your subscriptions. Subscriptions are managed by subject-specific controllers. Mostly this serves
  * testing purposes.
  */
@Controller(value = "subscriptions", root = true)
@RequestMapping(path = "subscriptions")
@Secured(allowAnonymous = false)
trait SubscriptionRootApi extends ApiRootComponent:
  import SubscriptionRootApi.*

  /** Get your subscriptions. */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[Subscription]

  /** Get my interest in something. */
  @RequestMapping(path = "interest", method = Method.POST, mode = Mode.READ_ONLY)
  def interest(@RequestBody request: InterestRequest): InterestLevel
end SubscriptionRootApi

object SubscriptionRootApi:

  /** Interest level */
  final case class InterestLevel(interest: Option[Interest])

  final case class InterestRequest(
    context: Long,
    @JsonDeserialize(contentAs = classOf[JLong]) sender: Option[Long],
    path: List[String]
  )
end SubscriptionRootApi
