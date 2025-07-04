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

import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.web.DePathSegment;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.WebRequestFactory;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.util.StringUtils;
import loi.cp.web.HttpResponseEntity;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;
import loi.cp.web.mediatype.ComponentEntity;
import loi.cp.web.mediatype.DeEntity;
import loi.cp.web.mediatype.WithComponents;
import loi.cp.web.mediatype.factory.DeEntityFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Handles the expansion of another method result. Expansion of another method result
 * includes the addition of metadata such as count, totalCount, offset, and limit. It
 * also includes the addition of embedded entities.
 */
public class ExpandingMethodHandler implements SequencedMethodHandler {

    private final SequencedMethodResult unexpanded;
    private final ComponentInstance component;
    private final String uriTemplate;
    private final List<DePathSegment> pathSegments;
    private final ComponentEnvironment componentEnvironment;

    public ExpandingMethodHandler(
      final SequencedMethodResult unexpanded, final ComponentInstance component,
      final String uriTemplate, final List<DePathSegment> pathSegments, ComponentEnvironment componentEnvironment) {
        this.unexpanded = unexpanded;
        this.component = component;
        this.uriTemplate = uriTemplate;
        this.pathSegments = pathSegments;
        this.componentEnvironment = componentEnvironment;
    }

    @Override
    public SequencedMethodResult handle(
            final WebRequest request, final HttpServletResponse response) {

        final DeEntityFactory deEntityFactory = getDeEntityFactory();

        final Object target = unexpanded.getValue();
        final SequenceContext sequenceContext = unexpanded.getSequenceContext();
        final DeEntity entity;

        if (target instanceof DeEntity) {
            entity = (DeEntity) target;
        } else if (sequenceContext.isEmbedded()) {
            entity = deEntityFactory.createEmbedded(target, request);
        } else {
            entity = deEntityFactory.create(target, request);
        }

        final SequenceContext nextSequenceContext;

        if (!pathSegments.isEmpty()) {
            final String embedPath = pathSegments.get(pathSegments.size() - 1).getEmbedPath();

            if (sequenceContext.isEmbedded() || (embedPath == null)) {
                nextSequenceContext = sequenceContext;
            } else {
                nextSequenceContext = sequenceContext.with("+" + embedPath);
            }
            expand(request, response, entity, embedPath);

        } else {
            nextSequenceContext = sequenceContext;
        }

        final HttpResponseEntity<DeEntity> responseValue = HttpResponseEntity.okay(entity);
        return new HttpResponseMethodResult(responseValue, nextSequenceContext);
    }

    /**
     * Processes embed and fetch expansions for a Difference Engine media-type entity
     * (i.e. a component/list-of-component)
     *
     * @param request request from which method args can be assigned their values (the
     * method args of request mapping targeted by the expansion expressions).
     * @param entity entity being expanded
     * @param embedExpression expansion path
     * @return {@code entity} expanded.
     */
    private DeEntity expand(
            final WebRequest request, final HttpServletResponse response,
            final DeEntity entity, final String embedExpression) {

        List<String> embedPaths = parseRoutes(embedExpression);

        final WebRequestFactory webRequestFactory = getWebRequestFactory();

        for (final String embedPath : embedPaths) {

            final WebRequest embedRequest = webRequestFactory
                    .createForEmbed(embedPath, request.getVersion(), request);

            final String rest = StringUtils.substringAfter(embedPath, ".");


            for (final ComponentEntity deComponent : ((WithComponents) entity).getComponents()) {

                final ComponentInterface value = deComponent.getState();
                final ComponentInstance targetComponent =
                    value.getComponentInstance();

                final SequencedMethodScope scope = new ComponentFrameworkSequencedMethodScope(
                        SequenceContext.newEmbeddedInstance(), targetComponent, null,
                        false, componentEnvironment);

                final SequencedMethodResult embedResult =
                        continueSequence(scope, embedRequest, response);

                final Object embedded = embedResult.getValue();

                if (!(embedded instanceof HttpResponseEntity) ||
                        !(((HttpResponseEntity) embedded).get() instanceof DeEntity)) {
                    throw new ResourceNotFoundException(
                            "No such resource with given embed expression; '" +
                                    embedPath + "'");
                }

                final DeEntity subEntity =
                        (DeEntity) ((HttpResponseEntity) embedded).get();

                final String embedKeyword = embedRequest.getPath();
                deComponent.putEmbed(embedKeyword, subEntity);

            }
        }

        return entity;
    }

    private SequencedMethodResult continueSequence(
            final SequencedMethodScope scope, final WebRequest embedRequest,
            final HttpServletResponse response) {

        final SequencedMethodHandler methodHandler = scope.findNextHandler(embedRequest);

        final SequencedMethodResult methodResult =
                methodHandler.handle(embedRequest, response);

        final SequencedMethodScope nextScope = methodResult.getNextScope();
        if (nextScope.hasNextHandler()) {
            return continueSequence(nextScope, embedRequest, response);
        } else {
            return methodResult;
        }
    }

    private List<String> parseRoutes(final String embedValue) {
        return new EmbedRouteParser().parseRoutes(embedValue);
    }

    DeEntityFactory getDeEntityFactory() {
        return ComponentSupport.lookupService(DeEntityFactory.class);
    }

    WebRequestFactory getWebRequestFactory() {
        return ComponentSupport.lookupService(WebRequestFactory.class);
    }

}
