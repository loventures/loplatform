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
import com.google.common.base.Joiner;
import com.learningobjects.cpxp.async.async.AsyncRouter;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.web.DePathSegment;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.async.async.AsyncResponse;
import loi.cp.web.HttpResponseEntity;
import loi.cp.web.WebApiAsyncOperation;
import loi.cp.web.handler.SequencedMethodResult;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles processing of an asynchronous {@link RequestMapping}-annotated method.
 */
public class AsyncRequestMappingMethodHandler extends AbstractRequestMappingMethodHandler {
    private static final Logger logger = Logger.getLogger(AsyncRequestMappingMethodHandler.class.getName());

    public AsyncRequestMappingMethodHandler(
            final ComponentInstance component, final RequestMappingInstance method,
            final List<DePathSegment> effectivePathSegments,
            final List<DePathSegment> pathSegments,
            final SequenceContext sequenceContext, final ActorSystem actorSystem) {
        super(component, method, effectivePathSegments, pathSegments, sequenceContext,
                actorSystem);
    }

    @Override
    public Object handleForValue(
            final WebRequest request, final HttpServletResponse response) {


        method.preAsync(request, response, effectivePathSegments);

        final Date now = new Date();

        /*
         * FIXME If this method handler is fulfilling an `any`-method (see
         * {@link RequestMapping#method()}, then `pathSegments` is the empty string and
         * the event name will be a useless '/'. I don't know what the event name should
         * be though, when the event is fulfilling an `any`-method
         */
        final String handledPath = '/' + Joiner.on('/').join(pathSegments);

        final WebApiAsyncOperation operation =
                new WebApiAsyncOperation(method, handledPath);
        final AsyncResponse asyncResult = new AsyncResponse("async",
                AsyncRouter.guid2channel(operation.guid()), now);

        // TODO: This should be upon commit.. Meaning the actorSelection tell should be in the HttpResponseEntitiy..
        // or more precisely it should be a new WebResult so that the ApiDispatcher commits and then tells it.
        if (actorSystem != null) {
            AsyncRouter.localActor().tell(operation, null);
            return HttpResponseEntity.accepted(asyncResult);
        } else {
            logger.log(Level.WARNING, "No actor system. Executing asynchronous task synchronously: {0}", request.getPath());
            return operation.perform();
        }
    }

    @Override
    protected SequencedMethodResult handleForResult(
            final WebRequest request, final HttpServletResponse response,
            final Object handledValue, final SequenceContext nextSequenceContext) {

        return new DefaultMethodResult(handledValue, nextSequenceContext);
    }
}
