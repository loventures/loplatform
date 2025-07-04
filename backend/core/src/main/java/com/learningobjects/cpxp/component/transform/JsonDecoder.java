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

package com.learningobjects.cpxp.component.transform;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningobjects.cpxp.component.util.ComponentUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collections;

public class JsonDecoder {
    private final ObjectMapper _mapper = ComponentUtils.getObjectMapper();

    private JavaType _type, _subtype;

    public JsonDecoder(Type type) {
        if (type != JsonNode.class) {
            _type = _mapper.getTypeFactory().constructType(type);
            if (_type.isContainerType() && !_type.isMapLikeType()) {
                _subtype = _type.containedType(0);
            }
        }
    }

    public Object decode(String value) throws IOException {
        if (value == null) {
            return null;
        } else if (_type == null) {
            return _mapper.readTree(value);
        } else if ((_subtype == null) || value.startsWith("[")) {
            return _mapper.readValue(value, _type);
        } else {
            Object element = _mapper.readValue(value, _subtype);
            if (_type.isArrayType()) {
                if (element == null) {
                    return Array.newInstance(_subtype.getRawClass(), 0);
                } else {
                    Object array = Array.newInstance(_subtype.getRawClass(), 1);
                    Array.set(array, 0, element);
                    return array;
                }
            } else {
                if (element == null) {
                    return Collections.emptyList();
                } else {
                    return Collections.singletonList(element);
                }
            }
        }
    }
}
