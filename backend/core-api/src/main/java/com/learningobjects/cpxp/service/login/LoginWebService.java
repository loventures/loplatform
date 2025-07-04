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

package com.learningobjects.cpxp.service.login;

import java.util.Date;

import javax.ejb.Local;

/**
 * Login web service.
 */
@Local
public interface LoginWebService {
    public static final String EXTERNAL_PREFIX = "Ext:";

    public Long authenticate(String userName, String password);

    public void recordLogin(Long userId);

    public void setPassword(Long userId, String password);

    public boolean hasPassword(Long userId);

    public Login authenticateExternal(String username, String password);

    public Date getExternalAuthTime(Long userId);

    public void setExternalAuthTime(Long userId, Date when);

    public void setExternalPassword(Long user, String type, Long system, String password);

    public static class Login {
        public Long userId;
        public boolean pass;
        public boolean external;
        public LoginStatus status;
    }

    public static enum LoginStatus {
        OK,
        Pending,
        Unconfirmed,
        Locked,
        Suspended,
        InvalidCredentials,
        ServerError;
    }
}
