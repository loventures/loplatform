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

import argonaut.DecodeJson;
import argonaut.DecodeResult;
import argonaut.Json;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.util.lang.EnumLike;
import com.learningobjects.cpxp.util.lang.OptionLike;
import org.apache.commons.lang3.reflect.TypeUtils;
import scala.collection.Seq;
import scala.jdk.javaapi.CollectionConverters;
import scala.reflect.ClassTag;
import scala.util.Failure$;
import scala.util.Success$;
import scala.util.Try;
import scaloi.GetOrCreate;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.Instant;
import java.util.*;

/**
 * Handler for a facade method that gets a field.
 */
abstract class FacadeGetHandler implements FacadeMethodHandler {
    private final Method _method;
    private final String _dataType;
    private final boolean _isList, _isSeq, _isScalaList;
    private final boolean _isGeneric;
    private final boolean _isOptional;
    private final boolean _isTry;
    private final boolean _isItem;
    private final boolean _isFinder;
    private final boolean _isTypeVariable;
    private final boolean _isInstant;
    private final boolean _isPrimitiveBoolean;
    private final Class<?> _wrappingResultType, _resultType;
    private Class<? extends ComponentInterface> _componentInterface;
    protected Class<? extends Facade> _facadeClass;
    private final Class<?> _declaringClass;
    private final boolean _isEnum;
    private final boolean _isUUID;
    private final boolean _jsonNode, _argo, _decodeJson;
    private final JavaType _jsonType;
    private final boolean _isId;

    @SuppressWarnings("unchecked")
    protected FacadeGetHandler(DataFormat format, Method method,
                               FacadeData facadeData, String dataType) {
        _method = method;
        final Class<?>[] params = method.getParameterTypes();
        final int nParams = params.length;
        _declaringClass = method.getDeclaringClass();
        _dataType = dataType;
        _wrappingResultType = method.getReturnType();
        Type genericResultType = method.getGenericReturnType();
        Class<?> resultType = _wrappingResultType;
        // Is the facade exposing a list of data
        _isList = List.class.isAssignableFrom(resultType) && (genericResultType instanceof ParameterizedType);
        _isSeq = false;
        _isScalaList = false;
        _isOptional = OptionLike.isOptionLike(resultType);
        _isTry = Try.class.equals(resultType);
        _isPrimitiveBoolean = _wrappingResultType.equals(boolean.class);
        _isGeneric = _isOptional || _isTry || GetOrCreate.class.isAssignableFrom(resultType); // hack
        if (_isList || _isGeneric) {
            genericResultType = ((ParameterizedType) method.getGenericReturnType())
              .getActualTypeArguments()[0];
            resultType = (genericResultType instanceof Class) ? (Class<?>) genericResultType : Object.class;
        }
        // Handling item fields
        _isItem = (format == DataFormat.item);
        // Handling both a class parameter and getFoo[T: ClassTag]: T
        _isTypeVariable = (method.getGenericReturnType() instanceof TypeVariable)
            && (nParams == 1) && (Class.class.equals(params[0]) || ClassTag.class.equals(params[0]));
        if (_isItem) {
            // Either a Component, a Facade or a Long
            if (ComponentInterface.class.isAssignableFrom(resultType)) {
                _componentInterface = (Class<? extends ComponentInterface>) resultType;
            } else if (Facade.class.isAssignableFrom(resultType)) {
                _facadeClass = (Class<? extends Facade>) resultType;
            }
        }
        _isFinder = Finder.class.isAssignableFrom(resultType);
        // Handling enum results
        _isEnum = EnumLike.isEnumType(resultType);
        _isUUID = resultType == UUID.class;
        _isInstant = Instant.class.isAssignableFrom(resultType);
        _isId = Id.class.isAssignableFrom(resultType);
        boolean isJson = ((format == DataFormat.text) || (format == DataFormat.string)) && !_isEnum && !_isUUID && !String.class.equals(resultType);
        _jsonNode = (format == DataFormat.json);
        _jsonType =
          (isJson || (_jsonNode && !JsonNode.class.isAssignableFrom(resultType)))
            ? ComponentUtils.getObjectMapper().constructType(genericResultType)
            : null;
        _argo = (format == DataFormat.json) && argonaut.Json.class.isAssignableFrom(resultType);
        _decodeJson = (nParams == 1) && DecodeJson.class.isAssignableFrom(params[0]);
        if (_decodeJson) {
            final Type decType = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
            final Type valType = method.getGenericReturnType();
            final Type rawValType = TypeUtils.getRawType(valType, null);
            final boolean isWrappedResult = (
              (rawValType == DecodeResult.class || rawValType == scala.Option.class)
                && TypeUtils.equals(((ParameterizedType) valType).getActualTypeArguments()[0], decType)
            );
            if (!TypeUtils.equals(decType, valType) && !isWrappedResult) {
                throw new IllegalStateException("DecodeJson type does not match return type in method: " + method + ": " + TypeUtils.toString(decType) + " vs " + TypeUtils.toString(valType));
            }
        }
        _resultType = resultType;
    }

    @SuppressWarnings("unchecked")
    protected FacadeGetHandler(Method method, FacadeChild facadeChild, Class<? extends Facade> facade, FacadeQuery query) {
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _dataType = facadeChild.value(); // actually an item type..
        _wrappingResultType = method.getReturnType();
        Class<?> resultType = _wrappingResultType;
        // Is the facade exposing a list of data
        _isList = List.class.isAssignableFrom(resultType);
        _isSeq = Seq.class.isAssignableFrom(resultType);
        _isScalaList = scala.collection.immutable.List.class.isAssignableFrom(resultType);
        _isOptional = OptionLike.isOptionLike(resultType);
        _isTry = Try.class.equals(resultType);
        _isPrimitiveBoolean = _wrappingResultType.equals(boolean.class);
        _isGeneric = _isOptional || _isTry ||  GetOrCreate.class.isAssignableFrom(resultType); // hack
        if (_isList || _isSeq || _isGeneric) {
            Type subtype = ((ParameterizedType) method.getGenericReturnType())
              .getActualTypeArguments()[0];
            resultType = (subtype instanceof Class) ? (Class<?>) subtype : Object.class;
        }
        boolean itemProjection = (query == null) || "".equals(query.projection()) || DataFormat.item.equals(BaseOntology.getOntology().getDataFormat(query.projection()));
        _isItem = !method.getName().startsWith("count") && !method.getName().startsWith("query") && ((query == null) || (Function.NONE.equals(query.function()) && itemProjection));
        _isTypeVariable = false;
        _isEnum = false;
        _isUUID = false;
        _isInstant = false;
        // Either a ComponentInterface or a Facade or a Long
        if (ComponentInterface.class.isAssignableFrom(resultType)) {
            _componentInterface = (Class<? extends ComponentInterface>) resultType;
        } else if (Facade.class.isAssignableFrom(resultType)) {
            _facadeClass = (Class<? extends Facade>) resultType;
        }
        _isFinder = Finder.class.isAssignableFrom(resultType);
        _jsonNode = false;
        _jsonType = null;
        _resultType = resultType;
        _argo = false;
        _decodeJson = false;
        _isId = false;
    }

    @SuppressWarnings("unchecked")
    protected FacadeGetHandler(Method method, FacadeParent facadeParent) {
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _dataType = null;
        _wrappingResultType = method.getReturnType();
        Class<?> resultType = _wrappingResultType;
        _isList = false;
        _isSeq = false;
        _isScalaList = false;
        _isOptional = false;
        _isTry = false;
        _isGeneric = false;
        _isItem = true;
        _isTypeVariable = false;
        _isEnum = false;
        _isUUID = false;
        _isInstant = false;
        _isPrimitiveBoolean = false;
        // Either a ComponentInterface or a Facade or a Long
        if (ComponentInterface.class.isAssignableFrom(resultType)) {
            _componentInterface = (Class<? extends ComponentInterface>) resultType;
        } else if (Facade.class.isAssignableFrom(resultType)) {
            _facadeClass = (Class<? extends Facade>) resultType;
        }
        _isFinder = Finder.class.isAssignableFrom(resultType);
        _jsonNode = false;
        _jsonType = null;
        _resultType = resultType;
        _argo = false;
        _decodeJson = false;
        _isId = false;
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    protected String getDataType() {
        return _dataType;
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args)
            throws Throwable {
        if (_isTry) {
            try { // mm.
                return Success$.MODULE$.apply(invokeImpl(handler, args));
            } catch (Exception e) {
                return Failure$.MODULE$.apply(e);
            }
        } else {
            return invokeImpl(handler, args);
        }
    }

    private Object invokeImpl(FacadeInvocationHandler handler, Object[] args)
            throws Throwable {
        Object value;
        String methodName = _method.getName();
        if (handler.hasValue(methodName)) {
            value = handler.getValue(methodName);
            if (_isItem && value instanceof Long) {
                value = transformValue(handler.getContext().getItemService().get((Long)value), handler, args);
                handler.setValue(methodName, value, args);
            }
        } else if (handler.isDummy()) {
            value = _isList ? Collections.emptyList() : _isSeq ? scala.collection.immutable.Nil.empty() : null;
        } else if (_isList || _isSeq) {
            List<Object> list = new ArrayList<>();
            for (Object object : findValues(handler, args)) {
                if (_isItem && object instanceof Long) {
                    object = handler.getContext().getItemService().get((Long) object);
                }
                object = transformValue(object, handler, args);
                if (object != null) {
                    list.add(object);
                }
            }
            value = _isScalaList ? CollectionConverters.asScala(list).toList()
              : _isSeq ? CollectionConverters.asScala(list).toSeq()
              : list;
            handler.setValue(methodName, value, args);
        } else if (_isPrimitiveBoolean) {
            value = getValue(handler, args);
            if (value == null) {
                value = false;
            }
            handler.setValue(methodName, value, args);
        } else {
            value = transformValue(getValue(handler, args), handler, args);
            handler.setValue(methodName, value, args);
        }
        return value;
    }

    /**
     * Transform a value from the underlying data model to the type expected by
     * the facade. This handles mapping numbers and strings into enums, and
     * mapping items into ids and facades.
     */
    protected Object transformValue(Object value,
            FacadeInvocationHandler handler, Object[] args) throws Exception {
        if (value != null) {
            if (_isItem) {
                Item item = (Item) value;
                if (item.getDeleted() != null) {
                    value = null;
                } else if (_componentInterface != null) {
                    value = ComponentSupport.get(item, _componentInterface);
                } else if (_facadeClass != null) {
                    value = FacadeFactory.getFacade(_facadeClass, item, handler.getContext());
                } else if (_isFinder) {
                    value = item.getFinder();
                } else if (_isId) {
                    value = item;
                } else {
                    value = item.getId();
                }
            } else if (_isEnum) {
                scala.Option<?> vopt;
                if (value instanceof Long) {
                    vopt = EnumLike.fromIndex(((Long) value).intValue(), _resultType);
                } else {
                    vopt = EnumLike.fromName((String) value, _resultType);
                }
                if (vopt.isDefined()) {
                    value = vopt.get();
                } else {
                    throw new NoSuchElementException(
                      String.format("%s#%s: %s not member of %s",
                        _method.getDeclaringClass().getSimpleName(), _method.getName(), value, _wrappingResultType.getName()));
                }
            } else if (_isInstant) {
                value = ((Date)value).toInstant();
            } else if (_isTypeVariable) {
                JsonNode node = (JsonNode) value;
                if (args[0] instanceof ClassTag<?>) {
                    value = ComponentUtils.fromJson(node, ((ClassTag<?>) args[0]).runtimeClass());
                } else if (args[0] instanceof Class<?>) {
                    value = ComponentUtils.fromJson(node, (Class<?>) args[0]);
                } else {
                    throw new IllegalArgumentException(
                      String.format("%s#%s: %s %s does not name a type",
                        _method.getDeclaringClass().getSimpleName(), _method.getName(),
                        args[0].getClass().getSimpleName(), args[0]));
                }
            } else if (_argo) {
                //Do nothing parsing is handled by hibernate.
            } else if (_decodeJson) {
                DecodeJson decodeJson = (DecodeJson) args[0];
                if (_isOptional && ((Json) value).isNull()) {
                    value = null; // OptionLike.empty(_wrappingResultType);
                } else {
                    DecodeResult result = decodeJson.decodeJson((argonaut.Json) value);
                    if (result.isError()) {
                        throw new RuntimeException("Error decoding " + _method + ": " + result.toEither().left().get());
                    }
                    if (DecodeResult.class.equals(_resultType)) {
                        value = result;
                    } else {
                        value = result.fold((s, h) -> {
                            throw new RuntimeException("Error parsing " + _method + ": " + s);
                        }, o -> o);
                    }
                }
            } else if (_jsonType != null) {
                if (_jsonNode) {
                    value = ComponentUtils.fromJson((JsonNode) value, _jsonType);
                } else {
                    value = ComponentUtils.fromJson((String) value, _jsonType);
                }
            } else if (_isUUID) {
                value = UUID.fromString((String) value);
            }
        } else if (_isTry) {
            throw new NullPointerException("null try value");
        }
        if (_isOptional) {
            value = OptionLike.ofNullable(_wrappingResultType, value);
        }
        return value;
    }

    protected abstract Object getValue(FacadeInvocationHandler handler, Object[] args)
      ;

    protected abstract Collection<?> findValues(FacadeInvocationHandler handler, Object[] args)
      ;

    @Override
    public String toString() {
        return String.format("[dataType: %1$s, declaringClass: %2$s, method: %3$s]", _dataType, _declaringClass, _method.getName());
    }
}
