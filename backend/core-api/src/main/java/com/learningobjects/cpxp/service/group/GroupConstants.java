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

package com.learningobjects.cpxp.service.group;

import com.fasterxml.jackson.annotation.JsonValue;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.subtenant.SubtenantConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface GroupConstants {
    String ITEM_TYPE_GROUP = "Group";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_GROUP_ID = "groupId";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER = "Group.externalIdentifier";

    @DataTypedef(value = DataFormat.item, itemType = SubtenantConstants.ITEM_TYPE_SUBTENANT)
    String DATA_TYPE_GROUP_SUBTENANT = "Group.subtenant";

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_GROUP_PROJECT = "Group.project";

    @DataTypedef(value = DataFormat.item, itemType = ITEM_TYPE_GROUP)
    String DATA_TYPE_GROUP_MASTER_COURSE = "Group.masterCourse";

    String DATA_TYPE_IMAGE = DataTypes.DATA_TYPE_IMAGE;

    String DATA_TYPE_GROUP_NAME = DataTypes.DATA_TYPE_NAME;

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_GROUP_LINKED_ASSET_ID = "Group.linkedAsset_id";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_GROUP_LINKED_ASSET_NAME = "Group.linkedAsset";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_START_DATE = "startDate";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_END_DATE = "endDate";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_SHUTDOWN_DATE = "shutdownDate";

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_GROUP_BRANCH = "Group.branch";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_GROUP_ARCHIVED = "Group.archived";

    public static final String DATA_TYPE_GROUP_TYPE = DataTypes.DATA_TYPE_TYPE;

    @DataTypedef(DataFormat.number)
    public static final String DATA_TYPE_COMMIT = "Group.commit";

    String ID_FOLDER_GROUPS = "folder-groups";

    String ID_FOLDER_LIBRARIES = "folder-libraries";

    String ID_FOLDER_COURSES = "folder-courses";

    String ID_FOLDER_PREVIEW_SECTIONS = "folder-previewSections";

    String ID_FOLDER_TEST_SECTIONS = "folder-testSections";

    String ID_FOLDER_COURSE_OFFERINGS = "folder-courseOfferings";

    String ID_FOLDER_DEMO_COURSES = "folder-demoCourses";

    String ID_FOLDER_USER_GROUPS = "folder-userGroups";

    enum GroupType {
        // Legacy
        DEFAULT(ID_FOLDER_GROUPS, "default"),
        COURSE(ID_FOLDER_GROUPS, "course"),
        GLOBAL_COURSE(ID_FOLDER_GROUPS, "globalCourse"),
        USER_CREATED(ID_FOLDER_USER_GROUPS, "userCreated"),
        // Non-legacy
        DemoSection(ID_FOLDER_DEMO_COURSES, "DemoSection"), // short term hack
        MasterCourse(ID_FOLDER_LIBRARIES, "MasterCourse"),
        CourseOffering(ID_FOLDER_COURSE_OFFERINGS, "CourseOffering"),
        CourseSection(ID_FOLDER_COURSES, "CourseSection"),
        PreviewSection(ID_FOLDER_PREVIEW_SECTIONS, "PreviewSection"),
        TestSection(ID_FOLDER_TEST_SECTIONS, "TestSection"),
        // Hack
        DOMAIN("", "domain");

        private String _parentFolderId;
        private String _name;

        GroupType(String parentFolderId, String name) {
            _parentFolderId = parentFolderId;
            _name = name;
        }

        public String getFolderId() {
            return _parentFolderId;
        }

        /**
         * @return a pretty name
         */
        @JsonValue
        public String getName() {
            return _name;
        }

        public static GroupType forFolder(String folderId) {
            for (GroupType gt : values())
                if (folderId.equals(gt._parentFolderId))
                    return gt;
            return DEFAULT;
        }

        private static final Map<String, GroupType> nameIndex;

        static {
            var map = new HashMap<String, GroupType>();
            for (GroupType value : GroupType.values()) {
                map.put(value.getName(), value);
            }
            nameIndex = Collections.unmodifiableMap(map);
        }

        public static GroupType forName(String name) {
            return nameIndex.getOrDefault(name, GroupType.DEFAULT);
        }
    }
}
