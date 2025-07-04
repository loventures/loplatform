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

package loi.cp.system;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.HtmlResponse;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.component.web.WebResponseOps;
import com.learningobjects.cpxp.util.SessionUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.session.SessionRootComponent;

import javax.inject.Inject;

@Component
@ServletBinding(path = LogoutServlet.CONTROL_LOGOUT)
public class LogoutServlet extends AbstractComponentServlet {

    @Inject
    private SessionRootComponent sessionRoot;

    public static final String CONTROL_LOGOUT = "/control/logout";

    /**
     *  This endpoint is deprecated, and now only exists as a convenience for developers/testers.
     *
     *  Do not expose any direct links to the GET variant of this endpoint. As per RFC 2616,
     *  GET requests should not have side-effects, and this particular endpoint can produce
     *  extremely undesirable side-effects if it is cached or prefetched.
     *
     *  Use {@link loi.cp.session.SessionRootComponent#logout /api/v2/sessions/logout} if you
     *  need an RPC.
     */
    @Override @Deprecated
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        sessionRoot.logout(request, response);
        SessionUtils.clearInfoCookie(request, response); // do not want to see a second logged-out page
        String path = StringUtils.substringAfter(request.getRequestURI(), CONTROL_LOGOUT);
        if (StringUtils.isNotEmpty(path)) {
            response.sendRedirect(path);
        } else {
            WebResponseOps.send(HtmlResponse.apply(this, "logout.html"), request, response);
        }
    }
}
