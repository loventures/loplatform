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

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.attachment.ImageFacade;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.integration.IntegrationParentFacade;
import com.learningobjects.cpxp.service.name.UrlFacade;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.cpxp.service.relationship.RoleFacade;

import java.util.Date;
import java.util.List;
import java.util.Optional;


/**
 * A facade for groups.
 */
@FacadeItem(GroupConstants.ITEM_TYPE_GROUP)
public interface GroupFacade extends UrlFacade, IntegrationParentFacade {
    @FacadeData(GroupConstants.DATA_TYPE_GROUP_ID)
    String getGroupId();
    void setGroupId(String groupId);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER)
    Optional<String> getGroupExternalId();
    void setGroupExternalId(Optional<String> externalIdentifier);

    @FacadeData(DataTypes.DATA_TYPE_NAME)
    String getName();
    void setName(String name);

    @FacadeData(DataTypes.DATA_TYPE_DESCRIPTION)
    String getDescription();
    void setDescription(String description);

    @FacadeData(GroupConstants.DATA_TYPE_START_DATE)
    Date getStartDate();
    void setStartDate(Date start);

    @FacadeData(GroupConstants.DATA_TYPE_END_DATE)
    Date getEndDate();
    void setEndDate(Date end);

    @FacadeData(DataTypes.DATA_TYPE_IN_DIRECTORY)
    Boolean getInDirectory();
    void setInDirectory(Boolean in);

    @FacadeData(value = DataTypes.DATA_TYPE_IMAGE)
    ImageFacade getImage();

    @FacadeData(value = DataTypes.DATA_TYPE_LOGO)
    ImageFacade getLogo();

    @FacadeData(value = DataTypes.DATA_TYPE_ARCHETYPE)
    Long getArchetype();

    @FacadeData(DataTypes.DATA_TYPE_UNAVAILABLE)
    Boolean getUnavailable();
    void setUnavailable(Boolean un);

    @FacadeData(DataTypes.DATA_TYPE_DISABLED)
    Boolean getDisabled();
    void setDisabled(Boolean d);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_TYPE)
    GroupConstants.GroupType getGroupType();
    void setGroupType(GroupConstants.GroupType type);

    @FacadeData(RelationshipConstants.DATA_TYPE_SUPPORTED_ROLE)
    List<RoleFacade> getSupportedRoles();
    void addSupportedRole(RoleFacade role);
    void addSupportedRole(String role);

    @FacadeChild(RelationshipConstants.ITEM_TYPE_ROLE)
    List<RoleFacade> getLocalRoles();
    RoleFacade addLocalRole();

    @FacadeData(DataTypes.DATA_TYPE_CREATE_TIME)
    public Date getCreateTime();
    public void setCreateTime(Date date);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_BRANCH)
    public Long getBranchId();
    public void setBranchId(long branchId);

    @FacadeData(GroupConstants.DATA_TYPE_GROUP_PROJECT)
    Optional<Long> getProjectId();
    void setProjectId(long projectId);

    @FacadeData(GroupConstants.DATA_TYPE_COMMIT)
    Optional<Long> getCommitId();
    void setCommitId(Optional<Long> commitId);

    /**
     * Json blob properties
     */
    @FacadeJson
    <T> T getProperty(String propName, Class<T> type);
    void setProperty(String propName, Object value);

    // Manually set group name.
    // When attaching handler to this facade, the framework can't infer the group name...
    // should it be 'Properties' or 'Propertieses'? The return type (JsonNode) doesn't disambiguate b/c
    // it can be a single thing or (in this case) many things.
    @FacadeGroup(value = "Properties")
    JsonNode getProperties();

    @FacadeParent
    GroupParentFacade getFolder();
}
