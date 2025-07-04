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

import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.attachment.ImageFacade;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.integration.IntegrationParentFacade;
import com.learningobjects.cpxp.service.name.UrlFacade;
import com.learningobjects.cpxp.service.subtenant.SubtenantFacade;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.Date;
import java.util.Optional;

/**
 * A facade for users.
 */
@FacadeItem(UserConstants.ITEM_TYPE_USER)
public interface UserFacade extends UrlFacade, IntegrationParentFacade {
    @FacadeData(UserConstants.DATA_TYPE_USER_TITLE)
    public String getUserTitle();
    public void setUserTitle(String title);

    @FacadeData(UserConstants.DATA_TYPE_USER_NAME)
    public String getUserName();
    public void setUserName(String userName);

    @FacadeData(UserConstants.DATA_TYPE_EXTERNAL_ID)
    Optional<String> getUserExternalId();
    void setUserExternalId(Optional<String> externalIdentifier);

    @FacadeData(UserConstants.DATA_TYPE_GIVEN_NAME)
    public String getGivenName();
    public void setGivenName(String givenName);

    @FacadeData(UserConstants.DATA_TYPE_MIDDLE_NAME)
    public String getMiddleName();
    public void setMiddleName(String middleName);

    @FacadeData(UserConstants.DATA_TYPE_FAMILY_NAME)
    public String getFamilyName();
    public void setFamilyName(String familyName);

    @FacadeData(UserConstants.DATA_TYPE_FULL_NAME)
    public void setFullName(String fullName);

    /** Updates the searchable full name from the given/middle/family parts of this user. */
    default String updateFullName() {
        String fullName = StringUtils.defaultString(getGivenName()) + " " + StringUtils.defaultString(getMiddleName())
          + " " + StringUtils.defaultString(getFamilyName());
        setFullName(fullName);
        return fullName;
    }

    @FacadeData(UserConstants.DATA_TYPE_EMAIL_ADDRESS)
    public String getEmailAddress();
    public void setEmailAddress(String emailAddress);

    @FacadeData(DataTypes.DATA_TYPE_IN_DIRECTORY)
    public Boolean getInDirectory();
    public void setInDirectory(Boolean inDirectory);

    @FacadeData(value = UserConstants.DATA_TYPE_IMAGE)
    public ImageFacade getImage();

    @FacadeData(UserConstants.DATA_TYPE_LICENSE_ACCEPTED)
    public Boolean getLicenseAccepted();
    public void setLicenseAccepted(Boolean licenseAccepted);

    @FacadeData(UserConstants.DATA_TYPE_USER_TYPE)
    public UserType getUserType();
    public void setUserType(UserType type);

    @FacadeData(UserConstants.DATA_TYPE_USER_STATE)
    public UserState getUserState();
    public void setUserState(UserState state);

    @FacadeData(UserConstants.DATA_TYPE_RSS_USERNAME)
    public String getRssUsername();
    public void setRssUsername(String rssUsername);

    @FacadeData(UserConstants.DATA_TYPE_RSS_PASSWORD)
    public String getRssPassword();
    public void setRssPassword(String rssPassword);

    @FacadeData(DataTypes.DATA_TYPE_CREATE_TIME)
    public Date getCreateTime();
    public void setCreateTime(Date date);

    @FacadeData(UserConstants.DATA_TYPE_PASSWORD)
    public String getPassword();
    public void setPassword(String password);

    @FacadeData(DataTypes.DATA_TYPE_DISABLED)
    public Boolean getDisabled();
    public void setDisabled(Boolean disabled);

    @FacadeData(UserConstants.DATA_TYPE_USER_SUBTENANT)
    Optional<SubtenantFacade> getSubtenant();
    void setSubtenant(Optional<SubtenantFacade> subtenantFacade);

    /**
     * Wait to obtain a pessimistic lock on the user.
     *
     * @return true if a lock was obtained, false otherwise. Returns false for
     * lock timeout exception
     */
    public boolean lock(long timeoutMillis);

    @FacadeChild(UserConstants.ITEM_TYPE_USER_HISTORY)
    Optional<UserHistoryFacade> getHistory();
    UserHistoryFacade getOrCreateHistory();
}
