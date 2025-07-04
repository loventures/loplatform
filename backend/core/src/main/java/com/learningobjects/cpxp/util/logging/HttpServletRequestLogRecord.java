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

package com.learningobjects.cpxp.util.logging;

import com.learningobjects.cpxp.util.MimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SystemUtils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HttpServletRequestLogRecord extends LogRecord {
    private static final int MAX_COLLECTION_SIZE = 10;

    private transient HttpServletRequest _request;

    public HttpServletRequestLogRecord(Level level, HttpServletRequest request, String name) {
        super(level, null);
        _request = request;
        setLoggerName(name);
    }

    private String _message;

    public String getMessage() {
        if (_message == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("HttpServletRequest");
            if (_request != null) {
                append(sb, "Method", _request.getMethod());
                append(sb, "Request URI", _request.getRequestURI());
                append(sb, "Protocol", _request.getProtocol());
                append(sb, "Query String", _request.getQueryString());
                append(sb, "Content Length", _request.getContentLength());
                append(sb, "Content Type", _request.getContentType());
                append(sb, "Server Name", _request.getServerName());
                append(sb, "Server Port", _request.getServerPort());
                append(sb, "Requested Session Id", _request.getRequestedSessionId());
                append(sb, "Requested Session Id Valid", _request.isRequestedSessionIdValid());
                append(sb, "Remote Address", _request.getRemoteAddr());
                if (!"POST".equals(_request.getMethod()) || (_request.getContentType() != null && _request.getContentType().startsWith(MimeUtils.MIME_TYPE_APPLICATION_X_WWW_FORM_URLENCODED))) {
                    Enumeration e = _request.getParameterNames();
                    if (e.hasMoreElements()) {
                        sb.append(SystemUtils.LINE_SEPARATOR);
                        sb.append("Request Parameters");
                        do {
                            String name = (String) e.nextElement();
                            for (String value: _request.getParameterValues(name)) {
                                if (name.toLowerCase().contains("password")) {
                                    value = "<?>";
                                }
                                append(sb, name, value);
                            }
                        } while (e.hasMoreElements());
                    }
                }
                sb.append(SystemUtils.LINE_SEPARATOR);
                sb.append("Request Headers");
                Enumeration e1 = _request.getHeaderNames();
                while (e1.hasMoreElements()) {
                    String name = (String) e1.nextElement();
                    Enumeration ev = _request.getHeaders(name);
                    while (ev.hasMoreElements()) {
                        append(sb, name, ev.nextElement());
                    }
                }
                sb.append(SystemUtils.LINE_SEPARATOR);
                sb.append("Request Attributes");
                Enumeration e2 = _request.getAttributeNames();
                while (e2.hasMoreElements()) {
                    String name = (String) e2.nextElement();
                    Object value = _request.getAttribute(name);
                    // This is a horrible hack to stop our JSF message bundles
                    // from being dumped. They have no usable class.
                    if ("msg".equals(name) || "constant".equals(name)) {
                        value = value.getClass().getName();
                    }
                    append(sb, name, value);
                }
                HttpSession session = _request.getSession(false);
                if (session != null) {
                    sb.append(SystemUtils.LINE_SEPARATOR);
                    sb.append("Session Attributes");
                    append(sb, "Id", session.getId());
                    append(sb, "Creation Time", session.getCreationTime());
                    Enumeration e3 = session.getAttributeNames();
                    while (e3.hasMoreElements()) {
                        String name = (String) e3.nextElement();
                        Object value = session.getAttribute(name);
                        append(sb, name, value);
                    }
                }
            }
            _message = sb.toString();
        }
        return _message;
    }

    private void append(StringBuilder sb, String msg, Object value) {
        if (value != null) {
            if (value instanceof Collection) {
                int size = ((Collection) value).size();
                if (size > MAX_COLLECTION_SIZE) {
                    value = ClassUtils.getShortClassName(value.getClass()) + "<" + size + ">";
                }
            }
            sb.append(SystemUtils.LINE_SEPARATOR);
            sb.append(msg);
            sb.append(": ");
            sb.append(value);
        }
    }
}
