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

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.service.session.SessionService;
import com.learningobjects.cpxp.service.session.SessionSupport;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;
import java.io.IOException;

public class SessionFilter extends AbstractFilter {

    @Inject
    private SessionService _sessionService;

    public SessionFilter() {
        super();
    }

    @Override
    protected void filterImpl(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String sessionId = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION);
        if (SessionSupport.isPersistentId(sessionId)
            && !"true".equals(request.getHeader("X-No-Session-Extension"))) {
            _sessionService.pingSession(sessionId, HttpUtils.getRemoteAddr(request, BaseServiceMeta.getServiceMeta()));
        }
        chain.doFilter(request, response);
    }

}
