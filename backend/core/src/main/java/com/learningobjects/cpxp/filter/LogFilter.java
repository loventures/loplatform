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

package com.learningobjects.cpxp.filter;

import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.Underground;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.logging.HttpServletRequestLogInfoRecord;
import com.learningobjects.cpxp.util.logging.HttpServletRequestLogRecord;
import de.tomcat.juli.LogMeta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.commons.lang3.CharEncoding;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A filter that sets up logging.
 */
public class LogFilter extends AbstractFilter {
    private static final Logger logger = Logger.getLogger(LogFilter.class.getName());

    public static final String REQUEST_ATTRIBUTE_EXCEPTION = "ug:exception";

    @Inject
    private ServiceMeta sm;

    public LogFilter() {
        super();
    }

    /**
     * Restore logging.
     */
    @Override
    public void destroy() {
        Logger logger = Underground.getBaseLogger();
        logger.setLevel(null);
    }

    /**
     * Queue log records relating to a particular request.
     */
    @Override
    protected void filterImpl(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        LogMeta.session(request.getRequestedSessionId());
        LogMeta.request(GuidUtil.mediumGuid());
        LogMeta.ug(HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION));
        // I have to set the char encoding or else it gets fixed @ LATIN 1 when
        // I log the request parameters. IE and FF send using the same encoding
        // as the page was rendered in so hopefully this is always safe.. May
        // need to extend it to check whethr a char encoding was actually specified..
        request.setCharacterEncoding(CharEncoding.UTF_8);
        if ("POST".equals(request.getMethod()) && MimeUtils.MIME_TYPE_APPLICATION_X_WWW_FORM_URLENCODED.equals(request.getContentType())) {
            request.getParameterNames(); // preload the request body so logging does not stall...
        }
        String xDomain = request.getHeader(CurrentFilter.HTTP_HEADER_X_DOMAIN_ID);
        String uri = (request.isSecure() ? "https://" : "http://") + request.getServerName() + request.getRequestURI()
          + (xDomain != null ? " (" + xDomain + ")" : "");
        ThreadLog.begin(uri);
        try {
            long then = System.currentTimeMillis();
            HttpSession session = request.getSession(false);
            boolean isStatus = AccessFilter.isStatusCheck(request);
            if (session != null) {
                Optional.ofNullable((Long) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_DOMAIN_ID)).ifPresent(LogMeta::domain);
                Optional.ofNullable((Long) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_USER_ID)).ifPresent(LogMeta::user);
                Optional.ofNullable((String) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_USER_NAME)).ifPresent(LogMeta::username);
                Optional.ofNullable((String) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER)).ifPresent(sudoer ->
                  LogMeta.put("sudoer", Long.parseLong(sudoer.substring(1 + sudoer.lastIndexOf(':')))));
            }
            LogMeta.put("ip", HttpUtils.getRemoteAddr(request, sm));
            HttpServletRequestLogInfoRecord lir = new HttpServletRequestLogInfoRecord(isStatus ? Level.FINE : Level.INFO, request);
            lir.setLoggerName(logger.getName());
            logger.log(lir);
            logger.log(new HttpServletRequestLogRecord(Level.FINE, request, logger.getName()));
            chain.doFilter(request, response);
            long delta = System.currentTimeMillis() - then;
            ResponseFacade facade = TomcatUtils.getResponseFacade(response);
            int statusCode = facade.getStatus();
            final String bytes = facade.getContentWritten() + " bytes";
            Throwable th = (Throwable) request.getAttribute(REQUEST_ATTRIBUTE_EXCEPTION);
            if (th == null) {
                String status = request.isAsyncStarted() ? "asynchronous" : ("status code " + statusCode);
                Level level = isStatus ? Level.FINE : Level.INFO;
                logger.log(level, "Request " + uri + " processed in " + delta + " ms, " + bytes + ", " + status);
            } else {
                logger.log(Level.INFO, "Request " + uri + " processed in " + delta + " ms, " + bytes + ", status code " + statusCode, th);
            }
        } finally {
            LogMeta.clear();
            ThreadLog.end();
        }
    }
}
