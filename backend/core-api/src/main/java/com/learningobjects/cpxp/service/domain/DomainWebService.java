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

import java.io.File;
import javax.ejb.Local;

import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.portal.NameFacade;
import com.learningobjects.cpxp.service.session.SessionState;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.util.StringUtils;

/**
 * Domain web service.
 */
@Local
public interface DomainWebService {
    public CurrentStatus checkCurrentStatus(CurrentInfo info, boolean extendSession);

    public static class CurrentInfo {
        private static final long serialVersionUID = -2390094654655396825L;

        private String _sessionId;

        public void setSessionId(String sessionId) {
            _sessionId = sessionId;
        }

        public String getSessionId() {
            return _sessionId;
        }

        private String _ipAddress;

        public void setIpAddress(String ipAddress) {
            _ipAddress = ipAddress;
        }

        public String getIpAddress() {
            return _ipAddress;
        }

        private String _hostName;

        public void setHostName(String hostName) {
            _hostName = hostName;
        }

        public String getHostName() {
            return _hostName;
        }

        private String _domainId;

        public void setDomainId(String domainId) {
            _domainId = domainId;
        }

        public String getDomainId() {
            return _domainId;
        }
    }

    public static class CurrentStatus {
        private static final long serialVersionUID = 4514178616915760725L;
        private boolean _domainUpgrading;
        private DomainDTO _domain;
        private UserDTO _user;
        private Long _sessionPk;
        private SessionState _sessionState;
        private boolean _rememberSession;
        private boolean _licenseRequired;
        private SecurityLevel _securityLevel;
        private Long _anonymous;

        public boolean isDomainUpgrading() {
            return _domainUpgrading;
        }

        public void setDomain(DomainDTO domain) {
            _domain = domain;
        }

        public DomainDTO getDomain() {
            return _domain;
        }

        public void setUser(UserDTO user) {
            _user = user;
        }

        public UserDTO getUser() {
            return _user;
        }

        public void setSessionState(SessionState sessionState) {
            _sessionState = sessionState;
        }

        public SessionState getSessionState() {
            return _sessionState;
        }

        public void setSessionPk(Long sessionPk) {
            _sessionPk = sessionPk;
        }

        public Long getSessionPk() {
            return _sessionPk;
        }

        public void setRememberSession(boolean rememberSession) {
            _rememberSession = rememberSession;
        }

        public boolean getRememberSession() {
            return _rememberSession;
        }

        public void setLicenseRequired(boolean licenseRequired) {
            _licenseRequired = licenseRequired;
        }

        public boolean getLicenseRequired() {
            return _licenseRequired;
        }

        public void setSecurityLevel(SecurityLevel securityLevel) {
            _securityLevel = securityLevel;
        }

        public SecurityLevel getSecurityLevel() {
            return _securityLevel;
        }

        public void setAnonymousUser(Long anonymous) {
            _anonymous = anonymous;
        }

        public Long getAnonymousUser() {
            return _anonymous;
        }
    }

    public PathStatus checkPathStatus(String path);

    public static class PathStatus {
        private static final long serialVersionUID = 2350566873349109904L;
        private boolean _notFound;
        private Item _item;

        public void setNotFound(boolean notFound) {
            _notFound = notFound;
        }

        public boolean isNotFound() {
            return _notFound;
        }

        public void setItem(Item item) {
            _item = item;
        }

        public Item getItem() {
            return _item;
        }
    }

    public DomainFacade addDomain();

    public DomainDTO getDomainDTO(Long id);

    public DomainFacade getDomain(Long id);

    public void removeDomain(Long id);

    public Long getDomainById(String domainId);

    public Long getItemRoot(Long id);

    public Long getDomainIdByHost(String host);

    public Item getDomainByExactHost(String host);

    public void setDomainType(Long domain, String type);

    public Long findDomainByType(String type);

    public String getCurrentDomainPath();

    public void setCssZip(String fileName, File zipFile);

    public void setImage(String type, String fileName, Long width, Long height, File logoFile);

    public void logStatistics();

    public String getAdministrationLink();

    public void setState(Long domainId, DomainState state, String message);

    public ComponentEnvironment setupContext(Long domainId);

    public ComponentEnvironment setupUserContext(Long userId);

    void invalidateHostnameCache();
}
