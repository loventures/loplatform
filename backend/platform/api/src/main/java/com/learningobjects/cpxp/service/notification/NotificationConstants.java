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

package com.learningobjects.cpxp.service.notification;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;

public class NotificationConstants {

    public static final String DATA_TYPE_NOTIFICATION_PROCESSED = "Notification.processed";

    public static final String DATA_TYPE_NOTIFICATION_TIME = "Notification.time";

    public static final String DATA_TYPE_NOTIFICATION_SENDER = "Notification.sender";

    public static final String DATA_TYPE_NOTIFICATION_CONTEXT = "Notification.context";

    public static final String DATA_TYPE_NOTIFICATION_DATA = "Notification.data";

    public static final String DATA_TYPE_NOTIFICATION_TOPIC = "Notification.topic";

    /** deprecated */
    public static final String DATA_TYPE_NOTIFICATION_TOPIC_ID = "Notification.topic_id";

    public static final String ITEM_TYPE_NOTIFICATION = "Notification";

    @DataTypedef(DataFormat.time)
    public static final String DATA_TYPE_USER_ALERT_VIEW_TIME = "Alert.viewTime";

    public static final String DATA_TYPE_ALERT_TIME = "Alert.time";

    public static final String DATA_TYPE_ALERT_COUNT = "Alert.count";

    public static final String DATA_TYPE_ALERT_CONTEXT = "Alert.context";

    public static final String DATA_TYPE_ALERT_AGGREGATION_KEY = "Alert.aggregationKey";

    public static final String DATA_TYPE_ALERT_NOTIFICATION = "Alert.notification";

    public static final String DATA_TYPE_ALERT_VIEWED = "Alert.viewed";

    public static final String ITEM_TYPE_ALERT = "Alert";


}
