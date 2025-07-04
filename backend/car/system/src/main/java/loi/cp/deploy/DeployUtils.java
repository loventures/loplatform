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

package loi.cp.deploy;

import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException;
import com.learningobjects.cpxp.service.integration.IntegrationWebService;
import com.learningobjects.cpxp.service.integration.SystemFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.admin.right.HostingAdminRight;
import loi.cp.apikey.ApiKeySystem;
import loi.cp.right.RightService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;

public class DeployUtils {

    static void sendError(HttpServletResponse response, PrintWriter pw, Throwable ex) throws IOException {
        // Print only the innermost exception stack frames
        String[] sw = ExceptionUtils.getRootCauseStackTrace(ex);

        if (response.isCommitted()) {
            // If we've already written to the response, we can't change the status code.
            // This sucks.
            for (int i = 0; (i < sw.length) && !sw[i].contains("ReportableException"); ++i) {
                pw.println(sw[i]);
            }
        } else {
            String msg = "";
            for (int i = 0; (i < sw.length) && !sw[i].contains("ReportableException"); ++i) {
                msg += sw[i] + "\n";
            }

            int code = ex instanceof AccessControlException ? HttpStatus.SC_FORBIDDEN : HttpStatus.SC_BAD_REQUEST;
            response.sendError(code, msg);
        }
    }

    static void validateApiKey(HttpServletRequest request) throws AccessControlException {
        String keyStr = request.getParameter("apiKey");
        if (keyStr == null) {
            throw new AccessControlException("Missing API Key");
        }
        SystemFacade system = ServiceContext.getContext().getService(IntegrationWebService.class).getByKey(keyStr);
        if (system == null || system.getDisabled() || !ComponentSupport.isSupported(ApiKeySystem.class, system)) {
            throw new AccessControlException("Invalid API Key");
        }
        ApiKeySystem apiKey = ComponentSupport.getInstance(ApiKeySystem.class, system, null);
        if (!apiKey.getRightClasses(ComponentSupport.lookupService(RightService.class)).contains(HostingAdminRight.class)) {
            throw new AccessControlException("Invalid API Key");
        }
    }
}
