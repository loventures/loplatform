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

import java.util.Optional;
import java.util.Properties;
import javax.ejb.Local;

/**
 * Session service.
 */
@Local
public interface SessionService {
    /**
     * Look up a session.
     *
     * @param id the session id
     * @oaram ipAddress the client IP address
     *
     * @return the session, or null
     */
    public SessionFacade lookupSession(String id, String ipAddress);

    public SessionFacade lookupSession(String id, String ipAddress, boolean extendSession);

    /* Number of ms left on the session, or < 0. */
    public long getSessionValidity(String id);

    /**
     * Open a session.
     *
     * @return the persistent session, or null if the session limit is reached
     */
    public SessionFacade openSession(Long user, boolean persistent, String ipAddress);

    /**
     * Invalidate a user's sessions.
     * @param user the user
     * @param exclude an optional session to exclude from invalidation
     */
    public void invalidateUserSessions(Long user, Optional<Long> exclude);

    default void invalidateUserSessions(Long user) {
        invalidateUserSessions(user, Optional.empty());
    }

    public void recordAccess(SessionFacade session, String ipAddress);

    public void closeSession(String id);

    public void pingSession(String id, String ipAddress);

    public long getActiveSessionCount(Long domainId);

    public long getActiveSessionCount();

    public void flushSessionAccesses();

    public void purgeExpiredSessions();

    public void closeDomainSessions(Long domainId);

    public void setProperties(String id, Properties properties);

    public Properties getProperties(String id);
}
