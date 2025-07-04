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

import com.learningobjects.cpxp.component.web.HttpResponse;
import loi.cp.web.handler.SequencedMethodResult;
import loi.cp.web.handler.SequencedMethodScope;

/**
 * A method result for a handler that returns a raw HTTP response.
 */
public class HttpResponseMethodResult implements SequencedMethodResult {

    private final HttpResponse httpResponseValue;

    private final SequenceContext sequenceContext;

    public HttpResponseMethodResult(final HttpResponse httpResponseValue, final SequenceContext sequenceContext) {
        this.httpResponseValue = httpResponseValue;
        this.sequenceContext = sequenceContext;
    }

    @Override
    public Object getValue() {
        return httpResponseValue;
    }

    @Override
    public SequencedMethodScope getNextScope() {
        return EmptySequencedMethodScope.INSTANCE;
    }

    @Override
    public SequenceContext getSequenceContext() {
        return sequenceContext;
    }
}
