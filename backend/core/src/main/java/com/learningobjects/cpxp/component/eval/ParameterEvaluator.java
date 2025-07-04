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

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.UserException;
import com.learningobjects.cpxp.component.annotation.Parameter;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.util.ConfigUtils;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.controller.upload.Uploads;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Evaluates(Parameter.class)
public class ParameterEvaluator extends JsonBodyEvaluator {
    private String _name;
    private Class<?> _decodeType;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, Map.class, type, annotations);
        Parameter parameter = getAnnotation(Parameter.class);
        _name = StringUtils.defaultIfEmpty(parameter.name(), name);
        if (_name == null) {
            throw new RuntimeException("Unnamed parameter in " + delegate.getDelegateClass().getName());
        }
        _decodeType = (_itemClass != null) ? Long.class : _subtype;
    }

    @Override
    public Object getValue(ComponentInstance instance, Object object, Map<String, Object> parameters) {
        final Object value = parameters.get(_name);
        if (_required && (value == null)) {
            throw new UserException("Required parameter " + _name + " not specified");
        } else if ((value != null) && (_subtype != null) && !_subtype.isInstance(value)) {
            if (Boolean.class.equals(_subtype)) {
                try {
                    return Boolean.parseBoolean((String) value);
                } catch (Exception ignored) {
                }
            } else if (Integer.class.equals(_subtype)) {
                try {
                    return Integer.parseInt((String) value);
                } catch (Exception ignored) {
                }
            } else if (Long.class.equals(_subtype)) {
                try {
                    return Long.parseLong((String) value);
                } catch (Exception ignored) {
                }
            } else if ((_rawType != null) && (_rawType.isInstance(value))) {
                return value; // This is so one tag can pass a generic collection to another
            } else if (Callable.class.equals(_subtype)) { // passing html to a callable tag parameter
                return new Callable<Void>() {
                    @Override
                    public Void call() {
                        BaseWebContext.getContext().getHtmlWriter().write(value);
                        return null;
                    }
                };
            }
            throw new RuntimeException("Invalid parameter " + _name + ": " + value.getClass().getName() + " is not a " + _subtype.getName());
        }
        return value;
    }

    @Override
    protected Object decodeValue(HttpServletRequest request) throws IOException {
        if (request == null) {
            return null;
        }
        var idempotent = request.getMethod().equals("GET");
        String[] values = null;
        if (isJson(request) && (request.getQueryString() == null)) {
            // really crude support for translating json object to parameters...
            Object jsonObject = super.decodeValue(request);
            if (jsonObject instanceof Map) {
                Map map = (Map) jsonObject;
                Object value = map.get(_name);
                if (value != null) {
                    if (value instanceof List) {
                        List a = (List) value;
                        values = new String[a.size()];
                        for (int i = 0; i < values.length; ++i) {
                            values[i] = String.valueOf(a.get(i));
                        }
                    } else {
                        values = new String[]{String.valueOf(value)};
                    }
                }
            } else if (jsonObject != null) {
                //The json was converted into a real object instead of a map. This only happens if this is called at the wrong point in the life-cycle
                throw new IllegalStateException("Called after the JSON has been converted to Objects, check for @Parameter annotation");
            }
        } else {
            values = request.getParameterValues(_name);
        }
        if (_required && ((values == null) || (values.length == 0))) {
            throw new UserException(InternationalizationUtils.formatMessage("validation_fieldNotEmpty", _name));
        }
        if (_array) {
            int n = (values == null) ? 0 : values.length;
            Object[] array = ConfigUtils.getArray(_subtype, n);
            for (int i = 0; i < n; ++i) {
                array[i] = decode(values[i], idempotent);
            }
            return array;
        } else if (_rawType != null) {
            Collection<Object> collection = ConfigUtils.getCollection(_rawType);
            if (values != null) {
                for (String value : values) {
                    collection.add(decode(value, idempotent));
                }
            }
            return collection;
        } else if ((values != null) && (values.length > 0)) {
            return decode(values[0], idempotent);
        } else {
            return null;
        }
    }

    private Object decode(String str, boolean idempotent) {
        str = str.trim();
        Object value;
        if (_decodeType == UploadInfo.class) {
            value = idempotent ? Uploads.retrieveUpload(str) : Uploads.consumeUpload(str); // gruesome.. registerable decoders would be cleaner..
        } else {
            value = ConfigUtils.decodeValue(str, _decodeType);
        }
        if (_itemClass != null) {
            value = getItem((Long) value, null);
        }
        return value;
    }

    @Override
    public String getParameterName() {
        return _name;
    }
}
