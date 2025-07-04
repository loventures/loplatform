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

package loi.cp.lti;

public class LTIConstants {

    // versions
    public static final String VERSION_1_0 = "V1.0";
    public static final String VERSION_1_1 = "V1.1";
    public static final String VERSION_2_0 = "V2.0";

    public static final String CONTEXT_ID = "context_id";
    public static final String CONTEXT_TYPE = "context_type";
    public static final String CONTEXT_TITLE = "context_title";
    public static final String CONTEXT_LABEL = "context_label";

    public static final String LTI_MESSAGE_TYPE = "lti_message_type";
    public static final String BASIC_LTI_LAUNCH_REQUEST = "basic-lti-launch-request";

    public static final String LTI_VERSION = "lti_version";
    public static final String LTI_1P0 = "LTI-1p0";
    public static final String LTI_1P3 = "LTI-1p3";

    public static final String RESOURCE_LINK_ID = "resource_link_id";
    public static final String RESOURCE_LINK_TITLE = "resource_link_title";
    public static final String RESOURCE_LINK_DESCRIPTION = "resource_link_description";
    public static final String USER_ID = "user_id";
    public static final String USER_IMAGE = "user_image";
    public static final String ROLES = "roles";

    public static final String LIS_PERSON_NAME_GIVEN = "lis_person_name_given";
    public static final String LIS_PERSON_NAME_FAMILY = "lis_person_name_family";
    public static final String LIS_PERSON_NAME_FULL = "lis_person_name_full";
    public static final String LIS_PERSON_CONTACT_EMAIL_PRIMARY = "lis_person_contact_email_primary";

    public static final String LAUNCH_PRESENTATION_LOCALE = "launch_presentation_locale";
    public static final String LAUNCH_PRESENTATION_DOCUMENT_TARGET = "launch_presentation_document_target";
    public static final String LAUNCH_PRESENTATION_CSS_URL = "launch_presentation_css_url";
    public static final String LAUNCH_PRESENTATION_WIDTH = "launch_presentation_width";
    public static final String LAUNCH_PRESENTATION_HEIGHT = "launch_presentation_height";
    public static final String LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";

    public static final String TOOL_CONSUMER_INFO_PRODUCT_FAMILY_CODE = "tool_consumer_info_product_family_code";
    public static final String TOOL_CONSUMER_INFO_VERSION = "tool_consumer_info_version";
    public static final String TOOL_CONSUMER_INSTANCE_GUID = "tool_consumer_instance_guid";
    public static final String TOOL_CONSUMER_INSTANCE_NAME = "tool_consumer_instance_name";
    public static final String TOOL_CONSUMER_INSTANCE_DESCRIPTION = "tool_consumer_instance_description";
    public static final String TOOL_CONSUMER_INSTANCE_URL = "tool_consumer_instance_url";
    public static final String TOOL_CONSUMER_INSTANCE_CONTACT_EMAIL = "tool_consumer_instance_contact_email";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    public static final String OAUTH_NONCE = "oauth_nonce";
    public static final String OAUTH_VERSION = "oauth_version";
    public static final String OAUTH_SIGNATURE = "oauth_signature";
    public static final String OAUTH_CALLBACK = "oauth_callback";

    // LTI Outcomes.
    public static final String LIS_RESULT_SOURCEDID = "lis_result_sourcedid";
    public static final String LIS_OUTCOME_SERVICE_URL = "lis_outcome_service_url";

    // general claims
    public static final String MESSAGE_TYPE_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    // ok, these next few (without URLs) are technically "non-claim data", but they function similarly
    public static final String GIVEN_NAME_CLAIM = "given_name";
    public static final String FAMILY_NAME_CLAIM = "family_name";
    public static final String MIDDLE_NAME_CLAIM = "middle_name";
    public static final String PICTURE_CLAIM = "picture";
    public static final String EMAIL_CLAIM = "email";
    public static final String NAME_CLAIM = "name";
    public static final String NONCE_CLAIM = "nonce";
    public static final String VERSION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/version";
    public static final String LOCALE_CLAIM = "locale";
    public static final String RESOURCE_LINK_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/resource_link";
    public static final String CONTEXT_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/context";
    public static final String ROLES_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/roles";
    public static final String TOOL_PLATFORM_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/tool_platform";
    public static final String LIS_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/lis";
    public static final String ASSIGNMENT_GRADE_SERVICE_CLAIM = "https://purl.imsglobal.org/spec/lti-ags/claim/endpoint";
    public static final String NAMES_ROLE_SERVICE_CLAIM = "https://purl.imsglobal.org/spec/lti-nrps/claim/namesroleservice";
    public static final String CALIPER_SERVICE_CLAIM = "https://purl.imsglobal.org/spec/lti-ces/claim/caliper-endpoint-service";
    public static final String PRESENTATION_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";
    public static final String CUSTOM_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/custom";
    public static final String TARGET_LINK_URI_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/target_link_uri";
    public static final String ROLE_SCOPE_MENTOR_CLAIM = "https://purlimsglobal.org/spec/lti/claim/role_scope_mentor";

    public static final String RESOURCE_LINK_MESSAGE_TYPE = "LtiResourceLinkRequest";
    public static final String DEEP_LINK_MESSAGE_TYPE = "LtiDeepLinkingRequest";

    // deep linking claims
    public static final String DEPLOYMENT_ID_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    public static final String AUTHORIZED_PART_CLAIM = "azp";
    public static final String DEEP_LINKING_SETTINGS_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings";
    public static final String DEEP_LINKING_CONTENT_ITEMS_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";
    public static final String DEEP_LINKING_MESSAGE_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/msg";
    public static final String DEEP_LINKING_LOG_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/log";
    public static final String DEEP_LINKING_ERROR_MESSAGE_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/errormsg";
    public static final String DEEP_LINKING_ERROR_LOG_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/errorlog";
    public static final String DEEP_LINKING_DATA_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/data";

    // group set claim
    public static final String GROUPS_SERVICE_CLAIM = "https://purl.imsglobal.org/spec/lti-gs/claim/groupsservice";

    // System roles
    public static final String ACCOUNT_ADMIN_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/system/person#AccountAdmin";
    public static final String CREATOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/system/person#Creator";
    public static final String SYS_ADMIN_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/system/person#SysAdmin";
    public static final String SYS_SUPPORT_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/system/person#SysSupport";
    public static final String USER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/system/person#User";

    // Institution roles
    public static final String INSTITUTION_ADMINISTRATOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Administrator";
    public static final String FACULTY_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Faculty";
    public static final String GUEST_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Guest";
    public static final String NONE_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#None";
    public static final String OTHER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Other";
    public static final String STAFF_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Staff";
    public static final String STUDENT_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Student";
    public static final String ALUMNI_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Alumni";
    public static final String INSTITUTION_INSTRUCTOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor";
    public static final String INSTITUTION_LEARNER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Learner";
    public static final String INSTITUTION_MEMBER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Member";
    public static final String INSTITUTION_MENTOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Mentor";
    public static final String OBSERVER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Observer";
    public static final String PROSPECTIVE_STUDENT_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#ProspectiveStudent";

    // Context roles
    public static final String ADMINISTRATOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Administrator";
    public static final String CONTENT_DEVELOPER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#ContentDeveloper";
    public static final String INSTRUCTOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";
    public static final String LEARNER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
    public static final String MENTOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Mentor";
    public static final String MANAGER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Manager";
    public static final String MEMBER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Member";
    public static final String OFFICER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Officer";

    // Scopes
    public static final String AGS_LINE_ITEM_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem";
    public static final String AGS_LINE_ITEM_READONLY_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem.readonly";
    public static final String AGS_RESULT_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly";
    public static final String AGS_SCORE_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/score";
    public static final String NAMES_AND_ROLES_SCOPE = "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly";
    public static final String CALIPER_SCOPE = "https://purl.imsglobal.org/spec/lti-ces/v1p0/scope/send";

    private LTIConstants() {
    }
}
