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

package com.learningobjects.cpxp.component;

import com.google.common.base.Joiner;
import com.google.common.reflect.TypeToken;
import com.learningobjects.cpxp.component.annotation.MaxLimit;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.function.FunctionBinding;
import com.learningobjects.cpxp.component.function.RequestMappingFunctionRegistry;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.BaseApiPage;
import com.learningobjects.cpxp.component.web.DePathSegment;
import com.learningobjects.cpxp.component.web.ErrorResponse;
import com.learningobjects.cpxp.component.web.MatrixParameterName;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverterUtils;
import com.learningobjects.cpxp.component.web.exception.HttpMediaTypeNotSupportedException;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.service.exception.HttpApiException;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.cpxp.util.lang.TypeTokens;
import com.learningobjects.de.authorization.CollectedAuthorityManager;
import com.learningobjects.de.authorization.SecuredAdvice;
import com.learningobjects.de.authorization.SecurityContext;
import com.learningobjects.de.authorization.SecurityGuard;
import com.learningobjects.de.authorization.decision.AccessDecisionManager;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.ResponseBody;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.web.message.AbsentResourceMessage;
import org.apache.commons.lang3.ArrayUtils;
import scala.util.Try;
import scalaz.$bslash$div;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains the behavior of invoking a {@link RequestMapping} with a {@link WebRequest}
 */
@FunctionBinding(
  registry = RequestMappingFunctionRegistry.class,
  annotations = RequestMapping.class
)
public class RequestMappingInstance extends DeBaseFunctionInstance {

    private static final Logger logger =
            Logger.getLogger(RequestMappingInstance.class.getName());

    private CollectedAuthorityManager _collectedAuthorityManager;

    private AccessDecisionManager _accessDecisionManager;

    private Class<? extends SecurityGuard>[] _guards;

    @Override
    public void init(final ComponentInstance instance,
            final FunctionDescriptor function) {
        super.init(instance, function);
        _collectedAuthorityManager =
                ComponentSupport.lookupService(CollectedAuthorityManager.class);
    }

    public boolean isDirect() {
        return _function.getMethod().isAnnotationPresent(ResponseBody.class);
    }

    public boolean isVoid() {
        return Void.TYPE.equals(_function.getMethod().getReturnType());
    }

    /**
     * Sucks that this has to be here, but I don't know how else ot get the output of
     * another {@code @Function} annotation on the same method (in this case the {@code
     * @Secured} annotation's AccessDecisionManager on the same method as {@code
     * @RequestMapping}). {@code @Function} wasn't meant to have two of them on one
     * method. Need to come up with something else, but punting for now. Further
     * complicating things is that {@code @Secured} has to be in :component, but this
     * class is in :core. Moving either is a big undertaking.
     */
    public void setAccessDecisionManager(
            final AccessDecisionManager accessDecisionManager) {
        this._accessDecisionManager = accessDecisionManager;
    }

    /**
     * Ditto kill me when RMI can just look at the @Secured annotation.
     */
    public void setSecurityGuards(final Class<? extends SecurityGuard>[] guards) {
        this._guards = guards;
    }

    /**
     * Invokes the function after resolving method arguments.
     *
     * @return the function return object.
     */
    public Object invoke(final WebRequest request, final HttpServletResponse response,
                         final List<DePathSegment> path) {

        request.clearOutValues();
        final Object[] args = preinvoke(request, response, path);
        final Object object = getObject();
        final Object methodResult;
        try {
            methodResult = super.invoke(object, args);
        } finally {
            cleanupArgs(args);
        }

        final Object handlerResult =
                doAfterRequestMapping(request.getPath(), methodResult, request.isEmbed());

        final SecurityContext securityContext =
                Current.getTypeSafe(SecurityContext.class);

        // Removed embedded objects from collecting various authorization handlers.  Enabling authority collection for
        // embedded objects causes an n+1 query when trying to grab non-cached things.  This has the other
        // sideeffect that it doesn't seem to actually do any rights checking after these embeds were collected.
        // TODO: Figure out good way to reject users from accessing embeds that they shouldn't have access to.
        if(!request.isEmbed()) {
            _collectedAuthorityManager.collectAuthorities(securityContext, handlerResult);
        }

        return handlerResult;
    }

    /* Horrendo but I have to parse the original request in the context of the
     * request processing phase, so I only perform business logic in the subsequent
     * asynchronous stage. */
    private Object[] asyncArgs;
    private SecurityContext asyncSC;

    public void preAsync(final WebRequest request, final HttpServletResponse response,
                         final List<DePathSegment> path) {
        asyncArgs = preinvoke(request, response, path);
        asyncSC = Current.getTypeSafe(SecurityContext.class);
        // null out Http* objects which are not valid in the asynchronous call context
        for (int i = 0; i < asyncArgs.length; ++i) {
            Object arg = asyncArgs[i];
            if ((arg instanceof HttpServletRequest) || (arg instanceof HttpServletResponse)) {
                asyncArgs[i] = null;
            } else if (arg instanceof WebRequest) {
                asyncArgs[i] = WebRequest.withoutRawRequest((WebRequest) arg);
            }
        }
    }

    public Object asyncInvoke(final AsyncContext context) throws Exception {
        Current.putTypeSafe(SecurityContext.class, asyncSC);

        if (context != null) {
            int i = ArrayUtils.indexOf(_function.getMethod().getParameterTypes(), AsyncContext.class);
            if (i >= 0) {
                asyncArgs[i] = context;
            }
        }

        final Object object = getObject();

        final Object methodResult = _function.getMethod().invoke(object, asyncArgs);

        final Object handlerResult = doAfterRequestMapping("#async", methodResult, false);

        return handlerResult;
    }

    // horrific
    public void asyncCleanup() {
        cleanupArgs(asyncArgs);
    }

    private Object[] preinvoke(final WebRequest request,
            final HttpServletResponse response, final List<DePathSegment> path) {
        Object[] args = resolveHandlerArguments(request, response, path, true);

        final List<Object> securedAdviceArgs = new ArrayList<>();
        for (int i = 0; i < _function.getMethod().getParameterTypes().length; i++) {
            for (final Annotation annotation : _function.getMethod()
                    .getParameterAnnotations()[i]) {
                if (SecuredAdvice.class.isAssignableFrom(annotation.annotationType())) {
                    securedAdviceArgs.add(args[i]);
                    break;
                }
            }
        }

        final SecurityContext securityContext =
                Current.getTypeSafe(SecurityContext.class);
        for (final Object securedAdvice : securedAdviceArgs) {
            _collectedAuthorityManager.collectAuthoritiesFromReference(securityContext,
                    securedAdvice);
        }

        //NOTE: I had to move the guard checking above the access decision manager,
        //      since BearerAuthorizationGuard is now adding Rights to the security context
        //      that will be checked by _accessDecisionManager once this changes,
        //      this change will no longer be necessary
        if (_guards != null) {
            for (Class<? extends SecurityGuard> guard : _guards) {
                // ho hum, decorate this component with the guard. should the guard be
                // an interface?
                ComponentDescriptor guardComponent =
                        ComponentSupport.getComponentDescriptor(guard.getName());
                SecurityGuard securityGuard =
                        guard.cast(_instance.getInstance(guardComponent.getDelegate()));
                securityGuard.checkAccess(request, securityContext);
            }
        }

        final String logName =
                ClassUtils.getClassAndMethodName(_function.getMethod());
        if (_accessDecisionManager == null) {
            throw new IllegalStateException("'" + logName + "' has no access decider");
        }

        if (!_accessDecisionManager.decide(securityContext)) {
            final String joinedPath = Joiner.on('/').join(path);
            logger.warning("Access denied; path: '" + joinedPath + "'; method: '" + logName);
            throw new AccessForbiddenException("Access denied; path: '" + joinedPath + "'");
        }

        return args;
    }

    private Object doAfterRequestMapping(final String request,
            final Object methodResult, boolean embed) {

        final Object processed;

        if (methodResult != null) {
            final Class<?> clazz = methodResult.getClass();
            if (OptionLike.isOptionLike(clazz)) {
                Object unwrapped = OptionLike.getOrNull(methodResult);
                if ((unwrapped == null) && !embed) {
                    throw handleResourceNotFound(request);
                }
                return unwrapped;
            } else if (Try.class.isAssignableFrom(clazz)) {
                processed = ((Try<?>) methodResult).get();
            } else if ($bslash$div.class.isAssignableFrom(clazz)) {
                $bslash$div eitherResult = ($bslash$div) methodResult;
                if (eitherResult.isRight()) {
                    processed = eitherResult.toOption().get();
                } else {
                    Object leftValue = eitherResult.swap().toOption().get();

                    // There are some @RMs that return left that are meant to
                    // be 2xx, and there are even integration tests to make
                    // sure they work, one of them is LdapLoginIntegrationTest,
                    // so we only unwrap lefty ErrorResponses

                    if (leftValue instanceof ErrorResponse) {
                        logger.warning(leftValue.toString());
                        processed = leftValue;
                    } else {
                        processed = eitherResult;
                    }
                }
            } else {
                processed = methodResult;
            }
        } else {
            processed = null;
        }

        return processed;
    }

    private HttpApiException handleResourceNotFound(String request) {
        return HttpApiException.notFound(new AbsentResourceMessage().apply(request));
    }

    private int getMaxLimit(final Annotation[] annotations) {
        for (final Annotation annotation : annotations) {
            if (annotation instanceof MaxLimit) {
                return ((MaxLimit) annotation).value();
            }
        }
        return ApiPage.UNBOUNDED_LIMIT;
    }

    @Override
    protected final ApiPage resolvePage(final DePathSegment lastSegment,
            final Annotation[] parameterAnnotations) {

        final int maxLimit = getMaxLimit(parameterAnnotations);

        final ApiQuery apiQuery = lastSegment.getApiQuery();
        final ApiPage page = apiQuery.getPage();
        final int rawLimit = page.getLimit();

        final boolean limitNotSetInUri = lastSegment.getUriPathSegment().getSystemMatrixParameters().get(
                MatrixParameterName.LIMIT) == null;
        final int effectiveLimit = limitNotSetInUri ? maxLimit : rawLimit;

        if ((maxLimit != ApiPage.UNBOUNDED_LIMIT) && (effectiveLimit > maxLimit)) {
            throw new ValidationException("limit", String.valueOf(page.getLimit()),
                    "limit must be in [1, " + maxLimit + "]");
        }

        return new BaseApiPage(page.getOffset(), effectiveLimit);
    }

    @Override
    protected final ApiQuery resolveQuery(final DePathSegment lastSegment, final boolean embed,
            final Annotation[] parameterAnnotations, final TypeToken<?> returnType) {

        final ApiQuery query = lastSegment.getApiQuery();

        final ApiPage page = resolvePage(lastSegment, parameterAnnotations);

        // because the page on `query` won't have @MaxLimit applied
        final ApiQuery.Builder builder = new ApiQuery.Builder(query).setPage(page).setEmbedded(embed);

        // add property mappings if we can infer the component interface type of a collection
        final Optional<TypeToken<?>> iterableElementType =
            TypeTokens.resolveIterableElementType(returnType, TypeToken.of(_function.getDelegate().getDelegateClass()));
        if (iterableElementType.isPresent()) {
            final Map<String, Queryable> pojoDataMappings =
                    ComponentSupport.getPojoDataMappings(iterableElementType.get().getRawType());
            builder.addPropertyMappings(pojoDataMappings);
        }

        return builder.build();
    }

    @Override
    protected final <T> T resolveRequestBody(final RequestBody requestBody, final WebRequest webRequest,
            final Type targetType) {
        final java.util.Optional<HttpMessageConverter<T>> converter =
                HttpMessageConverterUtils.getReader(webRequest.getContentType(),
                        targetType);

        if (converter.isPresent()) {
            final HttpMessageConverter<T> mc = converter.get();
            return mc.read(requestBody, webRequest, targetType);
        } else {
            /* there wasn't a converter for the given content type */
            throw new HttpMediaTypeNotSupportedException(webRequest.getContentType());
        }
    }

    @Override
    protected String methodUriTemplate() {
        return ((RequestMapping) _function.getAnnotation()).path();
    }

    private static void cleanupArgs(Object [] args) {
        for (Object arg: args) {
            if (arg instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) arg).close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error cleaning up " + arg + " after method handling", e);
                }
            }
        }
    }
}
