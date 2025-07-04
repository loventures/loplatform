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

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Schema
import com.learningobjects.cpxp.component.{ComponentInterface, ComponentService}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.web.Queryable.Trait
import com.learningobjects.de.web.{Queryable, QueryableProperties}
import loi.cp.email.Email

@QueryableProperties(
  value = Array(
    new Queryable(name = Notification.TypeProperty, traits = Array(Trait.NOT_SORTABLE))
  )
)
@Schema("notification")
trait Notification extends ComponentInterface with Id:
  import Notification.*

  /** The type of data needed to initialise this notification. */
  type Init

  /** The sender of this notification. This would be the author of an IM or a post. */
  @JsonProperty(SenderProperty)
  @Queryable
  def getSender: Option[Long]

  /** The context (course) in which this notification occurred. */
  @JsonProperty
  def getContext: Option[Long]

  /** The time of this notification. This is unsortable because of the indirection we use to get notifications. We pull
    * notifications from notifies, which have their own timestamp that is separate from the notification's timestamp.
    */
  @JsonProperty(TimeProperty)
  @Queryable(
    traits = Array(Trait.NOT_SORTABLE)
  )
  def getTime: Date

  /** The topic of this notification. This is an opaque string that may be used for filtering notifications; e.g. by
    * content identifier.
    */
  @JsonProperty
  @Queryable
  def getTopic: Option[String]

  /** Used to control how the notification should be displayed to the user. */
  @JsonProperty
  def urgency: NotificationUrgency

  /* The following methods are used during notification processing and do not form the JSON API. */

  /** The subscription path of this notification. If a notification defines a subscription path then a web controller
    * can provide a facility for users to subscribe to any prefix of that path and automatically receive those
    * notifications. For example, discussion posts provide a subscription path including the discussion id and the
    * thread id; users can then subscribe to either a thread or the discussion.
    */
  def subscriptionPath: Option[SubscriptionPath]

  /** The intended audience of this notification. The users will always receive the notification unless they have muted
    * it.
    */
  def audience: Iterable[Long]

  /** The intended interest level of this notification. */
  def interest: Interest // TODO: This conflates interest with priority. Add Priority and figure out a mapping.

  /** For alerting, the aggregation key for this notification. If multiple notifications have the same aggregation key
    * then they will only generate a single alert.
    */
  def aggregationKey: Option[String]

  /** Bindings for the notification's translation keys. */
  // TODO: why does this take a `ComponentService`?! also, why is it implicit?
  def bindings(implicit domain: DomainDTO, user: UserDTO, cs: ComponentService): Map[String, Any]

  /** Filter the observers of this event to those permitted to see it. */
  // def filterObservers(observers: Seq[Long]): Seq[Long]

  /** Get email info for this notification. */
  def emailInfo: Option[EmailInfo[? <: Email]]

  /** The schema name for this notification. */
  def `type`: String
end Notification

/** Notification component companion.
  */
object Notification:

  /** The type property. */
  final val TypeProperty = "_type"

  /** The sender property. */
  final val SenderProperty = "sender"

  /** The time property */
  final val TimeProperty = "time"

  /** The topic property */
  final val TopicProperty = "topic"

  case class EmailInfo[A <: Email](impl: Class[A], init: Email.Init)

  type Aux[K] = Notification { type Init = K }
end Notification

type NotificationInit[N <: Notification] = N match
  case Notification.Aux[k] => k
