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

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.controller.login.LoginController;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

@Component
@ServletBinding(path = LogbackServlet.CONTROL_EXIT)
public class LogbackServlet extends AbstractComponentServlet {
    public static final String CONTROL_EXIT = "/control/exit";

    /**
     *  This endpoint is only provided as a convenience for developers/testers.
     *
     *  Do not expose any direct links to the GET variant of this endpoint.  As per RFC 2616,
     *  GET requests should not have side-effects, and this particular endpoint can produce
     *  extremely undesirable side-effects if it is cached or prefetched.
     *
     *  Use {@link loi.cp.session.SessionRootComponent#exit}  /api/v2/sessions/exit}
     *  if you need an RPC.
     */
    @Override @Deprecated
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpUtils.sendAutopost(response, HttpUtils.getUrl(request, CONTROL_EXIT));
    }

    /**
     *  Deprecated.  Use {@link loi.cp.session.SessionRootComponent#exit}  /api/v2/sessions/exit}
     *  if you need an RPC.
     */
    @Override @Deprecated
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // make sure we're actually sudo'd...
        String returnUrl = null;
        if(request.getSession().getAttribute(SessionUtils.SESSION_ATTRIBUTE_SUDOER) != null){
            returnUrl = ComponentSupport.newInstance(LoginController.class).logBack();
        }
        response.sendRedirect(Optional.ofNullable(returnUrl).orElse("/"));
    }
}
