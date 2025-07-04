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

package com.learningobjects.cpxp.service.user;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.data.DataTypes;

public interface UserConstants {
    public static final String ITEM_TYPE_USER = "User";

    public static final String DATA_TYPE_USER_TITLE = "User.title";

    public static final String DATA_TYPE_USER_NAME = "User.userName";

    public static final String DATA_TYPE_EXTERNAL_ID = "User.externalId";

    public static final String DATA_TYPE_GIVEN_NAME = "User.givenName";

    public static final String DATA_TYPE_MIDDLE_NAME = "User.middleName";

    public static final String DATA_TYPE_FAMILY_NAME = "User.familyName";

    public static final String DATA_TYPE_EMAIL_ADDRESS = "User.emailAddress";

    public static final String DATA_TYPE_FULL_NAME = "User.fullName";

    String DATA_TYPE_PASSWORD = "User.password";

    public static final String DATA_TYPE_USER_TYPE = "User.type";

    public static final String DATA_TYPE_USER_STATE = "User.state";

    public static final String DATA_TYPE_LICENSE_ACCEPTED = "User.licenseAccepted";

    String DATA_TYPE_USER_SUBTENANT = "User.subtenant";

    public static final String DATA_TYPE_IMAGE = DataTypes.DATA_TYPE_IMAGE;

    public static String ID_USER_ANONYMOUS = "user-anonymous";

    public static String ID_USER_UNKNOWN = "user-unknown";

    public static String ID_USER_ROOT = "user-root";

    public static String ID_FOLDER_USERS = "folder-users";

    String DATA_TYPE_RSS_USERNAME = "User.rssUsername";

    String DATA_TYPE_RSS_PASSWORD = "User.rssPassword";

    String DATA_TYPE_DISABLED = "disabled";

    String DATA_TYPE_ACCESS_TIME = "UserHistory.accessTime";

    String DATA_TYPE_LOGIN_COUNT = "UserHistory.loginCount";

    String DATA_TYPE_LOGIN_TIME = "UserHistory.loginTime";

    String DATA_TYPE_LOGIN_AUTH_TIME = "UserHistory.authTime";

    String ITEM_TYPE_USER_HISTORY = "UserHistory";

}
