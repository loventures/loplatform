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

import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataFormat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handler for a facade method that gets a data-mapped field.
 */
class DataGetHandler extends FacadeGetHandler implements UserDefinedMethodHandler {
    private final Method _method;

    public DataGetHandler(DataFormat format, Method method, FacadeData facadeData, String dataType) {
        super(format, method, facadeData, dataType);
        _method = method;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    protected Object getValue(FacadeInvocationHandler handler, Object[] args) {
        Object value = null;
        for (Data data : DataTransfer.findRawData(handler.getItem(), getDataType())) {
            value = data.getValue(BaseOntology.getOntology());
            if (value != null) {
                break;
            }
        }
        if ((value == null) && (args != null) && (args.length == 1)) {
            value = args[0];
        }
        return value;
    }

    @Override
    protected Collection<?> findValues(FacadeInvocationHandler handler, Object[] args) {
        List<Object> values = new ArrayList<>();
        for (Data data : DataTransfer.findRawData(handler.getItem(), getDataType())) {
            values.add(data.getValue(BaseOntology.getOntology()));
        }
        return values;
    }
}
