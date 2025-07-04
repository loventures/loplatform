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

package com.learningobjects.cpxp.service.facade;

import com.learningobjects.cpxp.dto.FacadeParent;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * Handler for a facade method that gets a parent.
 */
class ParentGetHandler extends FacadeGetHandler {
    public ParentGetHandler(Method method, FacadeParent facadeParent) {
        super(method, facadeParent);
    }

    @Override
    protected Object getValue(FacadeInvocationHandler handler, Object[] args) {
        Object value = null;
        if (handler.getItem() != null) {
            value = handler.getItem().getParent();
        }
        return value;
    }

    @Override
    protected Collection<?> findValues(FacadeInvocationHandler handler, Object[] args) {
        return Collections.singleton(getValue(handler, args));
    }
}
