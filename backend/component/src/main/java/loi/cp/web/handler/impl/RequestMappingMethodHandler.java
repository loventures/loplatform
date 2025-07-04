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
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.component.web.exception
        .HttpRequestMethodNotSupportedException;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.de.web.Deletable;
import loi.cp.web.HttpResponseEntity;
import loi.cp.web.handler.SequencedMethodResult;
import scalaz.$bslash$div;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Handles processing of a non-asynchronous  {@link RequestMapping}-annotated method.
 */
public class RequestMappingMethodHandler extends AbstractRequestMappingMethodHandler {

    private final ComponentEnvironment componentEnvironment;

    public RequestMappingMethodHandler(
      final ComponentInstance component, final RequestMappingInstance method,
      final List<DePathSegment> effectivePathSegments,
      final List<DePathSegment> pathSegments,
      final SequenceContext sequenceContext, final ActorSystem actorSystem, ComponentEnvironment componentEnvironment) {
        super(component, method, effectivePathSegments, pathSegments, sequenceContext,
                actorSystem);
        this.componentEnvironment = componentEnvironment;
    }

    @Override
    public Object handleForValue(
            final WebRequest request, final HttpServletResponse response) {


        final Object methodValue =
                method.invoke(request, response, effectivePathSegments);

        final Object handledValue;
        if (method.isVoid()) {
            if (method.isDirect()) {
                handledValue = HttpResponseEntity.noResponse();
            } else {
                handledValue = HttpResponseEntity.noContent();
            }
        } else {
            handledValue = methodValue;
        }

        return handledValue;
    }

    @Override
    protected SequencedMethodResult handleForResult(
            final WebRequest request, final HttpServletResponse response,
            final Object handledValue, final SequenceContext nextSequenceContext) {

        final FunctionDescriptor function = method.getFunction();
        final RequestMapping annotation = function.getAnnotation();
        final boolean noMorePath = sequenceContext.getSpentPathSegments().size() ==
                request.getUriPathSegments().size();
        final boolean demotedHttpMethod =
            (request.getMethod() == Method.DELETE && function.getMethod().isAnnotationPresent(Deletable.class));
        final boolean last =
          (noMorePath && (demotedHttpMethod || (request.getMethod() == annotation.method())))
            || annotation.terminal();
        final SequencedMethodResult result;
        if (last) {
            if (demotedHttpMethod) {
                final SequencedMethodResult demotedResult =
                        new DefaultMethodResult(handledValue, nextSequenceContext);
                switch (request.getMethod()) {
                    case DELETE:
                        result = new DeletableMethodResult(demotedResult, component,
                                method);
                        break;
                    default:
                        // impossiblee
                        throw new HttpRequestMethodNotSupportedException(
                                request.getMethod());
                }
            }else {
                final SequencedMethodResult unexpandedResult;

                //Test the result as for an error type.
                if (handledValue instanceof $bslash$div) {
                    $bslash$div eitherResult = ($bslash$div) handledValue;
                    if (eitherResult.isLeft()) {
                        Object left = eitherResult.swap().toOption().get();
                        HttpResponse httpResponse = (left instanceof HttpResponse) ? (HttpResponse) left : HttpResponseEntity.accepted(left);
                        return new HttpResponseMethodResult(httpResponse, nextSequenceContext);
                    }
                    Object right = eitherResult.toOption().get();
                    if (right == scala.runtime.BoxedUnit.UNIT) {
                        return new HttpResponseMethodResult(NoContentResponse.instance(), nextSequenceContext);
                    }
                    // expand the right
                    unexpandedResult = new DefaultMethodResult(right, nextSequenceContext);
                } else if (handledValue == scala.runtime.BoxedUnit.UNIT) {
                    return new HttpResponseMethodResult(NoContentResponse.instance(), nextSequenceContext);
                } else {
                    // we successfully completed routing the main request
                    unexpandedResult = new DefaultMethodResult(handledValue, nextSequenceContext);
                }

                // null response means this is lohtml embedded srs and should not expanded
                if (!(unexpandedResult.getValue() instanceof HttpResponse) && (response != null)) {
                    // TODO maybe condition should be if @RM isDirect()? instead
                    // of switching on the value's type
                    result = new ExpandableMethodResult(unexpandedResult, component,
                            annotation.path(), effectivePathSegments, componentEnvironment);
                } else {
                    result = unexpandedResult;
                }
            }
        } else {

            if (handledValue instanceof ComponentInterface) {

                // then prepare for search for next method in sequence to invoke

                final boolean addSyntheticClassLevelPathSegment =
                        annotation.method() == Method.Any;

                final ComponentInstance component =
                        ((ComponentInterface) handledValue).getComponentInstance();
                final ComponentFrameworkSequencedMethodScope nextScope =
                        new ComponentFrameworkSequencedMethodScope(nextSequenceContext,
                                component, actorSystem,
                                addSyntheticClassLevelPathSegment, componentEnvironment);

                result = new DefaultMethodResult(handledValue, nextSequenceContext,
                        nextScope);

            } else {
                // there is no place to look for @RequestMapping elements that could
                // finish processing the remaining request-URI path.
                throw new ResourceNotFoundException(
                        "No such resource: '" + request.getPath() + "'");
            }

        }

        return result;
    }
}
