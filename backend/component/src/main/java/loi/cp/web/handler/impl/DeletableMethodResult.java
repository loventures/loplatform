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
import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.de.web.Deletable;
import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;

/**
 * A method result that decorates another method result with a scope that will trigger
 * {@link Deletable} processing on the value of the decorated result.
 */
public class DeletableMethodResult extends ForwardingSequencedMethodResult {

    private final SequencedMethodScope nextScope;

    /**
     * @param demotedResult the result of processing the HTTP message as a GET
     * @param component the component that received the method invocation that produced
     * {@code demotedResult}
     * @param method the method sent to {@code component} to produce {@code
     * demotedResult}
     */
    public DeletableMethodResult(
            final SequencedMethodResult demotedResult, final ComponentInstance component,
            final RequestMappingInstance method) {

        super(demotedResult);

        final DeletableMethodHandler handler =
                new DeletableMethodHandler(component, method, demotedResult);

        this.nextScope = new SingleMethodScope(handler);

    }

    @Override
    public SequencedMethodScope getNextScope() {
        return nextScope;
    }
}
