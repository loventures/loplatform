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

package com.learningobjects.cpxp.util;

import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.session.SessionConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionUtils {
    private static final Logger logger = Logger.getLogger(SessionUtils.class.getName());

    /** The user name session attribute name. Only used for logging purposes. */
    public static final String SESSION_ATTRIBUTE_DOMAIN_ID = "ug:domainId";

    /** The user name session attribute name. Only used for logging purposes. */
    public static final String SESSION_ATTRIBUTE_USER_ID = "ug:userId";

    /** The user name session attribute name. Only used for logging purposes. */
    public static final String SESSION_ATTRIBUTE_USER_NAME = "ug:userName";

    /** The domain message request attribute name. */
    public static final String REQUEST_ATTRIBUTE_DOMAIN_MESSAGE = "ug:domainMessage";

    /** Flag indicating that a request was bearer authorized. */
    public static final String REQUEST_PARAMETER_BEARER_AUTHORIZED = "ug:bearerAuthorized";

    /** The secure session attribute name. */
    public static final String SESSION_ATTRIBUTE_SECURE = "ug:secure";

    /** The sudoer attribute name. */
    public static final String SESSION_ATTRIBUTE_SUDOER = "ug:sudoer";

    /** The session Id attribute name. */
    public static final String SESSION_ATTRIBUTE_SESSION_ID = "ug:sessionId";

    /** The csrf attribute name. */
    public static final String SESSION_ATTRIBUTE_CSRF = "ug:csrf";

    /** When the last remembered cookies were sent. */
    public static final String SESSION_ATTRIBUTE_REMEMBER_TIME = "ug:rememberTime";

    /** The login required attribute name. */
    public static final String REQUEST_ATTRIBUTE_LOGIN_REQUIRED = "ug:loginRequired";

    /** The login expired attribute name. */
    public static final String REQUEST_ATTRIBUTE_LOGIN_EXPIRED = "ug:loginExpired";

    /** The return URL session attribute name. */
    public static final String SESSION_ATTRIBUTE_RETURN_URL = "ug:returnUrl";

    /**
     * A return URL that is only invoked when there is a logout.
     *  Contrast with SESSION_ATTRIBUTE_RETURN_URL, which can be invoked in other contexts. (E.g., activity completion during LTI launch.)
     */
    public static final String SESSION_ATTRIBUTE_LOGOUT_RETURN_URL = "ug:logoutReturnUrl";

    /** The SSO embed session attribute name. */
    public static final String SESSION_ATTRIBUTE_SSO_EMBED = "ug:ssoEmbed";

    /** The login mechanism attribute name. */
    public static final String SESSION_ATTRIBUTE_LOGIN_MECHANISM = "ug:loginMechanism";

    /** The overlord session attribute name. */
    public static final String SESSION_ATTRIBUTE_OVERLORD = "ug:overlord";

    /** The form cookie random number attribute. */
    public static final String SESSION_ATTRIBUTE_FORM_COOKIE = "ug:formCookie";

    /** The items and roles with which the user is currently previewing */
    public static final String SESSION_ATTRIBUTE_PREVIEW = "ug:preview";

    public static final String SESSION_ATTRIBUTE_CUSTOM_TITLE = "ug:customTitle";

    /** Session token. */
    public static final String COOKIE_NAME_SESSION = "UG";

    /** Information. */
    public static final String COOKIE_NAME_INFO = "UGINFO";

    /** Cross site request forgery. */
    public static final String COOKIE_NAME_CSRF = "CSRF";

    /** The item request attribute name. */
    public static final String REQUEST_ATTRIBUTE_ITEM = "ug:item";

    public static final String COOKIE_INFO_LOGGED_IN = "loggedIn";
    public static final String COOKIE_INFO_LOGGED_OUT = "loggedOut";
    public static final String COOKIE_INFO_EXPIRED = "expired";

    // This has the side-effect of removing any such cookie.
    // Note that page laout tests and clears this flag but displays nothign
    // for access to / because that is annoying for when you return to the
    // domain after your session expired and want to log in again.
    public static String hasLoginExpired(HttpServletRequest request, HttpServletResponse response) {
        String expired = (String) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_EXPIRED);
        if (expired != null) {
            logger.log(Level.FINE, "Has login expired, {0}", expired);
            // I can't use clearInfoCookie because I may have just
            // added the cookie in this request...
            setInfoCookie(response, null, false);
        }
        return expired;
    }

    public static void setSessionCookie(HttpServletResponse response, boolean secure, String id, boolean remember) {
        logger.log(Level.INFO, "Set session cookie, {0}, {1}, {2}", new Object[]{secure, id, remember});
        Current.setSessionId(id);
        Cookie sessionCookie = new Cookie(COOKIE_NAME_SESSION, id);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(remember ? (int) (getRememberTimeout() / 1000L) : -1);
        sessionCookie.setPath("/");
        sessionCookie.setSecure(secure);
        response.addCookie(sessionCookie);
    }

    public static void setInfoCookie(HttpServletResponse response, String info, boolean remember) {
        logger.log(Level.INFO, "Set info cookie, {0}, {1}", new Object[]{info, remember});
        Cookie infoCookie = new Cookie(COOKIE_NAME_INFO, StringUtils.defaultString(info));
        infoCookie.setHttpOnly(true);
        final int maxAge;
        if (info == null) {
            maxAge = 0;
        } else if (COOKIE_INFO_LOGGED_OUT.equals(info)) {
            maxAge = 30;
        } else if (remember) {
            maxAge = (int) (getRememberTimeout() / 1000L);
        } else {
            maxAge = -1;
        }
        infoCookie.setMaxAge(maxAge);
        infoCookie.setPath("/");
        infoCookie.setSecure(false);
        response.addCookie(infoCookie);
    }

    public static void clearInfoCookie(HttpServletRequest request, HttpServletResponse response) {
        logger.log(Level.INFO, "Set info cookie");
        Cookie infoCookie = HttpUtils.getCookie(request, COOKIE_NAME_INFO);
        // if (infoCookie != null) {
        setInfoCookie(response, null, false);
        // }
    }

    /**
     * Ensure that the HTTP session and browser cookie agree on a CSRF token. If the session has
     * no CSRF token, generate one. If the browser does not have the right value, send it.
     */
    public static void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        String csrf = (String) request.getSession().getAttribute(SESSION_ATTRIBUTE_CSRF);
        if (csrf == null) {
            csrf = GuidUtil.longGuid();
            request.getSession().setAttribute(SESSION_ATTRIBUTE_CSRF, csrf);
        }
        if (!csrf.equals(HttpUtils.getCookieValue(request, COOKIE_NAME_CSRF))) {
            setCsrfCookie(response, request.isSecure(), csrf);
        }
    }

    public static void setCsrfCookie(HttpServletResponse response, boolean secure, String token) {
        logger.log(Level.INFO, "Set csrf cookie, {0}", token);
        Cookie csrf = new Cookie(COOKIE_NAME_CSRF, StringUtils.defaultString(token));
        csrf.setHttpOnly(false);
        csrf.setMaxAge(-1);
        csrf.setPath("/");
        csrf.setSecure(secure);
        response.addCookie(csrf);
    }

    public static void invalidate(HttpSession session) {
        try {
            session.invalidate();
        } catch (Exception ignored) {
        }
    }

    // This creates a session, ensuring that it has the right security level
    public static HttpSession createSession(HttpServletRequest request) {
        // Formerly, we would re-use an existing session if it was reasonably
        // valid.. Which allowed you to store some state in your session while
        // anonymous (e.g. an upload) and then still have that state after
        // logging in. Now, when asked to create a session we /always/ create
        // a new one to ensure that everything bound to the old session id is
        // terminated.
        final HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            logger.log(Level.INFO, "Invalidate http session, {0}", oldSession.getId());
            invalidate(oldSession);
        }
        final HttpSession session = request.getSession(true); // create a new session
        logger.log(Level.INFO, "Create session, {0}, {1}", new Object[]{session.getId(), session.isNew()});
        if (request.isSecure()) {
            session.setAttribute(SESSION_ATTRIBUTE_SECURE, Boolean.TRUE);
        }
        return session;
    }

    public static long getRememberTimeout() {
        return ObjectUtils.getFirstNonNullIn(Current.getDomainDTO().getRememberTimeout(), SessionConstants.DEFAULT_REMEMBER_TIMEOUT).longValue();
    }
}
