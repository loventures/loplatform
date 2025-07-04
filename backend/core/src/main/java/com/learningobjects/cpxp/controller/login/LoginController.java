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

package com.learningobjects.cpxp.controller.login;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.controller.AbstractController;
import com.learningobjects.cpxp.filter.CurrentFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.domain.SecurityLevel;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserFacade;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ObjectUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.codec.binary.Hex;

import javax.inject.Inject;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic login controller.
 *
 * @see Current
 * @see CurrentFilter
 */
// TODO: I control nothing anymore and should be a utils or service
public class LoginController extends AbstractController {
    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    /* Services */

    @Inject
    private UserWebService _userWebService;

    @Inject
    private DomainWebService _domainWebService;

    /* Metadata */

    public Long getId() {
        return getCurrentUser();
    }

    public boolean getAnonymous() {
        return UserType.Anonymous == Current.getUserDTO().getUserType();
    }

    private UserFacade _user;

    public UserFacade getUser() {
        // Id will only be null when rendering some error pages
        if ((_user == null) && (getId() != null)) {
            _user = _userWebService.getUser(getId());
        }
        return _user;
    }

    public Date getNow() {
        return getCurrentTime();
    }

    public String getUserAgent() {
        return getRequest().getHeader(HttpUtils.HTTP_HEADER_USER_AGENT);
    }

    public String getHostName() {
        final ServiceMeta serviceMeta = BaseServiceMeta.getServiceMeta();
        return serviceMeta.getCluster() + "-" + serviceMeta.getNode();
    }

    private String _challenge;

    public String getChallenge() {
        if (_challenge == null) {
            if (Current.getSessionId() == null) {
                _challenge = String.valueOf(System.currentTimeMillis());
            } else {
                _challenge = Hex.encodeHexString(Current.getSessionId()
                        .getBytes());
            }
        }
        return _challenge;
    }

    private Long _timestamp;

    public Long getTimestamp() {
        if (_timestamp == null) {
            _timestamp = System.currentTimeMillis();
        }
        return _timestamp;
    }

    public JsonMap logout() {
        logger.log(Level.FINE, "Logging out");

        String mechanism = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_LOGIN_MECHANISM);
        String externalSystemCallback = null;

        String embedType = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_SSO_EMBED);
        String returnLink = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_RETURN_URL);

        CurrentFilter.logout(getRequest(), getResponse());

        JsonMap response = new JsonMap();

        if (StringUtils.equalsIgnoreCase(embedType,
                CurrentFilter.EMBED_TYPE_NEW)) {
            response.put("close", true);
        }

        if (StringUtils.isNotEmpty(externalSystemCallback)) {
            response.put("redirect", externalSystemCallback);
        } else if (StringUtils.isNotEmpty(returnLink)) {
            response.put("redirect", StringUtils.substringBefore(returnLink, "$"));
        } else {
            boolean loginRequired = Boolean.TRUE.equals(Current.getDomainDTO()
                    .getLoginRequired());
            if (loginRequired || !StringUtils.isEmpty(embedType)) {
                SessionUtils.setInfoCookie(getResponse(),
                        SessionUtils.COOKIE_INFO_LOGGED_OUT, false);
            }
            String redirect = (getDomainSecurityLevel() == SecurityLevel.SecureAlways) ? "/"
                    : HttpUtils.getHttpUrl(getRequest(), "/");
            response.put("redirect", redirect);
        }

        return response;
    }

    /**
     * su
     * @param userId
     * @return JSON Object with "url" property containing a suggested redirect URL.
     */
    public JsonMap logInAs(Long userId, String returnUrl) {
        UserDTO user = _userWebService.getUserDTO(userId);
        Properties properties = new Properties();
        String sudoer = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER);
        sudoer = StringUtils.isNotEmpty(sudoer) ? sudoer + ":" : "";
        properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_SUDOER, sudoer + getCurrentUser());
        String ol = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_OVERLORD);
        properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_OVERLORD, StringUtils.defaultString(ol));
        if (returnUrl != null)
          properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_LOGOUT_RETURN_URL, returnUrl);

        CurrentFilter.login(getRequest(), getResponse(), user, false, properties);
        JsonMap response = new JsonMap();
        response.put("url", user.getUrl());

        return response;
    }

    /**
     * Reverse su
     * Throws {@link RuntimeException} if current session is not su'd
     *
     * @return Redirect URL.  "/" if switching domains.  Null otherwise.  Many clients interpret a null response
     *         as an indication to refresh the current page.
     * @throws RuntimeException
     */
    public String logBack() {
        Properties properties = new Properties();
        String oldUser = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER);
        if (StringUtils.isEmpty(oldUser)) {
            throw new RuntimeException("No previous user");
        }
        final int index = oldUser.lastIndexOf(':');
        Long user = Long.parseLong(oldUser.substring(1 + index));
        if (index >= 0) {
            properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_SUDOER, oldUser.substring(0, index));
        } else {
            removeSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER);
        }
        String ol = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_OVERLORD);
        String returnUrl = (String) getSessionAttribute(SessionUtils.SESSION_ATTRIBUTE_LOGOUT_RETURN_URL);
        properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_OVERLORD, StringUtils.defaultString(ol));
        Long oldDomain = Current.getDomain();
        Long newDomain = _domainWebService.getItemRoot(user);
        Current.setDomainDTO(_domainWebService.getDomainDTO(newDomain));
        CurrentFilter.login(getRequest(), getResponse(), _userWebService.getUserDTO(user), false, properties);
        if (StringUtils.isNotEmpty(returnUrl)) {
            return returnUrl;
        } else if (!oldDomain.equals(newDomain)) {
            return "/Domains";
        } else if (Current.isRoot()) { // the right test is whether i'm an admin but that's hard
            return "/Administration";
        } else {
            return null;
        }
    }


    /* Internal */

    private SecurityLevel getDomainSecurityLevel() {
        return ObjectUtils.getFirstNonNullIn(Current.getDomainDTO()
                .getSecurityLevel(), SecurityLevel.NoSecurity);
    }

}
