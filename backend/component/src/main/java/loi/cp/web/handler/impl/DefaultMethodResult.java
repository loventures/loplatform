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

import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;

/**
 * A method result for a handler that does not return a raw HTTP response. That is, the
 * handler probably returned a component.
 */
public class DefaultMethodResult implements SequencedMethodResult {

    private final Object value;

    private final SequenceContext sequenceContext;

    private final SequencedMethodScope nextScope;

    public DefaultMethodResult(
            final Object value, final SequenceContext sequenceContext) {
        this(value, sequenceContext, EmptySequencedMethodScope.INSTANCE);
    }

    public DefaultMethodResult(
            final Object value, final SequenceContext sequenceContext,
            final SequencedMethodScope nextScope) {
        this.value = value;
        this.sequenceContext = sequenceContext;
        this.nextScope = nextScope;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public SequenceContext getSequenceContext() {
        return sequenceContext;
    }

    @Override
    public SequencedMethodScope getNextScope() {
        return nextScope;
    }

}
