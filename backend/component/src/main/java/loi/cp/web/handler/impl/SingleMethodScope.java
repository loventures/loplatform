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

import com.learningobjects.cpxp.component.web.WebRequest;
import loi.cp.web.handler.SequencedMethodHandler;
import loi.cp.web.handler.SequencedMethodScope;

/**
 * A scope that just has one handler.
 */
public class SingleMethodScope implements SequencedMethodScope {

    private final SequencedMethodHandler handler;

    public SingleMethodScope(final SequencedMethodHandler handler) {
        this.handler = handler;
    }

    @Override
    public SequencedMethodHandler findNextHandler(
            final WebRequest request) {
        return handler;
    }

    @Override
    public boolean hasNextHandler() {
        return true;
    }
}
