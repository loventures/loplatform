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

package loi.cp.overlord;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.UserException;
import com.learningobjects.cpxp.component.acl.AccessEnforcer;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.accesscontrol.WwwAuthException;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.overlord.OverlordWebService;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import loi.cp.session.SessionRootComponent;
import org.apache.commons.codec.binary.Base64;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Note that the www-auth variant does not yet log you in, just permits stuff.
 */
public class OverlordAuthEnforcer implements AccessEnforcer {
    private static final Logger logger = Logger.getLogger(OverlordAuthEnforcer.class.getName());

    private static final String ATTR_OVERLORD_AUTH = "auth:overlord";

    @Override
    public boolean checkAccess() throws Exception {
        // If I'm overlorded in then allow through
        if ((Current.getUserDTO() != null) && UserType.Overlord.equals(Current.getUserDTO().getUserType())) {
            return true;
        }
        HttpServletRequest request = BaseWebContext.getContext().getRequest();
        if (!request.isSecure()) {
            throw new UserException("Not on an insecure domain.");
        }
        // Already auth'ed
        HttpSession session = request.getSession();
        if (Boolean.TRUE.equals(session.getAttribute(ATTR_OVERLORD_AUTH))) {
            return true;
        }

        Long domainId = Current.getDomain();
        try {
            Long overlordId = ServiceContext.getContext().getService(OverlordWebService.class).findOverlordDomainId();
            ServiceContext.getContext().getService(DomainWebService.class).setupContext(overlordId);
            String authB64 = StringUtils.substringAfter(request.getHeader(HttpUtils.HTTP_HEADER_AUTHORIZATION), " ");
            if (authB64 != null) {
                String header = new String(Base64.decodeBase64(authB64), "UTF-8");
                String username = StringUtils.substringBefore(header, ":");
                String password = StringUtils.substringAfter(header, ":");
                logger.log(Level.INFO, "Overlord auth attempt: {0}", username);
                if (ComponentSupport.get(SessionRootComponent.class).authenticate(username, password, null).isRight()) {
                    logger.log(Level.INFO, "Overlord basic auth succeded: {0}", username);
                    session.setAttribute("auth:overlord", true);
                    session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_USER_NAME, "*" + username + "*");
                    return true;
                }
            }

            String header = HttpUtils.getBasicAuthHeader(Current.getDomainDTO().getName());
            throw (new WwwAuthException("Overlord auth required", header)).asUnloggable();
        } finally {
            if (domainId != null) {
                ServiceContext.getContext().getService(DomainWebService.class).setupContext(domainId);
            }
        }
    }
}
