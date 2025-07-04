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

package com.learningobjects.cpxp.servlet;

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.filter.LogFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceException;
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException;
import com.learningobjects.cpxp.service.accesscontrol.WwwAuthException;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.GuidUtil;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import com.learningobjects.cpxp.util.logging.HttpServletRequestLogRecord;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.apm.Apm;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass for servlet implementations.
 */
public abstract class AbstractServlet extends HttpServlet {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The logger. */
    private Logger logger = Logger.getLogger(getClass().getName());

    protected AbstractServlet() {
        ManagedUtils.di(this, false);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) {
        try {
            super.service(request, response);
        } catch (Throwable th) {
            try {
                doException(th, (HttpServletRequest) request, (HttpServletResponse) response, logger);
            } catch (Throwable th2) {
                logger.log(Level.WARNING, "Suppressed error", th);
                logger.log(Level.WARNING, "Error handling error", th2);
            }
        }
    }

    public static void doException(Throwable th, HttpServletRequest request, HttpServletResponse response, Logger logger) throws Exception {
        Apm.noticeError(th);
        try {
            ManagedUtils.rollback(); // start a new tx for the error page
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Rollback error", ex);
        }
        int index = ExceptionUtils.indexOfType(th, ServiceException.class);
        if (ExceptionUtils.indexOfType(th, FileNotFoundException.class) >= 0) {
            logger.fine("Not found");
            ComponentSupport.getFn("errors", "notFound").invoke();
        } else if (index >= 0) {
            ServiceException se = (ServiceException) ExceptionUtils.getThrowables(th)[index];
            if (se instanceof WwwAuthException) {
                response.addHeader(HttpUtils.HTTP_HEADER_WWW_AUTH, ((WwwAuthException) se).getAuthHeader());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else if (se instanceof AccessControlException) {
                if ((Current.getUserDTO() == null) || (UserType.Anonymous == Current.getUserDTO().getUserType())) {
                    logger.fine("Login required");
                    String expired = SessionUtils.hasLoginExpired(request, response);
                    ComponentSupport.getFn("errors", "loginRequired").invoke(expired);
                } else {
                    logger.fine("Access denied");
                    ComponentSupport.getFn("errors", "accessDenied").invoke();
                }
            } else {
                ComponentSupport.getFn("errors", "badRequest").invoke(se);
            }
            if (!se.isUnloggable()) {
                request.setAttribute(LogFilter.REQUEST_ATTRIBUTE_EXCEPTION, th);
            }
        } else if (ExceptionUtils.indexOfType(th, ClientAbortException.class) >= 0) {
            logger.fine("Client abort");
            // probably ineffective as the response is likely committed by now, this
            // is intended to suppress error logging for browser disconnects
            response.setStatus(0);
        } else {
            logger.log(new HttpServletRequestLogRecord(Level.WARNING, request, logger.getName()));
            String guid = GuidUtil.errorGuid();
            ComponentSupport.getFn("errors", "internalError").invoke(th, guid);
            request.setAttribute(LogFilter.REQUEST_ATTRIBUTE_EXCEPTION, new Exception("Web application error: " + guid, th));
        }
    }
}
