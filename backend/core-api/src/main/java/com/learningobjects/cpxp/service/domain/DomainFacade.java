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

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.portal.NameFacade;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.learningobjects.cpxp.service.data.DataTypes.*;
import static com.learningobjects.cpxp.service.domain.DomainConstants.*;

@FacadeItem(value = ITEM_TYPE_DOMAIN)
public interface DomainFacade extends NameFacade {

    @FacadeData(DATA_TYPE_DOMAIN_ID)
    public String getDomainId();
    public void setDomainId(String domainId);

    @FacadeData(DATA_TYPE_DOMAIN_SHORT_NAME)
    public String getShortName();
    public void setShortName(String shortName);

    @FacadeData(DATA_TYPE_DOMAIN_HOST_NAME)
    public String getPrimaryHostName();
    public void setPrimaryHostName(String primaryHostName);

    @FacadeData(DATA_TYPE_HOST_NAME)
    public List<String> getHostNames();
    public void setHostNames(List<String> hostNames);

    @FacadeData(DATA_TYPE_TYPE)
    public String getType();

    @FacadeData(DATA_TYPE_FAVICON)
    public AttachmentFacade getFavicon();
    public void setFavicon(Long favicon);

    @FacadeData(DATA_TYPE_IMAGE)
    public AttachmentFacade getImage();
    public void setImage(Long image);

    @FacadeData(DATA_TYPE_LOGO)
    public AttachmentFacade getLogo();
    public void setLogo(Long logo);

    @FacadeData(DATA_TYPE_LOGO2)
    public AttachmentFacade getLogo2();
    public void setLogo2(Long logo);

    @FacadeData(DATA_TYPE_DOMAIN_CSS)
    public AttachmentFacade getCss();

    @FacadeData(DATA_TYPE_CSS_FILE)
    public NameFacade getCssFile();

    @FacadeData(DATA_TYPE_LOCALE)
    public String getLocale();
    public void setLocale(String locale);

    @FacadeData(DATA_TYPE_DOMAIN_TIME_ZONE)
    public String getTimeZone();
    public void setTimeZone(String timeZone);

    @FacadeData(DATA_TYPE_SECURITY_LEVEL)
    public SecurityLevel getSecurityLevel();
    public void setSecurityLevel(SecurityLevel securityLevel);

    @FacadeData(DATA_TYPE_START_DATE)
    public Date getStartDate();

    @FacadeData(DATA_TYPE_END_DATE)
    public Date getEndDate();

    @FacadeData(DATA_TYPE_DOMAIN_STATE)
    public DomainState getState();
    public void setState(DomainState state);

    @FacadeData(DATA_TYPE_DOMAIN_MESSAGE)
    public String getMessage();

    @FacadeData(DATA_TYPE_PRIVACY_POLICY_HTML)
    public AttachmentFacade getPrivacyPolicy();
    public void setPrivacyPolicy(AttachmentFacade privacyPolicy);

    @FacadeData(DATA_TYPE_TERMS_OF_USE_HTML)
    public AttachmentFacade getTermsOfUse();
    public void setTermsOfUse(AttachmentFacade termsOfUse);

    @FacadeData(DATA_TYPE_GOOGLE_ANALYTICS_ACCOUNT)
    public String getGoogleAnalyticsAccount();
    public void setGoogleAnalyticsAccount(String account);

    @FacadeData(DATA_TYPE_LOGIN_REQUIRED)
    Boolean getLoginRequired();
    void setLoginRequired(Boolean required);

    @FacadeData(DATA_TYPE_LICENSE_REQUIRED)
    Boolean getLicenseRequired();
    void setLicenseRequired(Boolean required);

    @FacadeData(DATA_TYPE_SESSION_TIMEOUT)
    Long getSessionTimeout();
    void setSessionTimeout(Long timeout);

    @FacadeData(DATA_TYPE_REMEMBER_TIMEOUT)
    Long getRememberTimeout();
    void setRememberTimeout(Long timeout);

    @FacadeData(DATA_TYPE_USERS_LIMIT)
    Long getUserLimit();

    @FacadeData(DATA_TYPE_GROUPS_LIMIT)
    Long getGroupLimit();

    @FacadeData(DATA_TYPE_MEMBERSHIP_LIMIT)
    Long getMembershipLimit();

    @FacadeData(DATA_TYPE_ENROLLMENTS_LIMIT)
    Long getEnrollmentLimit();

    @FacadeData(DATA_TYPE_MAXIMUM_FILE_SIZE)
    Long getMaximumFileSize();

    @FacadeData(DATA_TYPE_USER_URL_FORMAT)
    String getUserUrlFormat();

    @FacadeData(DATA_TYPE_GROUP_URL_FORMAT)
    String getGroupUrlFormat();

    @FacadeData(DATA_TYPE_DOMAIN_SUPPORT_EMAIL)
    Optional<String> getSupportEmail();
    void setSupportEmail(Optional<String> supportEmail);
}
