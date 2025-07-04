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
import com.learningobjects.cpxp.component.web.DePathSegment;
import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;

import java.util.List;

/**
 * A method result that decorates another method result with expansion behavior.
 * Expansion behavior includes the addition of metadata such as count, totalCount,
 * offset, and limit. It also includes the addition of embedded entities.
 */
public class ExpandableMethodResult extends ForwardingSequencedMethodResult {

    private final SequencedMethodScope nextScope;

    /**
     * @param component the component instance that received the method invocation that
     * produced {@code target}
     * @param uriTemplate the URI template for the method invocation that was sent to
     * {@code component} to produce {@code target}
     * // TODO this is wrong, who says any URI was used to produce {@code target}?
     * @param pathSegments the path segments spent by the method invocation that was sent
 * to {@code component}
     * @param componentEnvironment
     */
    public ExpandableMethodResult(
      final SequencedMethodResult target, final ComponentInstance component,
      final String uriTemplate, final List<DePathSegment> pathSegments, ComponentEnvironment componentEnvironment) {

        super(target);

        final ExpandingMethodHandler handler =
                new ExpandingMethodHandler(target, component, uriTemplate,
                        pathSegments, componentEnvironment);
        this.nextScope = new SingleMethodScope(handler);
    }

    @Override
    public SequencedMethodScope getNextScope() {
        return nextScope;
    }

}
