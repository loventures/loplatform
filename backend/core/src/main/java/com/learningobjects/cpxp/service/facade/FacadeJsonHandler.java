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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.FacadeJson;
import com.learningobjects.cpxp.dto.Ontology;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Handler for a facade method that deals with values.
 */
class FacadeJsonHandler implements FacadeMethodHandler, UserDefinedMethodHandler {
    protected final Method _method;
    protected final Class<?> _declaringClass;
    protected final JavaType _returnType;
    protected final String _property;
    protected final String _dataType;
    protected final boolean _isSet;
    protected final boolean _isRemove;
    protected final boolean _anyProp; // first param is prop name
    protected final boolean _anyType; // second param is type
    protected final ObjectMapper _mapper;
    protected final Ontology _ontology;

    @SuppressWarnings("unchecked")
    public FacadeJsonHandler(Method method, String property, FacadeJson facadeJson, Ontology ontology) {
        _ontology = ontology;
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _property = property;
        _dataType = facadeJson.value();
        _isSet = method.getName().startsWith("set");
        _isRemove = method.getName().startsWith("remove");
        int paramLength = method.getParameterTypes().length;
        _anyProp = _isSet ? paramLength == 2 : paramLength >= 1;
        _anyType = (!_isSet && !_isRemove) && method.getParameterTypes().length == 2;
        _mapper = JacksonUtils.getMapper();
        if (_isSet) {
            _returnType = null;
        } else {
            _returnType = _mapper.getTypeFactory().constructType(method.getGenericReturnType());
        }
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) throws Throwable {
        String property = _anyProp || _isRemove ? (String) args[0] : _property;
        if (handler.isDummy()) {
            if (_isSet) {
                Object value = args[_anyProp ? 1 : 0];
                handler.setValue(property, value, null);
            } else {
                return handler.getValue(property);
            }
        }
        JsonNode json = (JsonNode) DataTransfer.getDataValue(handler.getItem(), _dataType);
        if (json == null || json.isNull()) {
            json = _mapper.createObjectNode();
        }
        ObjectNode node = (ObjectNode) json;

        if (_isSet || _isRemove) {

            final String itemType = handler.getItem().getItemType();

            // Note that we currently do not support facade json on data tables.
            // This guards against the case where mutating the json data succeeds, but the changes are transient.
            if (!isFinderizedProperty(itemType)) {
                throw new UnsupportedOperationException("Expected data type "+_dataType+" on finder table for item "+itemType+", but missing.");
            }

            // Update
            if (_isSet) {
                //Also, see ComponentUtils.toJsonNode
                //this funnyness is because mapper.valueToTree uses the TokenBuffer implementation
                //  of JsonGenerator, which doesn't support the .writeRaw(String) method, which is used by JsonLiftModule
                //  JsonLiftModule could be changed to use supported methods, but it will be complicated.
                //  so for now, we just change to string first, then to tree
                //It also may be possible in the future to use JsonEncode if/when that is implemented as a
                // Jackson Module that gets configured with our default mapper
                Object value = args[_anyProp ? 1 : 0];
                String jsonValue = _mapper.writeValueAsString(value);
                node.set(property, _mapper.readTree(jsonValue));
                handler.getContext().getDataService().setType(handler.getItem(), _dataType, node);
            } else if (_isRemove) {
                node.remove(property);
            }

            saveProperty(handler, node);
            return null;
        } else {

            // Entire map
            if (!_anyProp && _returnType.getRawClass().isAssignableFrom(JsonNode.class)) {
                return node;
            }

            // Value from property
            else {
                JsonNode prop = node.get(property);
                if (prop == null) {
                    return null;
                }
                JavaType type = _anyType ? _mapper.constructType((Type) args[1]) : _returnType;
                return _mapper.reader(type).readValue(prop);

            }
        }
    }

    /**
     * Returns true if a property is on the finder table.
     */
    private boolean isFinderizedProperty(final String itemType) {
        final EntityDescriptor desc = _ontology.getEntityDescriptor(itemType);
        return desc.containsDataType(_dataType);
    }

    private void saveProperty(final FacadeInvocationHandler handler, final ObjectNode value) {
        handler.getContext().getDataService().setType(handler.getItem(), _dataType, value);
    }

    private boolean isEmpty(Object[] args) {
            return args == null || args.length == 0;
    }

    @Override
    public String toString() {
        return String.format("[property: %1$s, declaringClass: %2$s, method: %3$s]", _property, _declaringClass, _method.getName());
    }
}
