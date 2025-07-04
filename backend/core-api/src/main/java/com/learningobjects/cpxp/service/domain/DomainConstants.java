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

package com.learningobjects.cpxp.service.domain;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.data.DataTypes;

/**
 * Domain constants.
 */
public interface DomainConstants {
    public static final String ITEM_TYPE_DOMAIN = "Domain";

    public static final String DATA_TYPE_NAME = DataTypes.DATA_TYPE_NAME;

    public static final String DATA_TYPE_DOMAIN_ID = "Domain.id";

    public static final String DATA_TYPE_DOMAIN_SHORT_NAME = "Domain.shortName";

    public static final String DATA_TYPE_DOMAIN_HOST_NAME = "Domain.hostName";

    public static final String DATA_TYPE_LOGIN_REQUIRED = "Domain.loginRequired";

    public static final String DATA_TYPE_SECURITY_LEVEL = "Domain.securityLevel";

    public static final String DATA_TYPE_LOCALE = "Domain.locale";

    public static final String DATA_TYPE_FAVICON = "Domain.favicon";

    public static final String DATA_TYPE_DOMAIN_CSS = "Domain.css";

    public static final String DATA_TYPE_CSS_FILE = "Domain.cssFile";

    public static final String DATA_TYPE_GUEST_POLICY = "Domain.guestPolicy";

    @DataTypedef(DataFormat.item)
    public static final String DATA_TYPE_TOS_FILE = "Domain.tosFile";

    public static final String DATA_TYPE_DOMAIN_TIME_ZONE = "Domain.timeZone";

    public static final String DATA_TYPE_DOMAIN_STATE = "Domain.state";

    public static final String DATA_TYPE_DOMAIN_MESSAGE = "Domain.message";

    public static final String DATA_TYPE_LICENSE_REQUIRED = "Domain.licenseRequired";

    public static final String DATA_TYPE_SESSION_LIMIT = "Domain.sessionLimit";

    public static final String DATA_TYPE_SESSION_TIMEOUT = "Domain.sessionTimeout";

    public static final String DATA_TYPE_REMEMBER_TIMEOUT = "Domain.rememberTimeout";

    public static final String DATA_TYPE_USERS_LIMIT = "Domain.userLimit";

    public static final String DATA_TYPE_GROUPS_LIMIT = "Domain.groupLimit";

    public static final String DATA_TYPE_MEMBERSHIP_LIMIT = "Domain.membershipLimit";

    public static final String DATA_TYPE_ENROLLMENTS_LIMIT = "Domain.enrollmentLimit";

    public static final String DATA_TYPE_START_DATE = "Domain.startDate";

    public static final String DATA_TYPE_END_DATE = "Domain.endDate";

    public static final String DATA_TYPE_MAXIMUM_FILE_SIZE = "Domain.maximumFileSize";

    public static final String DATA_TYPE_USER_URL_FORMAT = "Domain.userUrlFormat";

    public static final String DATA_TYPE_GROUP_URL_FORMAT = "Domain.groupUrlFormat";

    public static final String DATA_TYPE_DOMAIN_THEME_CUSTOM_COLORS = "Domain.customColors";

    public static final String DATA_TYPE_GOOGLE_ANALYTICS_ACCOUNT = "Domain.googleAnalyticsAccount";

    public static final String DATA_TYPE_DOMAIN_SUPPORT_EMAIL = "Domain.supportEmail";

    @DataTypedef(DataFormat.json)
    public static final String DATA_TYPE_DOMAIN_CONFIGURATION = "Domain.configuration";

    public static final String FOLDER_ID_DOMAIN = "folder-domain";

    public static final String FOLDER_ID_MEDIA = "folder-media";

    public static final String FOLDER_ID_ADMIN = "folder-admin";

    public static final String ID_FAVICON = "favicon";

    public static final String DOMAIN_TYPE_DUMP = "dump";

    public static final String DOMAIN_TYPE_OVERLORD = "overlord";

    public static final String DOMAIN_TYPE_STOCK = "stock";

    /* Version */

    public static final String DATA_TYPE_TERMS_OF_USE_HTML = "Domain.termsOfUseHtml";

    public static final String DATA_TYPE_PRIVACY_POLICY_HTML = "Domain.privacyPolicyHtml";

    String INVALIDATION_KEY_HOST_NAME = "hostName";
}
