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

import argonaut.DecodeJson;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.service.data.DataFormat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * Handler for a facade method that gets an entity-mapped field.
 */
class EntityGetHandler extends FacadeGetHandler implements UserDefinedMethodHandler {
    private final EntityDescriptor _descriptor;
    private final Method _method;

    public EntityGetHandler(EntityDescriptor descriptor, DataFormat format, Method method, FacadeData facadeData, String dataType) {
        super(format, method, facadeData, dataType);
        _descriptor = descriptor;
        _method = method;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    protected Object getValue(FacadeInvocationHandler handler, Object[] args) {
        Object value = _descriptor.get(handler.getItem(), getDataType());
        if ((value == null) && (args != null) && (args.length == 1) && !(args[0] instanceof Class<?>) && !(args[0] instanceof DecodeJson)) {
            value = args[0];
        }
        return value;
    }

    @Override
    protected Collection<?> findValues(FacadeInvocationHandler handler, Object[] args) {
        return Collections.singleton(_descriptor.get(handler.getItem(), getDataType()));
    }
}
