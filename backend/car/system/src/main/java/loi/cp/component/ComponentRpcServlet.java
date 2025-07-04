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

package loi.cp.component;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.RpcInstance;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

@Component(
    name = "$$name=Component Rpc Servlet",
    description = "$$description=This servlet services component rpcs.",
    version = "0.7"
)
@ServletBinding(
    path = "/control/component",
    transact = false
)
public class ComponentRpcServlet extends AbstractComponent implements ServletComponent {
    private static final Logger logger = Logger.getLogger(ComponentRpcServlet.class.getName());
    @Infer
    private DelegateDescriptor _delegate;

    @Override
    public WebResponse service(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ServletBinding binding = _delegate.getBinding(ServletComponent.class);
        String pathInfo = StringUtils.removeStart(request.getRequestURI(), binding.path() + "/");
        int i0 = pathInfo.indexOf('/'), i1 = pathInfo.indexOf('/', i0 + 1);
        if (i0 < 0) {
            return ErrorResponse.notFound();
        }
        if (i1 < 0) {
            i1 = pathInfo.length();
        }
        String cmp = pathInfo.substring(0, i0);
        String fn = pathInfo.substring(i0 + 1, i1);
        request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pathInfo.substring(i1));
        RpcInstance rpc;
        try {
            rpc = ComponentSupport.getRpc(cmp, request.getMethod(), fn);
        } catch (Exception ex) {
            return ErrorResponse.notFound();
        }
        return AbstractRpcServlet.perform(rpc, request, response, logger);
    }
}
