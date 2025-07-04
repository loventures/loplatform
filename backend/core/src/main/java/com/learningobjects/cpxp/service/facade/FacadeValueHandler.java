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

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemWebService;
import com.learningobjects.cpxp.util.Ids;
import com.learningobjects.cpxp.util.lang.EnumLike;
import com.learningobjects.cpxp.util.lang.OptionLike;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handler for a facade method that deals with values.
 */
abstract class FacadeValueHandler implements FacadeMethodHandler {
    protected final Method _method;
    protected final Class<?> _declaringClass;
    protected final String _dataType;
    protected final boolean _isString;
    protected final boolean _isItem;
    protected final boolean _isId;
    protected final boolean _isItemString;
    protected final boolean _isEnum;
    protected final boolean _isOrdinal;
    protected final boolean _isUpload;
    protected final boolean _isOptional;
    protected final boolean _isInstant;
    protected final Class<?> _entityClass;
    protected final boolean _isUUID;
    protected final boolean _isJson;
    protected final boolean _jsonNode;
    protected final boolean _argo;
    protected final boolean _encodeJson;

    @SuppressWarnings("unchecked")
    protected FacadeValueHandler(DataFormat format, Method method, FacadeData facadeData, String dataType, boolean dataMapped) {
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _dataType = dataType;
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length < 1) {
            throw new IllegalArgumentException("Missing argument in " + _declaringClass.getName() + " " + method.getName());
        }
        Class<?> paramType = paramTypes[0];
        _isOptional = OptionLike.isOptionLike(paramType);
        if (_isOptional) {
            Type subtype = ((ParameterizedType) method.getGenericParameterTypes()[0])
              .getActualTypeArguments()[0];
            paramType = (subtype instanceof Class) ? (Class<?>) subtype : Object.class;
        }
        _isString = (format == DataFormat.string);
        // Handling item fields
        _isItem = (format == DataFormat.item);
        // Either a Facade or a Long or as string
        _isId = (_isItem && (Id.class.isAssignableFrom(paramType)));
        _isItemString = (_isItem && (String.class.isAssignableFrom(paramType)));
        // Handling enum fields
        _isEnum = EnumLike.isEnumType(paramType);
        _isUUID = paramType == UUID.class;
        _isInstant = Instant.class.isAssignableFrom(paramType);
        _isOrdinal = _isEnum && (format == DataFormat.number);
        _isUpload = UploadInfo.class.equals(paramType);
        DataTypedef typedef = BaseOntology.getOntology().getDataType(_dataType);
        _entityClass = (dataMapped || "".equals(typedef.itemType())) ? Item.class
            : BaseOntology.getOntology().getEntityDescriptor(typedef.itemType()).getEntityType();
        boolean isDataMappedCollection = dataMapped && (paramType.isArray() || List.class.isAssignableFrom(paramType));
        _isJson = ((format == DataFormat.text) || (format == DataFormat.string)) && !_isEnum && !_isUUID && !String.class.equals(paramType) && !isDataMappedCollection;
        _jsonNode = (format == DataFormat.json) && !JsonNode.class.isAssignableFrom(paramType);
        _argo = argonaut.Json.class.isAssignableFrom(paramType);
        _encodeJson = (paramTypes.length == 2) && argonaut.EncodeJson.class.isAssignableFrom(paramTypes[1]);
        if (_encodeJson) {
            final Type encType = ((ParameterizedType) method.getGenericParameterTypes()[1]).getActualTypeArguments()[0];
            final Type valType = method.getGenericParameterTypes()[0];
            if (!TypeUtils.equals(encType, valType)) {
                throw new IllegalStateException("EncodeJson type does not match value type in method: " + method + ": " + TypeUtils.toString(encType) + " vs " + TypeUtils.toString(valType));
            }
        }
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    /**
     * Transform a value from the facade type to the underlying data model.
     * This handles mapping enums into numbers and strings , and
     * mapping ids and facades into items.
     */
    protected Object transformValue(Object value,
            FacadeInvocationHandler handler, Object[] args) throws Exception {
        if (value != null) {
            if (_isOptional) {
                value = OptionLike.getOrNull(value);
            }
            if (_isItem) {
                if (_isUpload) {
                    // TODO: Destroy the old attachment..
                    UploadInfo upload = (UploadInfo) value;
                    value = (upload == UploadInfo.REMOVE) ? null
                        : handler.getContext().getService(AttachmentWebService.class).createAttachment(handler.getId(), upload);
                } else if (_isId) {
                    value = Ids.get((Id) value);
                } else if (_isItemString) {
                    value = handler.getContext().getService(ItemWebService.class).getById((String) value);
                }
                if (value != null) {
                    Long id = (Long) value;
                    value = handler.getContext().getEntityManager().find(_entityClass, id);
                    if(value == null) {
                        throw new NullPointerException("No value found for " + _entityClass.toString() + " for id " + id.toString());
                    }
                }
            } else if (_isEnum) {
                if (_isOrdinal) {
                    value = (long) EnumLike.toIndex(value);
                } else {
                    value = EnumLike.toName(value);
                }
            } else if (_argo) {
                //Do nothing, parsing is handled by hibernate
            } else if (_encodeJson) {
                final argonaut.EncodeJson encodeJson = (argonaut.EncodeJson) args[1];
                value = encodeJson.encode(value);
            } else if (_isJson) {
                value = ComponentUtils.toJson(value);
            } else if (_jsonNode) {
                value = ComponentUtils.toJsonNode(value);
            } else if (_isUUID) {
                value = value != null ? ((UUID) value).toString() : null;
            } else if (_isInstant) {
                value = value != null ? Date.from((Instant) value) : null;
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return String.format("[dataType: %1$s, declaringClass: %2$s, method: %3$s]", _dataType, _declaringClass, _method.getName());
    }
}
