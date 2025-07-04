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

import com.google.common.net.HttpHeaders;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Fn;
import com.learningobjects.cpxp.component.annotation.Parameter;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.HtmlTemplate;
import com.learningobjects.cpxp.component.web.HtmlResponse;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.component.web.WebResponseOps;
import com.learningobjects.cpxp.filter.CurrentFilter;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceException;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.admin.right.AdminRight;
import loi.cp.admin.right.HostingAdminRight;
import loi.cp.right.Right;
import loi.cp.right.RightService;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: This should return the responses and let the caller render them.

@Component(alias = "errors")
public class Errors extends AbstractComponent {

    @Inject
    private HttpServletRequest request;

    @Inject
    private HttpServletResponse response;

    @Inject
    private ComponentEnvironment componentEnvironment;

    private String DEFAULT_I18N_ACCESS_DENIED_TITLE = "AccessDenied";
    private String DEFAULT_I18N_ACCESS_DENIED_MESSAGE = "AccessDeniedMessage";

    private static final Logger logger = Logger.getLogger(Errors.class.getName());
    private final Set<Class<? extends Right>> adminRights = Set.of(AdminRight.class, HostingAdminRight.class);
    /*
     * This is used for domain-level errors when there is no valid domain environment in scope.
     */
    @Fn
    public void domainError(@Parameter String type, @Parameter Object param) {
        String title = InternationalizationUtils.formatMessage
            ("error_domainError_" + type + "_title");
        String body = InternationalizationUtils.formatMessage
            ("error_domainError_" + type + "_instructions_html", param);
        final int sc;
        if (CurrentFilter.ERROR_TYPE_DOMAIN_MAINTENANCE.equals(type)
            || CurrentFilter.ERROR_TYPE_SYSTEM_UPGRADE.equals(type)) {
            sc = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            response.addHeader(HttpHeaders.RETRY_AFTER, "300"); // 5 minutes
        } else {
            sc = HttpServletResponse.SC_NOT_FOUND;
        }
        String errorPage =
          (Current.getDomain() == null) ? "domainError.html"
            : CurrentFilter.ERROR_TYPE_DOMAIN_MAINTENANCE.equals(type) ? "sys/maintenance.html"
              : "sys/domainError.html";
        send(HtmlResponse.apply(HtmlTemplate.apply(this, errorPage)
                .bind("type", type).bind("title", title).bind("body", body)
                .bind("param", param), sc));
    }

    /**
     * No content was found at the requested path.
     */
    @Fn
    public void notFound() {
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/notFound.html").bind("title", null).bind("body", null), HttpServletResponse.SC_NOT_FOUND));
    }

    /**
     * Forbidden access to content, drives to either login required or access denied.
     */
    @Fn
    public void forbidden(@Parameter Optional<ServiceException> maybeException) {
        if (Current.isAnonymous()) {
            String expired = SessionUtils.hasLoginExpired(request, response);
            loginRequired(expired);
        } else {
            String title = InternationalizationUtils.formatMessage(DEFAULT_I18N_ACCESS_DENIED_TITLE);
            String message = InternationalizationUtils.formatMessage(DEFAULT_I18N_ACCESS_DENIED_MESSAGE);
            if(maybeException.isPresent()){
                ServiceException serviceException = maybeException.get();
                if(serviceException.getTitle() != null){
                    title = serviceException.getTitle();
                }
                if(serviceException.getMessage() != null){
                    message = serviceException.getMessage();
                }
            }
            accessDenied0(title, message);
        }
    }

    /**
     * Not logged in, not session expired, accessing protected content.
     */
    @Fn
    public void loginRequired(String type) {
        String title = InternationalizationUtils.formatMessage
            ((type != null) ? "error_session" + type + "_title"
             : "error_loginRequired_title");
        String body = InternationalizationUtils.formatMessage
            ((type != null) ? "error_session" + type + "_instructions_html"
             : "error_loginRequired_instructions_html");
        // Roll headers into WebResponse/HttpResponse?
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "FormBased");
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/loginRequired.html")
                .bind("type", type).bind("title", title).bind("body", body), HttpServletResponse.SC_UNAUTHORIZED));
    }

    /**
     * Domain login page.
     */
    @Fn
    public void login() {
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/login.html")
          .bind("title", null).bind("body", null)));
    }

    /**
     * Logged in, accessing protected content.
     */
    @Fn
    public void accessDenied() {
        accessDenied0(InternationalizationUtils.formatMessage(DEFAULT_I18N_ACCESS_DENIED_TITLE),
          InternationalizationUtils.formatMessage(DEFAULT_I18N_ACCESS_DENIED_MESSAGE));
    }

    private void accessDenied0(String title, String message) {
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/accessDenied.html")
          .bind("title", title).bind("body", message), HttpServletResponse.SC_FORBIDDEN));
    }

    /**
     * After logging out of federated login. So you don't get bounced straight back to the federated
     * login provider.
     */
    @Fn
    public void loggedOut() {
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/loggedOut.html").bind("title", "").bind("body", ""), HttpServletResponse.SC_OK));
    }

    /**
     * An understood bad request.
     */
    @Fn
    public void badRequest(@Parameter ServiceException exception) {
        String title, body;
        String type = exception.getMsg();
        title = InternationalizationUtils.getMessages().getMessage
          ("error_serviceError_" + type + "_title"); // vanishingly few of these..
        if (title == null) { // UserException where the msg is actually the error
            body = StringEscapeUtils.escapeHtml4
              (ComponentUtils.i18n(exception.getMessage(), getComponentDescriptor()));
            title = InternationalizationUtils.formatMessage
              ("error_serviceError_badRequest_title");
        } else {
            body = InternationalizationUtils.formatMessage
              ("error_serviceError_" + type + "_instructions_html", exception.getParams());
        }
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/badRequest.html")
                .bind("title", title).bind("body", body), HttpServletResponse.SC_BAD_REQUEST));
    }

    /**
     * Unexpected internal error.
     */
    @Fn
    public void internalError(@Parameter Throwable exception,
                              @Parameter String guid) {
        if (response.isCommitted() || !EntityContext.inSession()) {
            return; // nothing to be done at this point.. less likely for the other errors
        }

        boolean isAdmin = (Current.getUser() != null) &&
          ComponentSupport.optionalLookupService(RightService.class)
            .filter(rs -> !SetUtils.intersection(adminRights, rs.getUserRights()).isEmpty()).isPresent();
        send(HtmlResponse.apply(HtmlTemplate.apply(this, "sys/internalError.html")
                .bind("exception", exception).bind("guid", guid).bind("debug", BaseWebContext.getDebug() || isAdmin), HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    }

    void send(WebResponse template) {
        try {
            // hack for uberlord spa
            if (getComponentDescriptor().getIdentifier().startsWith("loi.cp.overlord")
                && WebResponseOps.isInteractive(request)
                && (template.statusCode() != HttpServletResponse.SC_INTERNAL_SERVER_ERROR)) {
                template = HtmlResponse.apply(HtmlTemplate.apply(this, "index.html").bind("statusCode", template.statusCode()));
            }
            WebResponseOps.send(template, request, response);
        } catch (Exception ex) {
            if (ExceptionUtils.indexOfThrowable(ex, ClientAbortException.class) < 0) {
                logger.log(Level.WARNING, "Error sending error response", ex);
            }
        }
    }

    @SuppressWarnings("unused") // lohtml embed component config
    public Map<String, Object> getComponentConfiguration() {
        // This is somewhat wrong, but with the conjoined domain and error components
        // from pcp-ux-domain this is less invasive than other options
        final ComponentDescriptor domainComponent = ComponentSupport.lookupComponent
          (ItemSiteComponent.class, DomainConstants.ITEM_TYPE_DOMAIN, null, null);
        return (domainComponent == null) ? new HashMap<>()
          : componentEnvironment
              .getJsonConfiguration(domainComponent.getIdentifier());
    }
}
