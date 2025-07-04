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

import com.learningobjects.cpxp.util.GuidUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet(name = "JavascriptException",loadOnStartup = 1, urlPatterns = "/control/javascriptException")
public class JavascriptExceptionServlet extends AbstractServlet {
    private static final Logger logger = Logger.getLogger(JavascriptExceptionServlet.class.getName());

    private static final String PARAMETER_EXCEPTION = "exception";

    private static final String PARAMETER_USER_AGENT = "userAgent";

    private static final String PARAMETER_TRACE = "trace";

    private static final String PARAMETER_URL = "url";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO: protect against malicious posters?
        String errorGuid = GuidUtil.errorGuid();

        String exception = request.getParameter(PARAMETER_EXCEPTION);
        String userAgent = request.getParameter(PARAMETER_USER_AGENT);
        String trace = request.getParameter(PARAMETER_TRACE);
        String url = request.getParameter(PARAMETER_URL);

        StringBuffer sb = new StringBuffer();
        sb.append("Javascript error: ").append(errorGuid);
        sb.append("\n").append(exception);
        sb.append("\nURL: ").append(url);
        sb.append("\nUser agent: ").append(userAgent);
        sb.append("\nTrace:\n").append(trace);
        logger.warning(sb.toString());

        response.getWriter().append(errorGuid);
    }
}
