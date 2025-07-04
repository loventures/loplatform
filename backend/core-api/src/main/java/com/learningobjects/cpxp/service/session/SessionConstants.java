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

package com.learningobjects.cpxp.service.session;

import com.learningobjects.cpxp.util.DateUtils;
import java.text.SimpleDateFormat;

public interface SessionConstants {
    public static final String ITEM_TYPE_SESSION = "Session";

    public static final String DATA_TYPE_SESSION_ID = "Session.sessionId";

    public static final String DATA_TYPE_SESSION_USER = "Session.user";

    public static final String DATA_TYPE_SESSION_REMEMBER = "Session.remember";

    public static final String DATA_TYPE_SESSION_CREATED = "Session.created";

    public static final String DATA_TYPE_SESSION_IP_ADDRESS = "Session.ipAddress";

    public static final String DATA_TYPE_SESSION_NODE_NAME = "Session.nodeName";

    public static final String DATA_TYPE_SESSION_STATE = "Session.state";

    public static final String DATA_TYPE_SESSION_LAST_ACCESS = "Session.lastAccess";

    public static final String DATA_TYPE_SESSION_EXPIRES = "Session.expires";

    public static final String DATA_TYPE_SESSION_PROPERTIES = "Session.properties";

    String ITEM_TYPE_SESSION_STATISTICS = "SessionStatistics";

    String DATA_TYPE_SESSION_STATISTICS_DATE = "SessionStatistics.date";

    String DATA_TYPE_SESSION_STATISTICS_COUNT = "SessionStatistics.count";

    String DATA_TYPE_SESSION_STATISTICS_DURATION = "SessionStatistics.duration";

    SimpleDateFormat SESSION_STATISTICS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");

    long DEFAULT_SESSION_TIMEOUT = DateUtils.Unit.hour.getValue(3);

    long DEFAULT_REMEMBER_TIMEOUT = DateUtils.Unit.week.getValue();
}
