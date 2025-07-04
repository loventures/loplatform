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

import javax.ejb.ApplicationException;

/**
 * A login error.
 */
@ApplicationException(rollback=false)
public class LoginException extends RuntimeException {
    private final Long _user;
    private final LoginWebService.LoginStatus _failure;

    public LoginException(final String message, final LoginWebService.LoginStatus failure, final Long user) {
        super(message);
        _failure = failure;
        _user = user;
    }

    public Long getUser() {
        return _user;
    }

    public LoginWebService.LoginStatus getFailure() {
        return _failure;
    }
}
