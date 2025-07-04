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
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Handler for a facade method that gets a data-mapped field.
 */
class DataSetHandler extends FacadeSetHandler implements UserDefinedMethodHandler {

    public DataSetHandler(DataFormat format, Method method, FacadeData facadeData, String dataType) {
        super(format, method, facadeData, dataType, true);
    }

    @Override
    protected boolean setValue(Object value, FacadeInvocationHandler handler) {
        boolean modifiedSome = false;
        Data[] datas = DataTransfer.findRawData(handler.getItem(), getDataType()).toArray(new Data[]{});
        Object[] newValues;
        if (List.class.isInstance(value)) {
            newValues = ((List)value).toArray(new Object[]{});
        } else if (value != null) {
            newValues = new Object[]{value};
        } else {
            newValues = new Object[0];
        }
        // TODO: Is this even right at all?????
        for (int i = 0; i < datas.length; i++) {
            // Yucko this should be done with sets.. As it stands, its 'better' behaviour is nondeterministic.
            if (i < newValues.length) {
                if (!ObjectUtils.equals(newValues[i], (newValues[i] == null) ? null : datas[i].getValue(BaseOntology.getOntology()))) {
                    DataUtil.setValue(datas[i], newValues[i], BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
                    modifiedSome = true;
                }
            } else {
                handler.getContext().getDataService().remove(datas[i]);
                modifiedSome = true;
            }
        }
        for (int i = datas.length; i < newValues.length; i++) {
            Data data = DataUtil.getInstance(getDataType(), newValues[i], BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
            handler.getContext().getDataService().copy(handler.getItem(), data);
            modifiedSome = true;
        }
        handler.removeAllValuesInHandlerGroup(getMethod());
        return modifiedSome;
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
