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

package com.learningobjects.cpxp.controller;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.ManagedObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Date;
import java.util.TimeZone;

/**
 * Superclass for controller implementations. Provides a logger and various utility methods.
 */
public abstract class AbstractController extends ManagedObject {
    /**
     * Gets the current user.
     *
     * @return the current user
     */
    public Long getCurrentUser() {
        return Current.getUser();
    }

    /**
     * Gets anonymity.
     *
     * @return whether the current user is anonymous
     */
    public boolean isAnonymous() {
        return UserType.Anonymous == Current.getUserDTO().getUserType();
    }

    /** The current time. */
    protected Date getCurrentTime() {
        Date time = Current.getTime();
        if (time == null) {
            throw new IllegalStateException("Current time unset");
        }
        return time;
    }

    public String getTimeZone() {
        // TODO check DST on Current.time
        return InternationalizationUtils.getTimeZone().getDisplayName(true, TimeZone.SHORT, getRequest().getLocale());
    }

    /* JSF */

    protected static HttpSession getSession() {
        return getRequest().getSession(false);
    }

    protected static Object getSessionAttribute(String key) {
        HttpSession session = getSession();
        return (session == null) ? null : session.getAttribute(key);
    }

    protected Object removeSessionAttribute(String key) {
        HttpSession session = getSession();
        Object value = (session == null) ? null : session.getAttribute(key);
        if (value != null) {
            session.removeAttribute(key);
        }
        return value;
    }

    protected static HttpServletRequest getRequest() {
        return BaseWebContext.getContext().getRequest();
    }

    protected HttpServletResponse getResponse() {
        return BaseWebContext.getContext().getResponse();
    }

}
