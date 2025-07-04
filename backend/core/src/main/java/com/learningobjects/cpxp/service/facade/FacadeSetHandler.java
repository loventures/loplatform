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

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.service.data.DataFormat;

import java.lang.reflect.Method;

/**
 * Handler for a facade method that sets a field.
 */
abstract class FacadeSetHandler extends FacadeValueHandler {
    protected FacadeSetHandler(DataFormat format, Method method, FacadeData facadeData, String dataType, boolean dataMapped) {
        super(format, method, facadeData, dataType, dataMapped);
    }

    protected String getDataType() {
        return _dataType;
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args)
            throws Throwable {
        Object v0 = args[0];
        Object value = transformValue(v0, handler, args);
        if (handler.isDummy()) {
            handler.setValue("g" + getMethodName().substring(1), v0); // hack
        } else {
            setValue(value, handler);
        }
        return null;
    }

    protected abstract boolean setValue(Object value, FacadeInvocationHandler handler)
      ;
}
