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

package com.learningobjects.cpxp.service;

import javax.ejb.ApplicationException;

/**
 * A service exception for presentation to the end user.
 */
@ApplicationException(rollback=true)
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(String message, String msg, Object... params) {
        super(message);
        setMsg(msg, params);
    }

    public String getTitle() {
        return null;
    }

    private String _msg;
    private Object[] _params;

    public void setMsg(String msg, Object[] params) {
        _msg = msg;
        _params = params;
    }

    public String getMsg() {
        if(_msg == null) {
            return this.getMessage();
        } else {
            return _msg;
        }
    }

    public Object[] getParams() {
        return (_params == null) ? new Object[0] : _params;
    }

    public Object getParam1() {
        return getParam(0);
    }

    public Object getParam2() {
        return getParam(1);
    }

    public Object getParam3() {
        return getParam(2);
    }

    private Object getParam(int index) {
        return ((_params != null) && (_params.length > index)) ? _params[index] : null;
    }

    private boolean _unloggable;

    public boolean isUnloggable() {
        return _unloggable;
    }

    public ServiceException asUnloggable() {
        _unloggable = true;
        return this;
    }
}
