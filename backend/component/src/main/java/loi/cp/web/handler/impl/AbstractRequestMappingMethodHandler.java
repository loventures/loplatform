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
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.util.ReflectionSupport;
import com.learningobjects.cpxp.component.web.DePathSegment;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.authorization.SecurityGuard;
import com.learningobjects.de.authorization.decision.AnonymousRejector;
import com.learningobjects.de.authorization.decision.AccessDecisionManager;
import com.learningobjects.de.authorization.decision.AccessDecisionManagerFactory;
import com.learningobjects.de.authorization.decision.CompoundAccessDecisionManager;
import com.learningobjects.de.authorization.decision.UniformDecisionManager;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodResult;
import org.apache.commons.lang3.StringUtils;
import scala.compat.java8.OptionConverters;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Template class for handling a {@link RequestMapping} method.
 */
public abstract class AbstractRequestMappingMethodHandler
        implements SequencedMethodHandler {

    protected final ComponentInstance component;

    protected final RequestMappingInstance method;

    /**
     * this method's path segments (may include synthetic ones SRS added for routing)
     */
    protected final List<DePathSegment> effectivePathSegments;

    /**
     * this method's path segments that the client actually sent
     */
    protected final List<DePathSegment> pathSegments;

    /**
     * this object's spentPathSegments already includes this method's path segments
     */
    protected final SequenceContext sequenceContext;

    protected final ActorSystem actorSystem;

    protected AbstractRequestMappingMethodHandler(
            final ComponentInstance component, final RequestMappingInstance method,
            final List<DePathSegment> effectivePathSegments,
            final List<DePathSegment> pathSegments,
            final SequenceContext sequenceContext,
            final ActorSystem actorSystem) {
        this.component = component;
        this.method = method;
        this.effectivePathSegments = effectivePathSegments;
        this.pathSegments = pathSegments;
        this.sequenceContext = sequenceContext;
        this.actorSystem = actorSystem;
    }

    @Override
    public final SequencedMethodResult handle(
            final WebRequest request, final HttpServletResponse response) {

        applySecurity();

        final Object handledValue = handleForValue(request, response);

        final String transactionName;
        final java.lang.reflect.Method jlrMethod = method.getFunction().getMethod();
        if (StringUtils.isEmpty(sequenceContext.getTransactionName())) {
            final String className = StringUtils.removeEnd(jlrMethod.getDeclaringClass().getSimpleName(), "Component");
            transactionName = className + "#" + jlrMethod.getName();
        } else {
            transactionName = "#" + jlrMethod.getName();
        }

        Optional<String> deprecationMsg = Optional.empty();
//          OptionConverters.toJava(ReflectionSupport.deprecationMsg(jlrMethod))
//             .map(msg ->
//               msg.isEmpty() ? transactionName : transactionName + ": " + msg);

        final SequenceContext nextSequenceContext =
                sequenceContext
                  .with(transactionName)
                  .withDeprecationMsg(deprecationMsg);

        return handleForResult(request, response, handledValue, nextSequenceContext);
    }

    protected abstract Object handleForValue(
            final WebRequest request, final HttpServletResponse response);

    protected abstract SequencedMethodResult handleForResult(
            final WebRequest request, final HttpServletResponse response,
            final Object handledValue, final SequenceContext nextSequenceContext);

    /**
     * Connects the artifacts that Component Framework makes when it sees {@link
     * RequestMapping} and the artifacts it makes when it sees {@link Secured}.
     */
    private void applySecurity() {
        // one day, when Secured and Right are in the same module as RequestMappingFunctionDescriptor.
        // this security setup should be rolled up into the function descriptor so it is done once and
        // not on every method invocation.
        final FunctionDescriptor function = method.getFunction();
        final Secured methodSecured = function.getMethod().getAnnotation(Secured.class);
        final Class<?> declaringClass = function.getDelegate().getDelegateClass();
        final Secured typeSecured = ClassUtils.findAnnotation(declaringClass, Secured.class).orElse(null);
        final List<AccessDecisionManager> adms = new ArrayList<>();
        final List<Class<? extends SecurityGuard>> guards = new ArrayList<>();
        if (methodSecured != null) {
            adms.add(AccessDecisionManagerFactory.getAccessDecisionManager(methodSecured));
            guards.addAll(Arrays.asList(methodSecured.guard()));
        }
        if ((typeSecured != null) && ((methodSecured == null) || !methodSecured.overrides())) {
            adms.add(AccessDecisionManagerFactory.getAccessDecisionManager(typeSecured));
            guards.addAll(Arrays.asList(typeSecured.guard()));
        }
        final AccessDecisionManager adm =
            adms.isEmpty() ? new AnonymousRejector(UniformDecisionManager.ALL_ALLOWED_INSTANCE)
            : (adms.size() == 1) ? adms.get(0) : new CompoundAccessDecisionManager(adms);
        method.setAccessDecisionManager(adm);
        method.setSecurityGuards(guards.toArray(new Class[guards.size()]));
    }

}
