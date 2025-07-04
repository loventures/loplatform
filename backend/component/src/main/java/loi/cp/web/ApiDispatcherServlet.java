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

package loi.cp.web;

import loi.apm.Apm;
import org.apache.pekko.actor.ActorSystem;
import com.google.common.net.HttpHeaders;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.component.web.converter.ConvertOptions;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.HttpApiException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.authorization.CollectedAuthorityManager;
import com.learningobjects.de.authorization.CollectedRights;
import com.learningobjects.de.authorization.IntegrationAuthority;
import com.learningobjects.de.authorization.SecurityContext;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.apikey.ApiKeySystem;
import loi.cp.i18n.BundleMessage;
import loi.cp.i18n.Translatable;
import loi.cp.integration.IntegrationService;
import loi.cp.oauth.server.OAuthService;
import loi.cp.right.RightService;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;
import loi.cp.web.handler.impl.ComponentFrameworkSequencedMethodScope;
import loi.cp.web.handler.impl.SequenceContext;
import org.apache.http.HttpStatus;
import scala.Option;
import scala.jdk.javaapi.CollectionConverters;

import javax.inject.Inject;
import javax.validation.groups.Default;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.util.SessionUtils.REQUEST_PARAMETER_BEARER_AUTHORIZED;

/**
 * Central dispatcher for API requests. Dispatches to registered top-level components
 * ({@link ApiRootComponent}) for processing a web request. The top-level component's
 * {@link RequestMapping}s are in turn used to dispatch the web request if the path
 * remains to be looked up after routing to the top-level component.
 */
@Component(alias = "srs")
@ServletBinding(path = "/api")
public class ApiDispatcherServlet extends AbstractComponent implements ServletComponent {
    private static final Logger logger = Logger.getLogger(ApiDispatcherServlet.class.getName());

    @Inject
    private ExceptionResolver _exceptionResolver;

    @Inject
    private WebRequestFactory _webRequestFactory;

    @Inject
    private CollectedAuthorityManager _collectedAuthorityManager;

    @Infer
    private ActorSystem _actorSystem;

    @Inject
    private ComponentEnvironment componentEnvironment;

    @Inject
    private Translatable<BundleMessage> _bundleTranslator;

    @Fn // HtmlFilter support for $$api('v2/...')
    public Object api(@Parameter String path) {
        return api(path, false);
    }

    @Fn // ComponentUtils support for $$json_api('v2/...')
    public String jsonapi(@Parameter String path) throws Exception {
        Object result = api(path, true);
        return JacksonUtils.getMapper().writerWithView(Default.class)
                .writeValueAsString(result);
    }

    private Object api(@Parameter String path, boolean json) {
        final WebRequest webRequest = _webRequestFactory.create(Method.GET, "/api/" + path);
        try {
            HttpResponse httpResponse = doDispatch(webRequest, null, json);
            return (httpResponse instanceof HttpResponseEntity<?>) ?
                    ((HttpResponseEntity<?>) httpResponse).get() : httpResponse;
        } catch (ResourceNotFoundException ex) {
            return null;
        } catch (HttpApiException ex) {
            if (ex.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw ex;
            }
            return null;
        }
    }

    @Override
    public WebResponse service(
            final HttpServletRequest request, final HttpServletResponse response) {

        HttpUtils.setExpired(response); // this really should fold into response writing but...

        try {
            final WebRequest webRequest = _webRequestFactory.create(request);

            // TODO: Refactor me clean

            if (Current.isAnonymous()) {
                Option<HttpResponse> bearerResponse = ComponentSupport.lookupService(OAuthService.class).bearerAuthorize(request);
                if (bearerResponse.isDefined()) {
                    writeResponseWithMessageConverters(bearerResponse.get(), request, response, webRequest.getAcceptedMediaTypes(), null);
                    return NoResponse.instance();
                }
            }

            // TODO: disable the retry machinery for SRS

            final HttpResponse responseValue = doDispatch(webRequest, response, true);

            if (!Method.GET.equals(webRequest.getMethod())) {
                // Commit any changes before sending data back to the client..
                // TODO: will this result in detached entites?
                // TODO: how to make a GET request throw on data model change?
                ManagedUtils.commit();
            }

            writeResponseWithMessageConverters(responseValue, request, response,
              webRequest.getAcceptedMediaTypes(), webRequest.getOutValue(CacheOptions.class));

        } catch (Throwable ex) {

            final HttpResponse httpResponse = _exceptionResolver.resolveException(ex);

            ManagedUtils.rollback();

            writeResponseWithMessageConverters(httpResponse, request, response,
                    Collections.singletonList(MediaType.ALL), null);
        }
        return NoResponse.instance();
    }

    private void writeResponseWithMessageConverters(
            final HttpResponse httpResponse, final HttpServletRequest request,
            final HttpServletResponse response, final List<MediaType> acceptedMediaTypes,
            final CacheOptions cacheOptions) {

        // TODO: I don't like this but I'm having a hard time working out
        // the alternative.
        final Object responseBody = (httpResponse instanceof HttpResponseEntity<?>) ?
                ((HttpResponseEntity<?>) httpResponse).get() : httpResponse;
        if (httpResponse.statusCode() > 0) {
            response.setStatus(httpResponse.statusCode());
            if (httpResponse.statusCode() == HttpServletResponse.SC_NO_CONTENT) {
                // avoid Firefox whining
                response.setContentType(
                  MimeUtils.MIME_TYPE_TEXT_PLAIN + MimeUtils.CHARSET_SUFFIX_UTF_8);
            }
        }
        if (responseBody != null) {
            if (responseBody instanceof WebResponse) {
                WebResponseOps.send((WebResponse) responseBody, request, response);
                return;
            } else for (final MediaType acceptedMediaType : acceptedMediaTypes) {
                for (final HttpMessageConverter converter : ComponentSupport
                        .getComponents(HttpMessageConverter.class)) {
                    if (converter.canWrite(responseBody, acceptedMediaType)) { // TODO: this should just be a registry
                        converter.write(responseBody, new ConvertOptions(acceptedMediaType, Optional.ofNullable(cacheOptions)), request, response);
                        return;
                    }
                }
            }
            // bogus: this error occurs after commit, but is an edge case..
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }

    }

    private HttpResponse doDispatch(
            final WebRequest webRequest, final HttpServletResponse response,
            boolean jsonResponse) {

        /* find root component (will be in a global registry) */
        final String rootLookupPath = webRequest.getPath();
        final Method httpMethod = webRequest.getMethod();
        final String requestBodyType = webRequest.getRequestBodySchemaName();

        final SecurityContext securityContext = new SecurityContext();
        CollectedRights rights = new CollectedRights();
        if (response != null) {
            collectRightsFromRequestAuthorization(webRequest, securityContext, rights);
        }
        securityContext.put(CollectedRights.class, rights);
        Current.putTypeSafe(SecurityContext.class, securityContext);
        _collectedAuthorityManager
                .collectAuthoritiesFromReference(securityContext, Current.getDomain());

        final SequencedMethodScope rootScope =
                findRootScope(httpMethod, rootLookupPath, requestBodyType);

        final SequencedMethodResult methodResult =
                continueSequence(rootScope, webRequest, response);

        return processResult(methodResult, response, jsonResponse, webRequest);

    }

    // TODO: kill this legacy bearer auth... Or roll this into the OAuth bearer auth
    // where we actually log in as the system. That, of course, will probably break
    // all of our integration tests that may rely on the current ill behaviour of
    // users logging in plus sending bearer auth.. do this in another PR.

    private static final String BEARER_PREFIX = "Bearer ";

    private void collectRightsFromRequestAuthorization(final WebRequest request, final SecurityContext securityContext, final CollectedRights rights) {
        String authorization = StringUtils.trim(request.getRawRequest().getHeader(HttpHeaders.AUTHORIZATION));
        if (StringUtils.startsWith(authorization, BEARER_PREFIX)) {
            String key = StringUtils.removeStart(authorization, BEARER_PREFIX).trim();
            ApiKeySystem system = ComponentSupport.lookupService(IntegrationService.class)
              .getApiKeyBySecret(key, request.getRawRequest().getRemoteAddr());
            if (system != null) {
                securityContext.put(IntegrationAuthority.class, new IntegrationAuthority(system));
                logger.info("Bearer auth: " + system.getName() + " / " + system.getRights());
                if (Current.isAnonymous()) {
                    Current.setUserDTO(system.getSystemUser().toDTO());
                }
                rights.addAll(CollectionConverters.asJava(system.getRightClasses(ComponentSupport.lookupService(RightService.class))));
                request.getRawRequest().setAttribute(REQUEST_PARAMETER_BEARER_AUTHORIZED, true);
            }
        }
    }

    private SequencedMethodResult continueSequence(
            final SequencedMethodScope scope, final WebRequest request,
            final HttpServletResponse response) {

        final SequencedMethodHandler methodHandler = scope.findNextHandler(request);

        final SequencedMethodResult methodResult =
                methodHandler.handle(request, response);

        final SequencedMethodScope nextScope = methodResult.getNextScope();
        if (nextScope.hasNextHandler()) {
            return continueSequence(nextScope, request, response);
        } else {
            return methodResult;
        }

    }

    private HttpResponse processResult(
            final SequencedMethodResult methodResult,
            final HttpServletResponse response, final boolean jsonResponse, final WebRequest req) {

        final SequenceContext sequenceContext = methodResult.getSequenceContext();
        final String transactionName = sequenceContext.getTransactionName();
        final Object value = methodResult.getValue();

        if (response != null) {
            Apm.setTransactionName("srs", transactionName);
            sequenceContext.getDeprecation().ifPresent(msg -> {
                response.addHeader("X-Deprecation", msg);
            });
            SrsLog.logRoute(transactionName, req.getMethod());
        }

        if (value instanceof HttpResponse) {
            /* response is just some object, can't expand it or get metadata for it */
            return (HttpResponse) value;
        }

        return HttpResponseEntity.okay(value);

    }

    private SequencedMethodScope findRootScope(
            final Method method, final String path, final String requestBodySchemaName) {

        // throws if cannot find value for 'found'
        final ApiRootComponent found = ComponentSupport
                .lookup(ApiRootComponent.class, path, method, requestBodySchemaName);

        final ComponentInstance component = found.getComponentInstance();

        final SequenceContext sequenceContext = new SequenceContext();
        return new ComponentFrameworkSequencedMethodScope(sequenceContext, component,
                _actorSystem, false, componentEnvironment);
    }

}
