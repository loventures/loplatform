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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;
import com.google.common.net.HttpHeaders;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.RpcInstance;
import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.event.SessionStreamEvent;
import com.learningobjects.cpxp.operation.AbstractOperation;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.domain.DomainState;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.domain.DomainWebService.CurrentInfo;
import com.learningobjects.cpxp.service.domain.DomainWebService.CurrentStatus;
import com.learningobjects.cpxp.service.domain.SecurityLevel;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.internationalization.VersionedMessageMapCompositor;
import com.learningobjects.cpxp.service.language.LanguageService;
import com.learningobjects.cpxp.service.login.LoginException;
import com.learningobjects.cpxp.service.login.LoginWebService;
import com.learningobjects.cpxp.service.script.ScriptService;
import com.learningobjects.cpxp.service.session.SessionFacade;
import com.learningobjects.cpxp.service.session.SessionService;
import com.learningobjects.cpxp.service.session.SessionState;
import com.learningobjects.cpxp.service.session.SessionSupport;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.service.user.UserWebService;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.message.MessageMap;
import com.typesafe.config.Config;
import de.mon.SqlRedux;
import de.mon.ThreadStatistics;
import de.tomcat.juli.LogMeta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import loi.apm.Apm;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;
import scala.math.Ordering;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

/**
 * Filter that sets information about the current request - the user,
 * time, etc.
 *
 * @see Current
 * @see com.learningobjects.cpxp.controller.login.LoginController
 */
public class CurrentFilter extends AbstractFilter {
    private static final Logger logger = LoggerFactory.getLogger(CurrentFilter.class.getName());

    /**
     * Attribute to override the browser-sent locale.
     */
    public static final String SESSION_ATTRIBUTE_LOCALE = "ug:locale";

    /**
     * The domain under maintenance error type.
     */
    public static final String ERROR_TYPE_DOMAIN_MAINTENANCE = "domainMaintenance";

    /**
     * The domain suspended error type.
     */
    public static final String ERROR_TYPE_DOMAIN_SUSPENDED = "domainSuspended";

    /**
     * The domain broken  error type.
     */
    public static final String ERROR_TYPE_DOMAIN_BROKEN = "domainBroken";

    /**
     * The domain unavailable error type.
     */
    public static final String ERROR_TYPE_DOMAIN_UNAVAILABLE_BEFORE_START_DATE = "domainUnavailableBeforeStartDate";

    /**
     * The domain unavailable error type.
     */
    public static final String ERROR_TYPE_DOMAIN_UNAVAILABLE_AFTER_END_DATE = "domainUnavailableAfterEndDate";

    /**
     * The system upgrade error type.
     */
    public static final String ERROR_TYPE_SYSTEM_UPGRADE = "systemUpgrade";

    /**
     * The domain uninitialized error type.
     */
    public static final String ERROR_TYPE_DOMAIN_UNINITIALIZED = "domainUninitialized";

    /**
     * Override the server date, ms since the epoch
     */
    public static final String HTTP_HEADER_X_DATE = "X-Date";

    /**
     * Override the server date, ms since the epoch
     */
    public static final String HTTP_HEADER_X_DOMAIN_ID = "X-DomainId";

    /**
     * Used by the front-end to report who is making a request, so if the session-based user id has
     * changed due to logging in elsewhere, operations won't be performed as that new user. Has a
     * secondary usage, allowing instructors and authors to assume the identity of a fake student or
     * instructor.
     */
    public static final String HTTP_HEADER_X_USER_ID = "X-UserId";

    /**
     * Interactive request so check cookies.
     */
    public static final String HTTP_HEADER_X_INTERACTIVE = "X-Interactive";

    /**
     * Artificial domain id to access system environment.
     */
    public static final String DOMAIN_ID_SYSTEM = "*system*";

    /**
     * Bypass maintenance mode cookie. Expect "true". For more security we could generate a maintenance mode
     * guid and set that but little real benefit.
     */
    public static final String HTTP_COOKIE_MAINTENANCE_BYPASS = "MaintenanceBypass";

    /* Embed type: Embed */
    public static final String EMBED_TYPE_EMBED = "Embed";

    /* Embed type: Escaped */
    public static final String EMBED_TYPE_ESCAPED = "Escaped";

    /* Embed type: Top */
    public static final String EMBED_TYPE_TOP = "Top";

    /* Embed type: New */
    public static final String EMBED_TYPE_NEW = "New";

    /**
     * The domain web service.
     */
    @Inject
    private DomainWebService _domainWebService;

    /**
     * The user web service.
     */
    @Inject
    private UserWebService _userWebService;

    @Inject
    private ScriptService _scriptService;

    @Inject
    private Config config;

    @Inject
    private EnrollmentWebService _enrollmentWebService;

    private boolean _logStatistics;

    private static boolean __systemReady = false;

    public CurrentFilter() {
        super();
    }

    @Override
    public void init() {
        _logStatistics = config.getBoolean("com.learningobjects.cpxp.current.logStatistics");
    }

    /**
     * Record information about the user making this request.
     */
    protected void filterImpl(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws Exception {
        // Clear current request information
        Current.clear();
        final boolean isStatus = AccessFilter.isStatusCheck(request);
        try {
            ThreadStatistics.start();
            _scriptService.initComponentEnvironment();
            //ComponentEnvironment environment = ComponentManager.getComponentEnvironment();
            //Current.put(ComponentRing.class, environment.getRing());
            //WebContext.getContext().setComponentEnvironment(environment);

            ManagedUtils.perform(new CurrentOperation(request, response, chain));

            if (_logStatistics && !isStatus) {
                ThreadStatistics.statistics().foreach(queries -> {
                    StringBuilder sb = new StringBuilder("Queries: ").append(queries.count()).append(" (").append(queries.distinctCount()).append(" distinct)");
                    queries.statistics().sortBy(ss -> ss._2().count(), Ordering.Int$.MODULE$).foreach(t -> {
                        int count = t._2().count();
                        if (count >= 4) { // only log queries executed 4 or more times
                            sb.append('\n').append(count).append(": ").append(SqlRedux.simplify(t._1()));
                        }
                        return null;
                    });
                    logger.info(sb.toString());
                    return null;
                });
            }
            if (_logStatistics && !isStatus) {
                _domainWebService.logStatistics();
            }
        } catch (Exception ex) {
            int index = ExceptionUtils.indexOfType(ex, ReportableException.class);
            if (index < 0) {
                throw ex;
            }
            logger.warn("Reporting exception", ex);
            response.setContentType(MimeUtils.MIME_TYPE_TEXT_PLAIN +
              MimeUtils.CHARSET_SUFFIX_UTF_8);
            PrintWriter out = response.getWriter();
            out.println("Request failed during component initialisation:");
            response.setStatus(HTTP_INTERNAL_ERROR);
            ExceptionUtils.printRootCauseStackTrace(ex, out);
        } finally {
            ThreadStatistics.stop();
            Current.clear();
        }
    }

    private class CurrentOperation extends VoidOperation implements FilterInvocation {
        private final HttpServletRequest _request;
        private final HttpServletResponse _response;
        private final FilterChain _chain;
        private Iterator<FilterComponent> _filters;

        public CurrentOperation(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) {
            _request = request;
            _response = response;
            _chain = chain;
        }

        @Override
        public void execute() {
            try {
                FilterComponent syntheticFilter = new AbstractComponentFilter() {
                    @Override
                    public boolean filter(final HttpServletRequest request, final HttpServletResponse response, FilterInvocation invocation) throws Exception {
                        currentFilter(request, response, _chain);
                        return false;
                    }
                };
                _filters = Iterators.concat(Iterators.filter(ComponentSupport.lookupAll(FilterComponent.class).iterator(), FilterPredicate.SYSTEM), Iterators.singletonIterator(syntheticFilter));
                proceed(_request, _response);
            } catch (Throwable th) {
                doException(th, _request, _response);
            }
        }

        @Override
        public void proceed(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
            while (_filters.hasNext() && _filters.next().filter(request, response, this)) {
                // just a loop
            }
        }
    }

    private static class FilterPredicate implements Predicate<FilterComponent> {
        private boolean _system;

        private FilterPredicate(boolean system) {
            _system = system;
        }

        public boolean apply(FilterComponent filter) {
            FilterBinding binding = ComponentSupport.getBinding(filter, FilterComponent.class);
            return binding.system() == _system;
        }

        public static FilterPredicate SYSTEM = new FilterPredicate(true);
        public static FilterPredicate DOMAIN = new FilterPredicate(false);
    }

    void currentFilter(final HttpServletRequest request, HttpServletResponse response, final FilterChain chain) throws Exception {
        final String requestURI = request.getRequestURI();

        response.addHeader("P3P", "CP=\"ALL DSP COR CUR ADMa DEVa TAIa PSAa PSDa IVAa IVDa OUR BUS UNI COM NAV INT CNT STA PRE\""); // ONL breaks IE

        response.addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // TODO: list of approved js/css/font sources? response.addHeader("Content-Security-Policy", ...);
        // response.addHeader("Content-Security-Policy-Report-Only", "default-src https:; report-to /csp-violation-report-endpoint/");

        // We cannot do X-Frame-Options because we are legitimately framed

        // TODO: response.addHeader("X-Content-Type-Options", "nosniff");

        // I think we may need this in order for vimeo access control to work
        // TODO: response.addHeader("Referrer-Policy", "origin-when-cross-origin");

        // We cannot do Permissions-Policy because I don't see the point or what policy to use

        // Record the original request path for subsequent JSF URL masking and error reporting
        String queryString = request.getQueryString();
        String originalPath = requestURI + (queryString != null ? "?" + queryString : "");

        final ServletComponent servlet = ComponentSupport.lookup(ServletComponent.class, requestURI);
        if (executeServlet(servlet, request, response, true)) {
            return;
        }
        if (!__systemReady) {
            // system upgrades are only run on bootstrap, so we don't need to check for future unreadiness
            __systemReady = true; // _systemUpgradeService.isSystemReady();
            if (!__systemReady) {
                logger.debug("Forwarding to system upgrade page");
                domainError(request, response, chain, ERROR_TYPE_SYSTEM_UPGRADE, null);
                return;
            }
        }

        if (requestURI.startsWith("/control/admin")) {
            // We can't have the domain security overriding overlord security.
            chain.doFilter(request, response);
            return;
        }

        final boolean isBearerAuth = Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
          .filter(h -> h.startsWith("Bearer ")).isPresent();
        final boolean isStatic = requestURI.startsWith("/static/");
        final boolean isSys = requestURI.startsWith("/sys/");
        // TODO: this is bogus to the max; should just be `@RM(secure=true)` or summat
        final boolean isPasswordSubmission =
          requestURI.equals("/control/login")
            || requestURI.equals("/control/logout")
            || requestURI.startsWith("/secure/")
            || requestURI.startsWith("/api/v2/accounts/")
            || requestURI.startsWith("/api/v2/passwords/")
            || requestURI.startsWith("/api/v2/sessions/")
            || requestURI.startsWith("/api/v2/i18n")
            || requestURI.startsWith("/api/v2/lo_platform"); // wait what... "password submission"?!
        final boolean isSSO = requestURI.equals("/control/sso") || requestURI.equals("/control/integration") || requestURI.equals("/control/lti");
        boolean isSecure = request.isSecure();

        HttpSession session = request.getSession(false);

        // Set the current time
        Date currentTime = new Date();
        if (!BaseServiceMeta.getServiceMeta().isProdLike()) {
            String xDate = request.getHeader(HTTP_HEADER_X_DATE);
            if (StringUtils.isNotEmpty(xDate)) {
                currentTime = new Date(NumberUtils.parseLong(xDate));
            }
        }
        logger.debug("Request time, {}", currentTime);
        Current.setTime(currentTime);

        boolean overlordSession = false;
        String formCookie = null;
        if (session != null) {
            // Find the form cookie
            formCookie = (String) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_FORM_COOKIE);

            // Find the overlord session
            overlordSession = "true".equals(session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_OVERLORD));
        }

        String requestedSessionId = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION);
        String host = request.getServerName();

        CurrentInfo info = new CurrentInfo();
        info.setSessionId(requestedSessionId);
        info.setIpAddress(request.getRemoteAddr());
        info.setDomainId(request.getHeader(HTTP_HEADER_X_DOMAIN_ID));
        info.setHostName(host);
        boolean extendSession = !"true".equals(request.getHeader("X-No-Session-Extension"));
        final CurrentStatus status = _domainWebService.checkCurrentStatus(info, extendSession);

        Long currentDomain = (status.getDomain() == null) ? null : status.getDomain().getId();
        if (currentDomain == null) {
            if (host.startsWith("www.")) {
                String redirect = "https://" + host.substring(4) + originalPath;
                logger.debug("Redirect to non-www domain: " + redirect);
                HttpUtils.sendRedirect(response, redirect);
            } else {
                logger.debug("Forwarding to domain uninitialized page");
                domainError(request, response, chain, ERROR_TYPE_DOMAIN_UNINITIALIZED, null);
            }
            return;
        } else {
            LogMeta.domain(currentDomain);
        }

        Apm.addCustomParameter("domain", status.getDomain().getDomainId());

        /*String primaryHost = status.getDomain().getHostName();
        if ((requestedSessionId == null) && !isStatic
            && !requestURI.startsWith("/control") && "GET".equals(request.getMethod())
            && !requestURI.contains("!") && !host.equals(primaryHost)) {
            // redirect to the same URL at the primary hostname
            StringBuilder sb = new StringBuilder().append(request.getScheme()).append("://").append(primaryHost);
            int port = request.getServerPort();
            if (port != HttpUtils.HTTP_PORT) {
                sb.append(':').append(port);
            }
            sb.append(originalPath);
            logFine("Redirect to primary domain: " + primaryHost);
            response.sendRedirect(sb.toString());
            return;
        }*/

        logger.debug("Request domain, {}", currentDomain);

        _scriptService.initComponentEnvironment();

        SecurityLevel securityLevel = ObjectUtils.getFirstNonNullIn(status.getSecurityLevel(), SecurityLevel.NoSecurity);

        UserDTO currentUser = status.getUser();

        // If the user is viewing a course in preview mode then the preview user's identifier will
        // be reported as X-UserId and can override the effective user for this request.
        final var xUserIdHeader = request.getHeader(HTTP_HEADER_X_USER_ID);
        final var xUserIdParam = "GET".equals(request.getMethod()) ? request.getParameter(HTTP_HEADER_X_USER_ID) : null;
        final var xUserIdRequest = StringUtils.defaultIfEmpty(xUserIdHeader, xUserIdParam);
        if (StringUtils.isNotEmpty(xUserIdRequest)) {
            Long xUserId = Long.valueOf(xUserIdRequest);
            if (!xUserId.equals(currentUser.getId())) {
                final var xUser = _userWebService.getUser(xUserId);
                // preview users are progeny of the actual user
                if (xUser != null && xUser.getParentId().equals(currentUser.getId())) {
                    currentUser = UserDTO.apply(xUser);
                }
            }
        }

        String cookieInfo = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_INFO);
        boolean wasExpired = SessionUtils.COOKIE_INFO_EXPIRED.equals(cookieInfo);
        final boolean wasLoggedIn = SessionUtils.COOKIE_INFO_LOGGED_IN.equals(cookieInfo) ||
          SessionSupport.isPersistentId(requestedSessionId);

        // I don't issue a transient session id if you were logged
        // in because it is possible that you were logged in to a
        // SecureWhenLoggedIn domain and clicked on a http
        // permalink. The system will bounce you back to SSL. TODO:
        // Don't set a session id on a media request? TODO: Do erase
        // an existing session id in any case? These seem overbroad
        // to not allocate a session.. Will this behaviour cause
        // multiple session timed out errors?? Probably not.
        Current.setSessionId(requestedSessionId);
        Current.setSessionPk(status.getSessionPk());
        if (!SessionSupport.isSessionId(requestedSessionId) && !wasLoggedIn) {
            boolean secure = securityLevel == SecurityLevel.SecureAlways;
            if (secure == isSecure) { // don't mess with session cookies on the wrong ssl level
                String sessionId = SessionSupport.getTransientId();
                SessionUtils.setSessionCookie(response, secure, sessionId, false);
            }
        }

        // Note that if you were logged in via SSL and then clicked
        // on a non-SSL link, all these flags may be incorrect; but
        // you will be redirected to SSL and all will come back.
        boolean isAnonymous = currentUser.getId().equals(status.getAnonymousUser());
        boolean loginExpired = isAnonymous && (wasLoggedIn || wasExpired) &&
          !SessionState.Okay.equals(status.getSessionState());
        String domainMessage = status.getDomain().getMessage();
        request.setAttribute(SessionUtils.REQUEST_ATTRIBUTE_DOMAIN_MESSAGE, domainMessage);
        String domainPath = _domainWebService.getCurrentDomainPath() + "/";
        boolean isDomainMedia = requestURI.startsWith(domainPath);
        // I'll server domain media and SSO requests @ the wrong security level

        boolean isOverlordDomain = DomainConstants.DOMAIN_TYPE_OVERLORD.equals(status.getDomain().getType());

        if (!isStatic && !isDomainMedia && !isSSO && !isSecurityLevelValid(securityLevel, isSecure, isPasswordSubmission, isAnonymous, wasLoggedIn, overlordSession)) {
            if (loginExpired && isSecure) {
                // This implies secure login mode; my unknown session
                // cookie won't make it back to http land so I need to
                // notify of the expiration explicitly.
                logger.info("Login expired, sending expired cookies");
                SessionUtils.setSessionCookie(response, false, SessionSupport.getTransientId(), false);
                SessionUtils.setInfoCookie(response, SessionUtils.COOKIE_INFO_EXPIRED, false);
                // no need to set request attr since this is a redirect
                if (session != null) {
                    logger.warn("Invalidating session, {}", session.getId());
                    SessionUtils.invalidate(session);
                }
            }
            if ("GET".equals(request.getMethod())) {
                logger.debug("Sending security redirect, {}, {}, {}, {}, {}, {}", securityLevel, isSecure, isPasswordSubmission, isAnonymous, wasLoggedIn, overlordSession);
                HttpUtils.sendRedirect(response, HttpUtils.getUrl(request, originalPath, !isSecure));
            } else {
                // can't redirect other request types
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            if (loginExpired) {
                logger.info("Login expired, erasing cookies");
                if (!wasExpired) {
                    SessionUtils.setSessionCookie(response, false, SessionSupport.getTransientId(), false);
                    SessionUtils.setInfoCookie(response, SessionUtils.COOKIE_INFO_EXPIRED, false);
                    if (session != null) {
                        logger.info("Invalidating session, {}", session.getId());
                        SessionUtils.invalidate(session);
                    }
                }
                request.setAttribute(SessionUtils.REQUEST_ATTRIBUTE_LOGIN_EXPIRED, getExpiredString(status.getSessionState()));
            } else if (!isAnonymous && ((session == null) || ((requestedSessionId != null) && !requestedSessionId.equals(safeGetSessionAttribute(session, SessionUtils.SESSION_ATTRIBUTE_SESSION_ID))))) {
                logger.info("Updating session, {}, {}, {}", currentDomain, currentUser, loginExpired);
                session = prepareSession(request, response, currentDomain, currentUser, requestedSessionId);
            }

            if (status.getRememberSession()) {
                long rememberTime = NumberUtils.longValue((Long) session.getAttribute(SessionUtils.SESSION_ATTRIBUTE_REMEMBER_TIME));
                if (System.currentTimeMillis() - rememberTime >= SessionUtils.getRememberTimeout() / 32) { // @ 1 week, every 5 hours
                    // Refresh cookies so browser will hold onto them
                    boolean secure = securityLevel != SecurityLevel.NoSecurity;
                    SessionUtils.setSessionCookie(response, secure, requestedSessionId, true);
                    if (securityLevel == SecurityLevel.SecureWhenLoggedIn) {
                        SessionUtils.setInfoCookie(response, SessionUtils.COOKIE_INFO_LOGGED_IN, true);
                    }
                    session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_REMEMBER_TIME, System.currentTimeMillis());
                }
            }

            SessionUtils.ensureCsrfCookie(request, response);

            logger.debug("Request user, {}", currentUser);
            Current.setUserDTO(currentUser);
            Locale locale = getRequestLocale(request);
            logger.debug("Request locale, {}", locale);
            VersionedMessageMapCompositor compositor = ComponentSupport.lookupService(LanguageService.class).getDomainMessages();
            MessageMap messages = compositor.getCompositeMap(locale);
            logger.debug("Request messages, {}", messages);
            BaseWebContext.getContext().initMessages(messages);
            request.setAttribute(MessageFilter.REQUEST_ATTRIBUTE_MSG, messages);
            request.setAttribute(MessageFilter.REQUEST_ATTRIBUTE_MSG_VER, compositor.getVersion());
            response.setLocale(messages.getLocale());

            request.setAttribute(SessionUtils.SESSION_ATTRIBUTE_FORM_COOKIE, formCookie);

            SessionUtils.ensureCsrfCookie(request, response);
            List<String> roles = _enrollmentWebService
              .getActiveUserRoles(currentUser.id(), currentDomain)
              .stream()
              .map(FormattingUtils::roleStr)
              .collect(Collectors.toList());
            LogMeta.roles(CollectionConverters.asScala(roles).toList(), currentDomain);

            final UserDTO finalUser = currentUser;
            final boolean finalOverlord = overlordSession;
            FilterComponent syntheticFilter = new AbstractComponentFilter() {
                @Override
                public boolean filter(HttpServletRequest request, HttpServletResponse response, FilterInvocation invocation) throws Exception {
                    String cookieInfo = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_INFO);
                    boolean loggedOut = SessionUtils.COOKIE_INFO_LOGGED_OUT.equals(cookieInfo);
                    boolean wasExpired = SessionUtils.COOKIE_INFO_EXPIRED.equals(cookieInfo);
                    boolean isAnonymous = finalUser.getId().equals(status.getAnonymousUser());
                    boolean loginExpired = isAnonymous && (wasLoggedIn || wasExpired) &&
                      !SessionState.Okay.equals(status.getSessionState());
                    boolean loginRequired = isAnonymous && Boolean.TRUE.equals(status.getDomain().getLoginRequired());
                    Date startDate = status.getDomain().getStartDate();
                    Date endDate = status.getDomain().getEndDate();
                    Date currentDate = Current.getTime();
                    boolean domainSuspended = DomainState.Suspended == status.getDomain().getState();
                    boolean domainMaintenance = DomainState.Maintenance == status.getDomain().getState();
                    boolean domainBroken = DomainState.Init == status.getDomain().getState();
                    String domainMessage = status.getDomain().getMessage();
                    request.setAttribute(SessionUtils.REQUEST_ATTRIBUTE_DOMAIN_MESSAGE, domainMessage);
                    String domainPath = _domainWebService.getCurrentDomainPath() + "/";
                    boolean isDomainMedia = requestURI.startsWith(domainPath);

                    // We cannot display login/license required for access to Login RPC
                    // methods (e.g. about or login), or for domain media including
                    // themes, favicons, logos etc.
                    boolean isAcceptable = isSSO || isPasswordSubmission || isDomainMedia || isStatic || isSys || isBearerAuth;

                    final RpcInstance rpc = ComponentSupport.lookupRpc(requestURI);
                    final ServletComponent servlet = (rpc != null) ? null : ComponentSupport.lookup(ServletComponent.class, requestURI);
                    if ((startDate != null) && currentDate.before(startDate) && !finalOverlord) {
                        domainError(request, response, chain, ERROR_TYPE_DOMAIN_UNAVAILABLE_BEFORE_START_DATE, startDate);
                    } else if ((endDate != null) && currentDate.after(endDate) && !finalOverlord) {
                        domainError(request, response, chain, ERROR_TYPE_DOMAIN_UNAVAILABLE_AFTER_END_DATE, endDate);
                    } else if (domainSuspended && !finalOverlord) {
                        domainError(request, response, chain, ERROR_TYPE_DOMAIN_SUSPENDED, domainMessage);
                    } else if (domainMaintenance && !finalOverlord && !bypassMaintenance(request)) {
                        domainError(request, response, chain, ERROR_TYPE_DOMAIN_MAINTENANCE, domainMessage);
                    } else if (domainBroken) {
                        domainError(request, response, chain, ERROR_TYPE_DOMAIN_BROKEN, domainMessage);
                    } else if ("/".equals(requestURI) && (loggedOut || isAnonymous)) {
                        if (loggedOut) {
                            // Display a logged out message if you log out and login is
                            // required or this was an SSO session - see login controller
                            ComponentSupport.getFn("errors", "loggedOut").invoke();
                        } else {
                            ComponentSupport.getFn("errors", "login").invoke();
                        }
                    } else if (loginRequired && !isAcceptable) {
                        logger.debug("Login is required");
                        String expired =
                          (loginExpired && !"/".equals(requestURI))
                            ? getExpiredString(status.getSessionState())
                            : null;
                        ComponentSupport.getFn("errors", "loginRequired").invoke(expired);
                    } else if (!executeRpc(rpc, request, response) &&
                      !executeServlet(servlet, request, response, false)) {
                        chain.doFilter(request, response);
                    }
                    return false;
                }
            };

            final Iterator<FilterComponent> filters = Iterators.concat(Iterators.filter(ComponentSupport.lookupAll(FilterComponent.class).iterator(), FilterPredicate.DOMAIN), Iterators.singletonIterator(syntheticFilter));

            new FilterInvocation() {
                public void proceed(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    while (filters.hasNext() && filters.next().filter(request, response, this)) {
                        // just a loop
                    }
                }
            }.proceed(request, response);
        }

    }

    private Object safeGetSessionAttribute(HttpSession session, String key) {
        try {
            return session.getAttribute(key);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    boolean executeRpc(final RpcInstance rpc, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (rpc == null) {
            return false;
        }
        String binding = rpc.getBinding();
        if (binding.endsWith("*")) {
            final String requestURI = request.getRequestURI();
            final String pathInfo = StringUtils.removeStart(requestURI, binding.substring(0, binding.length() - 1));
            request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pathInfo);
        }
        /// TODO: Put APM instrumentation around this so the rpc doesn't show up as time spent in a filter
        AbstractRpcServlet.perform(rpc, request, response, java.util.logging.Logger.getLogger(CurrentFilter.class.getName()));
        return true;
    }

    boolean executeServlet(final ServletComponent servlet, final HttpServletRequest request, final HttpServletResponse response, final boolean systemPhase) {
        if (servlet == null) {
            return false;
        }
        ServletBinding binding = ComponentSupport.getBinding(servlet, ServletComponent.class);
        if (binding == null) {
            return false;
        }
        boolean systemBinding = binding.system() || DOMAIN_ID_SYSTEM.equals(request.getHeader(HTTP_HEADER_X_DOMAIN_ID));
        if (systemPhase && !systemBinding) {
            return false;
        }
        try {
            final String requestURI = request.getRequestURI();
            final String pathInfo = StringUtils.removeStart(requestURI, binding.path());
            request.setAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO, pathInfo);
            Apm.setTransactionName("servlet", ComponentSupport.getComponent(servlet).getIdentifier());
            WebResponse webResponse = servlet.service(request, response);
            ManagedUtils.commit();
            WebResponseOps.send(webResponse, request, response);
        } catch (Throwable th) {
            // I have to do this in here because if I fall out to AbstractFilter
            // to do this, current will have been cleared already.
            doException(th, request, response);
        }
        return true;
    }

    private boolean bypassMaintenance(HttpServletRequest request) {
        return StringUtils.startsWith(request.getHeader(HttpUtils.HTTP_HEADER_AUTHORIZATION), HttpUtils.AUTHORIZATION_BEARER)
          || "true".equals(HttpUtils.getCookieValue(request, HTTP_COOKIE_MAINTENANCE_BYPASS))
          || Boolean.TRUE.equals(request.getAttribute(CdnUtils.CdnRequestAttribute()));
    }

    private String getExpiredString(SessionState state) {
        if (state == SessionState.Okay) {
            return null;
        } else if (state != null) {
            return state.toString();
        } else {
            return SessionState.Expired.toString();
        }
    }

    private void domainError(HttpServletRequest request, HttpServletResponse response, FilterChain chain, String type, Object param) throws Exception {
        String uri = request.getRequestURI();
        boolean isDomainMedia = uri.startsWith("/Domain/") && Current.getDomain() != null;
        if (uri.startsWith("/static/") || uri.startsWith("/sys/") || uri.startsWith("/control/") || isDomainMedia) {
            chain.doFilter(request, response);
        } else {
            ComponentSupport.getFn("errors", "domainError").invoke(type, param);
        }
    }

    private boolean isSecurityLevelValid(SecurityLevel securityLevel, boolean isSecure, boolean isLogin, boolean isAnonymous, boolean wasLoggedIn, boolean isOverlord) {
        if (isOverlord) {
            /*
             * Previously, this condition would simply have returned the
             * isSecure value. However, to support automated functional testing,
             * Ovërlord may now be configured—at build-time—to allow insecure
             * access. This setting may not be changed after deployment. It also
             * implies that any security level is acceptable for Ovërlord
             * access. Hence, this method will return true under this condition.
             * See UG-4556 or contact Michael Ahlers for additional details.
             */
            return true;
        }

        switch (securityLevel) {
            case NoSecurity:
                return isSecure ? isLogin : true;

            case SecureWhenLoggedIn:
                // Insecure is only invalid if I was logged in
                return isSecure ? (isLogin || !isAnonymous) : !wasLoggedIn;

            default: // case SecureAlways:
                return isSecure ? true : false;
        }
    }

    /////////////////////////
    // Login Session Stuff //
    /////////////////////////

    // Returns true if the client presented a cookie and so we know
    // that cookies should be working.
    public static boolean login(final HttpServletRequest request, final HttpServletResponse response, final UserDTO user, final boolean remember) {
        return login(request, response, user, remember, null);
    }

    public static boolean login(final HttpServletRequest request, final HttpServletResponse response, final UserDTO user, final boolean remember, Properties props) {
        if (request.getRequestedSessionId() != null) {
            CpxpActorSystem.system().eventStream().publish(
              SessionStreamEvent.apply(request.getRequestedSessionId(), SessionStreamEvent.login()));
        }
        Properties properties = new Properties();
        if (props != null) {
            properties.putAll(props);
        }
        if (UserType.Overlord == user.getUserType()) {
            properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_OVERLORD, "true");
        }
        final String csrf = GuidUtil.longGuid();
        properties.setProperty(SessionUtils.SESSION_ATTRIBUTE_CSRF, csrf);
        final String requestedLoSessionId = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION);
        final String remoteAddress = request.getRemoteAddr();
        logger.debug("Login, {}, {}, {}, {}, {}", user, remember, properties, requestedLoSessionId, remoteAddress);
        SessionFacade sessionFacade = Operations.transact(new LoginOperation(requestedLoSessionId, remoteAddress, user.getId(), remember, properties));
        String loSessionId = (sessionFacade == null) ? null : sessionFacade.getSessionId();
        String userAgent = request.getHeader(HttpUtils.HTTP_HEADER_USER_AGENT);
        logger.info("Log in: " + user.getUserName() + " [" + loSessionId + "] using " + userAgent);

        if (loSessionId == null) {
            throw new LoginException("Exceeded max sessions for domain.", LoginWebService.LoginStatus.ServerError, user.getId());
        }

        SecurityLevel securityLevel = Current.getDomainDTO().getSecurityLevel();
        boolean secure = securityLevel != SecurityLevel.NoSecurity;
        boolean isOverlord = "true".equals(properties.getProperty(SessionUtils.SESSION_ATTRIBUTE_OVERLORD));
        SessionUtils.setSessionCookie(response, secure, loSessionId, remember);
        SessionUtils.setCsrfCookie(response, secure, csrf);
        Current.setSessionPk(sessionFacade.getId());

        if (securityLevel == SecurityLevel.SecureWhenLoggedIn) {
            SessionUtils.setInfoCookie(response, SessionUtils.COOKIE_INFO_LOGGED_IN, remember);
        } else {
            SessionUtils.clearInfoCookie(request, response);
        }

        if (isOverlord || (secure == request.isSecure())) {
            HttpSession session = prepareSession(request, response, Current.getDomain(), user, loSessionId);
            if (remember) {
                // Record that I've sent cookies
                session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_REMEMBER_TIME, System.currentTimeMillis());
            }
            for (String key : properties.stringPropertyNames()) {
                session.setAttribute(key, properties.get(key));
            }
        } else if (!loSessionId.equals(requestedLoSessionId)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                // destroy old session
                SessionUtils.invalidate(session);
            }
        }

        Current.setUserDTO(user);

        Apm.incrementCounter("Custom/loginCount");

        return (requestedLoSessionId != null);
    }

    private static class LoginOperation extends AbstractOperation<SessionFacade> {
        @Inject
        private SessionService _sessionService;

        @Inject
        private LoginWebService _loginWebService;

        private String _requestedSessionId;
        private String _remoteAddress;
        private Long _user;
        private boolean _remember;
        private Properties _properties;

        public LoginOperation(String requestedSessionId, String remoteAddress, Long user, boolean remember, Properties properties) {
            _requestedSessionId = requestedSessionId;
            _remoteAddress = remoteAddress;
            _user = user;
            _remember = remember;
            _properties = properties;
        }

        public SessionFacade perform() {
            _sessionService.closeSession(_requestedSessionId);
            SessionFacade session = _sessionService.openSession(_user, _remember, _remoteAddress);
            if (session == null) {
                return null;
            }
            if ((_properties != null) && !_properties.isEmpty()) {
                EntityContext.flush(); // necessary in order that the session be available to find
                _sessionService.setProperties(session.getSessionId(), _properties);
            }
            if ((_properties == null) || !"true".equals(_properties.getProperty(SessionUtils.SESSION_ATTRIBUTE_OVERLORD))) {
                _loginWebService.recordLogin(_user);
            }
            return session;
        }
    }

    private static HttpSession prepareSession(HttpServletRequest request, HttpServletResponse response, Long domainId, UserDTO user, String sessionId) {
        HttpSession session = SessionUtils.createSession(request);
        session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_SESSION_ID, sessionId);

        final String userName = (UserType.Overlord == user.getUserType()) ? "*" + user.getUserName() + "*" : user.getUserName();
        // Throwing these in the session allow `LogFilter` to set them up at the very
        // start of request processing during subsequent requests.
        session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_DOMAIN_ID, domainId);
        session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_USER_ID, user.getId());
        session.setAttribute(SessionUtils.SESSION_ATTRIBUTE_USER_NAME, userName);
        LogMeta.domain(domainId);
        LogMeta.user(user.getId());
        Optional.ofNullable(userName).ifPresent(LogMeta::username); // overlord anonymous user has no username

        SessionService sessionService = ServiceContext.getContext().getService(SessionService.class);
        Properties properties = sessionService.getProperties(sessionId);
        logger.debug("Preparing session, {}, {}, {}, {}", user, sessionId, session.getId(), properties);

        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                session.setAttribute((String) entry.getKey(), entry.getValue());
            }
        }

        return session;
    }

    public static void logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getRequestedSessionId() != null) {
            CpxpActorSystem.system().eventStream().publish(
              SessionStreamEvent.apply(request.getRequestedSessionId(), SessionStreamEvent.logout()));
        }

        final String loSessionId = HttpUtils.getCookieValue(request, SessionUtils.COOKIE_NAME_SESSION);
        if (SessionSupport.isPersistentId(loSessionId)) {
            Operations.transact(new CloseSessionOperation(loSessionId));
        }

        SecurityLevel securityLevel = Current.getDomainDTO().getSecurityLevel();
        Cookie sessionCookie = new Cookie(SessionUtils.COOKIE_NAME_SESSION, SessionSupport.getTransientId());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(-1);
        sessionCookie.setPath("/");
        sessionCookie.setSecure(securityLevel == SecurityLevel.SecureAlways);
        response.addCookie(sessionCookie);

        SessionUtils.clearInfoCookie(request, response);

//            getLogger().info(
//                    "Log out: [" + getSession().getId() + "]");

        HttpSession session = request.getSession(false);
        if (session != null) {
            SessionUtils.invalidate(session);
        }

        UserWebService userWebService = ServiceContext.getContext().getService(UserWebService.class);
        Current.setUserDTO(userWebService.getUserDTO(userWebService.getAnonymousUser()));
    }

    private static class CloseSessionOperation extends VoidOperation {
        @Inject
        private SessionService _sessionService;

        private String _sessionId;

        public CloseSessionOperation(String sessionId) {
            _sessionId = sessionId;
        }

        public void execute() {
            _sessionService.closeSession(_sessionId);
        }
    }

    static Locale getRequestLocale(HttpServletRequest request) {
        return (Locale) Optional
          .ofNullable(request.getSession())
          .flatMap(s -> Optional.ofNullable(s.getAttribute(SESSION_ATTRIBUTE_LOCALE)))
          .orElse(request.getLocale());
    }
}
