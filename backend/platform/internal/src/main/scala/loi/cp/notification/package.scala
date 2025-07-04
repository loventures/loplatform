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

package loi.cp

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.component.query.SchemaBasedTypeQueryHandler
import com.learningobjects.cpxp.service.notification.NotificationConstants.*
import com.learningobjects.cpxp.service.notification.SubscribeFinder

package object notification:

  /** Notification component data model evidence.
    */
  implicit val NotificationDataModel: DataModel[Notification] = DataModel(
    ITEM_TYPE_NOTIFICATION,
    singleton = false,
    schemaMapped = true,
    Map(
      Notification.SenderProperty -> DATA_TYPE_NOTIFICATION_SENDER,
      Notification.TimeProperty   -> DATA_TYPE_NOTIFICATION_TIME,
      Notification.TopicProperty  -> DATA_TYPE_NOTIFICATION_TOPIC
    ),
    Map(Notification.TypeProperty -> classOf[SchemaBasedTypeQueryHandler])
  )

  /** Alert component data model evidence.
    */
  implicit val AlertDataModel: DataModel[Alert] = DataModel(
    ITEM_TYPE_ALERT,
    singleton = true,
    schemaMapped = false,
    Map(
      Alert.CountProperty          -> DATA_TYPE_ALERT_COUNT,
      Alert.TimeProperty           -> DATA_TYPE_ALERT_TIME,
      Alert.ViewedProperty         -> DATA_TYPE_ALERT_VIEWED,
      Alert.AggregationKeyProperty -> DATA_TYPE_ALERT_AGGREGATION_KEY,
      Alert.ContextIdProperty      -> DATA_TYPE_ALERT_CONTEXT
    )
  )

  /** Subscription component data model evidence.
    */
  implicit val SubscriptionDataModel: DataModel[Subscription] = DataModel(
    SubscribeFinder.ITEM_TYPE_SUBSCRIBE,
    singleton = true,
    schemaMapped = false,
    Map(Subscription.ContextIdProperty -> SubscribeFinder.DATA_TYPE_SUBSCRIBE_CONTEXT)
  )
end notification
