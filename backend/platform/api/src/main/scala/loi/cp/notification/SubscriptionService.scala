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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId

@Service
trait SubscriptionService:
  def subscribe(usr: UserId, crs: ContextId, path: SubscriptionPath, interest: Interest): Unit
  def unsubscribe(usr: UserId, crs: ContextId, path: SubscriptionPath): Unit
  def unsubscribe(usr: UserId, crs: ContextId): Unit
  def subscriptions(usr: UserId, crs: ContextId): Map[SubscriptionPath, Interest]
  def interest(usr: UserId, crs: ContextId, path: SubscriptionPath): Option[Interest]

  // map from target users to their highest interest level in any one of the specified objects or subject
  def findSubscribers(crs: ContextId, path: SubscriptionPath): Map[Long, Interest]
end SubscriptionService
