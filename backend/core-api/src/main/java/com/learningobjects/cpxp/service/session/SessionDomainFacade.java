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

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import scala.Option;

import java.util.List;

/**
 * A facade for domain sessions.
 */
@FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
public interface SessionDomainFacade extends Facade {
    @FacadeChild(SessionConstants.ITEM_TYPE_SESSION)
    public List<SessionFacade> getSessions();
    public void removeSession(SessionFacade session);
    public SessionFacade addSession();

    public List<SessionFacade> findSessionsByUser(
      @FacadeCondition(SessionConstants.DATA_TYPE_SESSION_USER) Long userId
    );

    Option<SessionFacade> findSessionBySessionId(@FacadeCondition(SessionConstants.DATA_TYPE_SESSION_ID) String sessionId);

    @FacadeData(DomainConstants.DATA_TYPE_SESSION_LIMIT)
    public Long getSessionLimit(Long defaultLimit);

    @FacadeData(DomainConstants.DATA_TYPE_SESSION_TIMEOUT)
    public Long getSessionTimeout(Long defaultTimeout);

    @FacadeData(DomainConstants.DATA_TYPE_REMEMBER_TIMEOUT)
    public Long getRememberTimeout(Long defaultTimeout);


    @FacadeChild(SessionConstants.ITEM_TYPE_SESSION_STATISTICS)
    SessionStatisticsFacade getOrCreateSessionStatisticsByDate(
      @FacadeCondition(SessionConstants.DATA_TYPE_SESSION_STATISTICS_DATE) String date
    );
}
