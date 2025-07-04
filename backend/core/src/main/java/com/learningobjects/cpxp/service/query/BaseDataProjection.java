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

package com.learningobjects.cpxp.service.query;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class BaseDataProjection implements DataProjection {
    private final String _dataType;
    private final String _jsonField;
    private final Function _function;

    public BaseDataProjection(String dataType, String jsonField, Function function) {
        _dataType = dataType;
        _jsonField = jsonField;
        _function = function;
    }

    @Override
    public String getType() {
        return _dataType;
    }

    @Override
    public String getJsonField() {
        return _jsonField;
    }

    @Override
    public Function getFunction() {
        return _function;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseDataProjection)) {
            return false;
        }
        BaseDataProjection rhs = (BaseDataProjection) other;
        return new EqualsBuilder()
          .append(_dataType, rhs._dataType)
          .append(_jsonField, rhs._jsonField)
          .append(_function, rhs._function)
          .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
          .append(_dataType)
          .append(_jsonField)
          .append(_function)
          .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, new DataProjectionToStringStyle())
          .append("dataType", _dataType)
          .append("jsonAttribute", _jsonField)
          .append("function", _function)
          .build();
    }

    public static DataProjection ofDatum(String dataType) {
        return new BaseDataProjection(dataType, null, null);
    }

    public static DataProjection[] ofData(String ...dataTypes) {
        DataProjection[] projections = new DataProjection[dataTypes.length];
        for(int i = 0; i < dataTypes.length; i++) {
            projections[i] = new BaseDataProjection(dataTypes[i], null, null);
        }
        return projections;
    }

    public static DataProjection ofAggregateData(String dataType, Function function) {
        return new BaseDataProjection(dataType, null, function);
    }

    public static DataProjection ofJsonAttribute(String dataType, String jsonAttribute) {
        return new BaseDataProjection(dataType, jsonAttribute, null);
    }

    private static final class DataProjectionToStringStyle extends ToStringStyle {
        DataProjectionToStringStyle(){
            super();
            this.setUseIdentityHashCode(false);
        }
    }
}
