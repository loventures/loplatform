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

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.exception
        .HttpRequestMethodNotSupportedException;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.exception.AccessForbiddenException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.lang.OptionLike;
import com.learningobjects.de.authorization.SecurityContext;
import com.learningobjects.de.authorization.decision.AccessDecisionManager;
import com.learningobjects.de.authorization.decision.AccessDecisionManagerFactory;
import com.learningobjects.de.web.Deletable;
import com.learningobjects.de.web.DeletableEntity;
import loi.cp.web.HttpResponseEntity;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodResult;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * Handles processing of the {@link Deletable} annotation.
 */
public class DeletableMethodHandler implements SequencedMethodHandler {

    private static final Logger logger =
            Logger.getLogger(DeletableMethodHandler.class.getName());

    private final ComponentInstance deletableAccessorComponent;

    private final RequestMappingInstance deletableAccessor;

    private final SequencedMethodResult deletableResult;

    public DeletableMethodHandler(
            final ComponentInstance deletableAccessorComponent,
            final RequestMappingInstance deletableAccessor,
            final SequencedMethodResult deletableResult) {
        this.deletableAccessorComponent = deletableAccessorComponent;
        this.deletableAccessor = deletableAccessor;
        this.deletableResult = deletableResult;
    }

    @Override
    public SequencedMethodResult handle(
            final WebRequest request, final HttpServletResponse response) {

        final FunctionDescriptor function = deletableAccessor.getFunction();
        final java.lang.reflect.Method lastMethod = function.getMethod();

        Object deletable = deletableResult.getValue();

        if (OptionLike.isOptionLike(deletable.getClass())) {
            deletable = OptionLike.getOrNull(deletable);
            if (deletable == null) {
                throw new ResourceNotFoundException("No such resource");
            }
        }

        if (!(deletable instanceof ComponentInterface)) {
            throw new HttpRequestMethodNotSupportedException(Method.DELETE);
        }

        final Deletable annotation = lastMethod.getAnnotation(Deletable.class);
        if (annotation == null) {
            throw new HttpRequestMethodNotSupportedException(Method.DELETE);
        }

        final AccessDecisionManager adm = AccessDecisionManagerFactory
            .getAccessDecisionManager(annotation.value());
        final SecurityContext securityContext =
                Current.getTypeSafe(SecurityContext.class);
        if (!adm.decide(securityContext)) {
            logger.fine("DELETE denied by method: '" +
                    ClassUtils.getClassAndMethodName(lastMethod));
            throw new AccessForbiddenException();
        }

        if (deletable instanceof DeletableEntity) {
            ((DeletableEntity) deletable).delete();
            final SequenceContext sequenceContext = deletableResult.getSequenceContext();
            final HttpResponseEntity<?> value = HttpResponseEntity.noContent();
            return new HttpResponseMethodResult(value, sequenceContext);
        } else {
            throw new IllegalStateException(
                    "Entity is not a DeletableEntity: " + deletable);
        }

    }
}
