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

package com.learningobjects.cpxp.component.eval;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.annotation.Parameters;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;


@Evaluates(Parameters.class)
public class AllEvaluator extends AbstractEvaluator {
    // I don't use a static one because this mapper will also apply to the same type
    // so the caching isn't so useful, and this won't leak across redeploys.
    private final ObjectMapper _mapper = new ObjectMapper();

    private boolean _isMap;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _isMap = (_rawType != null) && Map.class.isAssignableFrom(_rawType);
    }

    @Override
    public Object getValue(ComponentInstance instance, Object object, Map<String, Object> parameters) {
        return parameters;
    }

    @Override
    public Object decodeValue(ComponentInstance instance, Object object, HttpServletRequest request) {
        if (_isMap) {
            return request.getParameterMap();
        } else {
            TreeMap<String, String[]> params = new TreeMap<>(request.getParameterMap());
            return decodeBean("", _mapper.getTypeFactory().constructType(_type), params);
        }
    }

    @Override
    public String getParameterName() {
        return "*";
    }

    // I iterate over the bean props instead of the parameters because that
    // is easier and fits more naturally with the jackson API.
    private Object decodeBean(String prefix, JavaType type, TreeMap<String, String[]> params) {
        String className = type.toCanonical();
        if (className.startsWith("java.lang")) {
            String[] paramArray = params.get(prefix);
            String param = (paramArray == null) ? null : paramArray[0];
            if (param == null) {
                return null;
            } else if (String.class.getName().equals(className)) {
                return param;
            } else if (Integer.class.getName().equals(className)) {
                return NumberUtils.parseInteger(param);
            } else if (Long.class.getName().equals(className)) {
                return NumberUtils.parseLong(param);
            } else if (Boolean.class.getName().equals(className)) {
                return Boolean.parseBoolean(param);
            } else {
                throw new RuntimeException("Unsupported primitive type: " + className);
            }
        } else if (type.isEnumType()) {
            String[] paramArray = params.get(prefix);
            String param = (paramArray == null) ? null : paramArray[0];
            return (param == null) ? null : Enum.valueOf((Class<? extends Enum>) type.getRawClass(), param);
        } else if (className.startsWith(Optional.class.getName())) { // meh canonical is foo.bar<Baz> // TODO: this is wrong
            JavaType subtype = type.containedType(0);
            Object value = decodeBean(prefix, subtype, params);
            return Optional.ofNullable(value);
        }

        if (StringUtils.isNotEmpty(prefix)) {
            prefix = prefix + '.';
        }
        String ceiling = params.ceilingKey(prefix);
        if ((ceiling == null) || !ceiling.startsWith(prefix)) {
            return null;
        }
        if (type.isContainerType()) { // only simple list supported right now
            JavaType subtype = type.getContentType();
            List<Object> list = new ArrayList<>();
            int index = 0;
            Object value;
            do {
                value = decodeBean(prefix + index, subtype, params);
                if (value != null) {
                    list.add(value);
                    ++ index;
                }
            } while (value != null);
            return list;
        }

        DeserializationConfig config = _mapper.getDeserializationConfig();
        BeanDescription description = config.introspect(type);

        AnnotatedConstructor ctor = description.findDefaultConstructor();
        if (ctor != null) {
            Object object = instantiate(ctor);
            for (BeanPropertyDefinition property : description.findProperties()) {
                AnnotatedMember accessor = property.getAccessor();
                String pname = property.getName();
                JavaType ptype = accessor.getType();
                Object value = decodeBean(prefix + pname, ptype, params);
                if (value != null) {
                    AnnotatedMember mutator = property.getMutator();
                    mutator.setValue(object, value);
                }
            }
            return object;
        } else {
            ctor = description.getConstructors().get(0);
            List<Object> parameters = new ArrayList<>();
            for (int i = 0; i < ctor.getParameterCount(); ++ i) {
                JsonProperty property = ctor.getParameterAnnotations(i).get(JsonProperty.class);
                String pname = property.value();
                JavaType ptype = ctor.getParameterType(i);
                parameters.add(decodeBean(prefix + pname, ptype, params));
            }
            return instantiate(ctor, parameters.toArray(new Object[0]));
        }
    }

    private Object instantiate(AnnotatedConstructor ctor, Object... args) {
        try {
            return ctor.call(args);
        } catch (Exception e) {
            throw new RuntimeException("While evaluating @Parameters on a " + _delegate, e);
        }
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
