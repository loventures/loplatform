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

package loi.cp.web.handler.impl;

import org.apache.pekko.actor.ActorSystem;
import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.service.exception.AuthorizationException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.SessionUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.ThreadTerminator;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodScope;
import org.apache.commons.collections4.ListUtils;
import scala.concurrent.Future;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static com.learningobjects.cpxp.util.SessionUtils.REQUEST_PARAMETER_BEARER_AUTHORIZED;

/**
 * A scope that uses the component framework to find the next method handler.
 */
public class ComponentFrameworkSequencedMethodScope implements SequencedMethodScope {
    private static final Logger logger = Logger.getLogger(ComponentFrameworkSequencedMethodScope.class.getName());

    private final SequenceContext sequenceContext;

    private final ComponentInstance component;

    private final ActorSystem actorSystem;

    private final boolean addSyntheticClassLevelPathSegment;

    private final ComponentEnvironment componentEnvironment;

    /**
     * @param sequenceContext a place to accumulate data as we process the sequence
     * @param component the component whose methods are available to invoke in this scope
     * @param actorSystem a dependency for one of the method handlers that this scope
 * might create (they don't get DI, TODO inject their dependencies)
     * @param addSyntheticClassLevelPathSegment whether or not this scope should prepend
* the class level {@link RequestMapping} path to the unhandled path segments of the
* request-URI when searching for the next method handler. The class level {@code
* RequestMapping} of {@code component} is selected from the first such annotated
* class that is found when walking {@code component}'s type tree using
* {@link BreadthFirstSupertypeIterable}.
     * @param componentEnvironment
     */
    public ComponentFrameworkSequencedMethodScope(
      final SequenceContext sequenceContext, final ComponentInstance component,
      final ActorSystem actorSystem,
      final boolean addSyntheticClassLevelPathSegment, ComponentEnvironment componentEnvironment) {
        this.sequenceContext = sequenceContext;
        this.component = component;
        this.actorSystem = actorSystem;
        this.addSyntheticClassLevelPathSegment = addSyntheticClassLevelPathSegment;
        this.componentEnvironment = componentEnvironment;
    }

    @Override
    public SequencedMethodHandler findNextHandler(final WebRequest request) {

        final Method method = request.getMethod();
        final String requestBodySchemaName = request.getRequestBodySchemaName();

        final List<DePathSegment> allSpent = sequenceContext.getSpentPathSegments();
        final List<DePathSegment> all = request.getDePathSegments();
        final List<DePathSegment> unhandled = all.subList(allSpent.size(), all.size());

        final Optional<RequestMapping> classAnnotation = getClassLevelAnnotation();

        final List<DePathSegment> effectiveUnhandled;
        if (classAnnotation.isPresent() && addSyntheticClassLevelPathSegment) {

            /*
             * Carry over matrix params from prior segment to synthetic segment. We only
             * ever add synthetic segments when the prior handler was an `any` handler
             * that composes two APIs. In that case, the 'any' handler is just a promise
             * that will be fulfilled by the handler that this `findNextHandler`
             * invocation will return. So it makes sense to carry over the matrix params.
             * Should anything else be carried over?
             */
            final DePathSegment synthetic;
            final String classLevelPath = classAnnotation.get().path();
            final DePathSegment previousSegment = Iterables.getLast(allSpent, null);
            if (previousSegment != null) {
                final UriPathSegment syntheticUps = previousSegment.getUriPathSegment().withSegment(classLevelPath);
                synthetic = new DePathSegment(syntheticUps, previousSegment.getApiQuery());
            } else {
                /*
                 * 90% sure this branch will never happen because 90% sure there had to be
                 * a prior segment for `addSyntheticClassLevelPathSegment` to be true
                 */
                synthetic = new DePathSegment(new UriPathSegment(classLevelPath));
            }

            effectiveUnhandled = ListUtils.union(List.of(synthetic), unhandled);
        } else {

            effectiveUnhandled = unhandled;
        }

        final RequestMappingInstance found = component
                .getFunctionInstance(RequestMappingInstance.class, method,
                        effectiveUnhandled, requestBodySchemaName);
        if (found == null) {
            throw new ResourceNotFoundException(request.getPath());
        }

        final FunctionDescriptor function = found.getFunction();
        final RequestMapping methodAnnotation = function.getAnnotation();


        /*
         *`effective` means the segments include synthetic UriPathSegments (that is, path
         * segments that are not found in the request-URI)
         */
        final int effectiveHandledCount = getEffectiveHandledCount(classAnnotation, methodAnnotation);
        final List<DePathSegment> effectiveHandled = effectiveUnhandled.subList(0, effectiveHandledCount);

        final int handledCount = getHandledCount(classAnnotation, methodAnnotation);
        final List<DePathSegment> handled = unhandled.subList(0, handledCount);

        final boolean noMorePath = handled.size() == unhandled.size();
        final boolean last = noMorePath && methodAnnotation.method() != Method.Any;

        /* If we're using a potentially mutative HTTP method, but aver that
         * we would be using a GET or something were it not for some sort of
         * pressing reason (excuses, excuses), then treat the request like a GET
         * and make it time out like a GET. */
        if (last && methodAnnotation.mode() == Mode.READ_ONLY) {
            ThreadTerminator.register();
        }

        // A request is pre-authorized if either null (implies an internal call) or it
        // received bearer authorization and thus does not need a CSRF token.
        final boolean preAuthorized = (request.getRawRequest() == null) ||
            Boolean.TRUE.equals(request.getRawRequest().getAttribute(REQUEST_PARAMETER_BEARER_AUTHORIZED));
        if (last && method.isUpdate() && methodAnnotation.csrf() && !preAuthorized) {
            csrfCheck(request.getRawRequest());
        }

        final SequenceContext nextSequenceContext =
                sequenceContext.withPathSegments(effectiveHandled, handled);

        final SequencedMethodHandler nextHandler;
        if (methodAnnotation.async() && last) {
            nextHandler = new AsyncRequestMappingMethodHandler(component, found,
                  effectiveHandled, handled, nextSequenceContext, actorSystem);
        } else if (isFuture(function.getMethod().getGenericReturnType())) {
            nextHandler = new FutureMethodHandler(component, found, effectiveHandled, handled,
              nextSequenceContext, actorSystem, componentEnvironment);
        } else {
            nextHandler = new RequestMappingMethodHandler(component, found, effectiveHandled, handled,
                    nextSequenceContext, actorSystem, componentEnvironment);
        }

        return nextHandler;
    }

    // Is this type either a future or a right future
    private static boolean isFuture(Type tpe) {
        if (tpe instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) tpe;
            Type rawType = pType.getRawType();
            if (rawType.equals(Future.class)) {
                return true;
            } else if (rawType.equals(scalaz.$bslash$div.class)) {
                Type rType = pType.getActualTypeArguments()[1]; // +1 for the R-Type reference, that developer
                if (rType instanceof ParameterizedType) {
                    ParameterizedType prType = (ParameterizedType) rType;
                    return prType.getRawType().equals(Future.class);
                }
            }
        }
        return false;
    }

    private void csrfCheck(HttpServletRequest request) {
        final String csrf = Optional.ofNullable(request.getHeader("X-CSRF"))
          .orElseThrow(() -> new AuthorizationException("CSRF deny"));
        if (!"true".equals(csrf) && !csrf.equals(request.getSession(true).getAttribute(SessionUtils.SESSION_ATTRIBUTE_CSRF))) {
            logger.info("CSRF token mismatch: " + request.getRequestURI());
        }
    }

    @Override
    public boolean hasNextHandler() {
        return true;
    }

    private Optional<RequestMapping> getClassLevelAnnotation() {

        final Class<?> componentClass = component.getComponent().getComponentClass();
        final Optional<Class<?>> requestMappingClass =
            BreadthFirstSupertypeIterable.from(componentClass)
              .filter(ClassUtils.hasAnnotation(RequestMapping.class))
              .findFirst();

        return requestMappingClass.map(c -> c.getAnnotation(RequestMapping.class));
    }

    private int getEffectiveHandledCount(
            final Optional<RequestMapping> classAnnotation,
            final RequestMapping methodAnnotation) {

        final String handledPath;
        if (classAnnotation.isPresent()) {
            if (methodAnnotation.path().isEmpty()) {
                handledPath = classAnnotation.get().path();
            } else {
                handledPath = classAnnotation.get().path() + "/" + methodAnnotation.path();
            }
        } else {
            handledPath = methodAnnotation.path();
        }

        return countPathSegments(handledPath);
    }

    private int getHandledCount(
            final Optional<RequestMapping> classAnnotation,
            final RequestMapping methodAnnotation) {

        final String handledPath;
        if (addSyntheticClassLevelPathSegment || !classAnnotation.isPresent()) {
            handledPath = methodAnnotation.path();
        } else {

            if (methodAnnotation.path().isEmpty()) {
                handledPath = classAnnotation.get().path();
            } else {
                handledPath = classAnnotation.get().path() + "/" + methodAnnotation.path();
            }
        }

        return countPathSegments(handledPath);

    }

    private int countPathSegments(final String str) {

        if (StringUtils.isEmpty(str)) {
            return 0;
        } else {
            return StringUtils.countMatches(str, "/") + 1;
        }

    }

}
