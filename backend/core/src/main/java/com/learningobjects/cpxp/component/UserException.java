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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.service.ServiceException;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class UserException extends ServiceException {
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_CONFIRM = "confirm";
    public static final String STATUS_CHALLENGE = "challenge";

    private final String _status;
    private final String _title;

    public UserException(String message) {
        this(STATUS_ERROR, null, message);
    }

    public UserException(String title, String message) {
        this(STATUS_ERROR, title, message);
    }

    public UserException(String status, String title, String message) {
        super(message);
        _status = status;
        _title = title;
    }

    public String getStatus() {
        return _status;
    }

    @Override
    public String getTitle() {
        return _title;
    }
}
