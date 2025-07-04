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

import java.util.Date;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.dto.FacadeParent;
import com.learningobjects.cpxp.service.user.UserFacade;

/**
 * A facade for sessions.
 */
@FacadeItem(SessionConstants.ITEM_TYPE_SESSION)
public interface SessionFacade extends Facade {
    @FacadeData(SessionConstants.DATA_TYPE_SESSION_ID)
    public String getSessionId();
    public void setSessionId(String sessionId);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_USER)
    public UserFacade getUser();
    public void setUser(Long userId);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_REMEMBER)
    public Boolean getRemember();
    public void setRemember(Boolean remember);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_STATE)
    public SessionState getState();
    public void setState(SessionState state);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_CREATED)
    public Date getCreated();
    public void setCreated(Date created);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_LAST_ACCESS)
    public Date getLastAccess();
    public void setLastAccess(Date lastAccess);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_EXPIRES)
    public Date getExpires();
    public void setExpires(Date expires);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_IP_ADDRESS)
    public String getIpAddress();
    public void setIpAddress(String ipAddress);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_NODE_NAME)
    public String getNodeName();
    public void setNodeName(String nodeName);

    @FacadeData(SessionConstants.DATA_TYPE_SESSION_PROPERTIES)
    public String getProperties();
    public void setProperties(String properties);

    @FacadeParent
    public SessionDomainFacade getParent();
}
