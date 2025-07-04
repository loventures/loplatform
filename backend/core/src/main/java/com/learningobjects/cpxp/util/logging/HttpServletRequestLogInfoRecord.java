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

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class HttpServletRequestLogInfoRecord extends LogRecord {
    private static final int MAX_PARAMETER_SIZE = 256;

    private final transient HttpServletRequest _request;

    public HttpServletRequestLogInfoRecord(Level level, HttpServletRequest request) {
        super(level, null);
        _request = request;
    }

    private String _url;

    public String getRequestLine() {
        if (_url == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(_request.getMethod()).append(' ');
            String protocol = _request.isSecure() ? "https://" : "http://";
            String host = _request.getServerName();
            sb.append(protocol).append(host).append(_request.getRequestURI());
            if (!"POST".equals(_request.getMethod()) || MimeUtils.MIME_TYPE_APPLICATION_X_WWW_FORM_URLENCODED.equals(_request.getContentType())) {
                int i = 0;
                Enumeration e = _request.getParameterNames();
                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    for (String value: _request.getParameterValues(name)) {
                        if (name.toLowerCase().contains("password")) {
                            value = "<?>";
                        }
                        append(sb, name, value, i ++);
                    }
                }
            }
            _url = sb.toString();
        }
        return _url;
    }

    private String _message;

    public String getMessage() {
        if (_message == null) {
            StringBuilder sb = new StringBuilder();
            String remoteAddr = HttpUtils.getRemoteAddr(_request, BaseServiceMeta.getServiceMeta());
            sb.append(remoteAddr).append(": ");
            sb.append(getRequestLine());
            String id = _request.getRequestedSessionId();
            if (id != null) {
                sb.append(" [");
                if (!_request.isRequestedSessionIdValid()) {
                    sb.append("???");
                }
                sb.append(id);
                sb.append(']');
            }
            _message = sb.toString();
        }
        return _message;
    }

    private void append(StringBuilder sb, String name, String value, int index) {
        sb.append((index == 0) ? '?' : '&').append(name).append('=');
        int len = value.length();
        if (len < MAX_PARAMETER_SIZE) {
            sb.append(value);
        } else {
            sb.append('<').append(len).append('>');
        }
    }
}
