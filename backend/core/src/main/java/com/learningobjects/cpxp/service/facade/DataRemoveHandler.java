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
import com.learningobjects.cpxp.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Handler for a facade method that removes a field.
 */
class DataRemoveHandler extends FacadeValueHandler implements UserDefinedMethodHandler {
    public DataRemoveHandler(DataFormat format, Method method, FacadeData facadeData, String dataType) {
        super(format, method, facadeData, dataType, true);
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args)
            throws Throwable {
        Object value = transformValue(args[0], handler, args);
        if (!handler.isDummy()) {
            List valuesToRemove;
            if (List.class.isAssignableFrom(value.getClass())) {
                valuesToRemove = (List)value;
            } else {
                valuesToRemove = Collections.singletonList(value);
            }
            for (Data data : DataTransfer.findRawData(handler.getItem(), _dataType)) {
                for (Object singleValue : valuesToRemove) {
                    if (ObjectUtils.equals(singleValue, data.getValue(BaseOntology.getOntology()))) {
                        handler.getContext().getDataService().remove(data);
                    }
                }
            }
        }
        handler.removeAllValuesInHandlerGroup(getMethod());
        return null;
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
