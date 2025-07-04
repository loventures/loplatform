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

import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.service.data.DataFormat;

import java.lang.reflect.Method;

/**
 * Handler for a facade method that sets an entity-mapped field.
 */
class EntitySetHandler extends FacadeSetHandler implements UserDefinedMethodHandler {
    private final EntityDescriptor _descriptor;
    private final boolean _global;

    public EntitySetHandler(EntityDescriptor descriptor, DataFormat format, Method method, FacadeData facadeData, String dataType, boolean global) {
        super(format, method, facadeData, dataType, false);
        _descriptor = descriptor;
        _global = global;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    protected boolean setValue(Object value, FacadeInvocationHandler handler) {
        handler.attach();
        if (_global) {
            handler.getContext().getDataService().setType(handler.getItem(), getDataType(), value);
        } else {
            _descriptor.set(handler.getItem(), getDataType(), value);
        }
        handler.removeAllValuesInHandlerGroup(getMethod());
        return true;
    }
}
