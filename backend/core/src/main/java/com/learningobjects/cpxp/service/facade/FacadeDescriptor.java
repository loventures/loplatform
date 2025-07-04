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

import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * A facade descriptor. Provides handlers mapped to each of the facade's public
 * methods.
 */
class FacadeDescriptor {
    private final Map<Method, FacadeMethodHandler> _classMethodHandlers = new HashMap<>();
    private final Map<String, FacadeMethodHandler> _methodHandlers = new HashMap<>();
    private final Map<String, FacadeChild> _facadeChildren = new HashMap<>();
    private final Map<String, FacadeData> _facadeDatas = new HashMap<>();
    private final Map<String, FacadeJson> _facadeJsons = new HashMap<>();
    private final Map<String, FacadeStorage> _facadeStorages = new HashMap<>();
    private final Map<String, FacadeParent> _facadeParents = new HashMap<>();
    private final Map<String, Class<? extends Facade>> _facadeFacades = new HashMap<>();
    private final Map<String, Class<? extends ComponentInterface>> _facadeComponents = new HashMap<>();
    private final Map<String, FacadeComponent> _facadeComponentsAnnotation = new HashMap<>();

    private final EntityDescriptor _descriptor;

    FacadeDescriptor(EntityDescriptor descriptor) {
        _descriptor = descriptor;
    }

    public EntityDescriptor getEntityDescriptor() {
        return _descriptor;
    }

    public void addHandler(FacadeMethodHandler handler) {
        addHandler(handler.getMethodName(), handler);
    }

    public void addHandler(String methodName, FacadeMethodHandler handler) {
        assert !_methodHandlers.containsKey(methodName) : String
                .format(
                        "Duplicate method, %1$s, from handler, %2$s; already provided by %3$s.",
                        methodName, handler.toString(), _methodHandlers.get(
                                methodName).toString());
        _methodHandlers.put(methodName, handler);
    }

    public void addHandler(Method method, FacadeMethodHandler handler) {
        assert !_classMethodHandlers.containsKey(method) : String
                .format(
                        "Duplicate method, %1$s.%2$s, from handler, %3$s; already provided by %4$s.",
                        method.getDeclaringClass(), method.getName(), handler
                                .toString(), _classMethodHandlers.get(method)
                                .toString());
        _classMethodHandlers.put(method, handler);
    }

    public FacadeMethodHandler getHandler(Method method) {
        FacadeMethodHandler handler = _classMethodHandlers.get(method);
        if (handler == null) {
            handler = _methodHandlers.get(method.getName());
        }
        return handler;
    }

    public List<FacadeMethodHandler> getMethodHandlersByGroupName(String groupName) {
        List<FacadeMethodHandler> methodHandlers = new ArrayList<>();

        if (StringUtils.isNotBlank(groupName)) {
            for (FacadeMethodHandler classMethodHandler : _classMethodHandlers.values()) {
                if (classMethodHandler instanceof UserDefinedMethodHandler) {
                    UserDefinedMethodHandler cachableMethodHandler = (UserDefinedMethodHandler) classMethodHandler;
                    if ((FacadeUtils.getNameForHandlerGroupWithThisMethod(cachableMethodHandler.getMethod())).equals(groupName)) {
                        methodHandlers.add(classMethodHandler);
                    }
                }
            }

            for (FacadeMethodHandler methodHandler : _methodHandlers.values()) {
                if (methodHandler instanceof UserDefinedMethodHandler) {
                    UserDefinedMethodHandler cachableMethodHandler = (UserDefinedMethodHandler) methodHandler;
                    if ((FacadeUtils.getNameForHandlerGroupWithThisMethod(cachableMethodHandler.getMethod())).equals(groupName)) {
                        methodHandlers.add(methodHandler);
                    }
                }
            }
        }

        return methodHandlers;
    }

    public void setFacadeChildForHandlerGroup(FacadeChild facadeChild, String groupName) {
        _facadeChildren.put(groupName, facadeChild);
    }

    public FacadeChild getFacadeChildForHandlerGroup(String groupName) {
        return _facadeChildren.get(groupName);
    }

    public void setFacadeDataForHandlerGroup(FacadeData facadeData, String groupName) {
        _facadeDatas.put(groupName, facadeData);
    }

    public FacadeData getFacadeDataForHandlerGroup(String groupName) {
        return _facadeDatas.get(groupName);
    }

    public void setFacadeJsonForHandlerGroup(FacadeJson facadeJson, String groupName) {
        _facadeJsons.put(groupName, facadeJson);
    }

    public FacadeJson getFacadeJsonForHandlerGroup(String groupName) {
        return _facadeJsons.get(groupName);
    }

    public void setFacadeStorageHandlerGroup(FacadeStorage facadeStorage, String groupName) {
        _facadeStorages.put(groupName, facadeStorage);
    }

    public FacadeStorage getFacadeStorageForHandlerGroup(String groupName) {
        return _facadeStorages.get(groupName);
    }

    public void setFacadeParentForHandlerGroup(FacadeParent facadeParent, String groupName) {
        _facadeParents.put(groupName, facadeParent);
    }

    public FacadeParent getFacadeParentForHandlerGroup(String groupNameName) {
        return _facadeParents.get(groupNameName);
    }

    public void setComponentInterfaceForHandlerGroup(Class<? extends ComponentInterface> iface, String groupName) {
        _facadeComponents.put(groupName, iface);
    }

    public Class<? extends Facade> getFacadeForHandlerGroup(String groupNameName) {
        return _facadeFacades.get(groupNameName);
    }

    public void setFacadeForHandlerGroup(Class<? extends Facade> iface, String groupName) {
        _facadeFacades.put(groupName, iface);
    }

    public Class<? extends ComponentInterface> getComponentInterfaceForHandlerGroup(String groupNameName) {
        return _facadeComponents.get(groupNameName);
    }

    public void setFacadeComponentForHandlerGroup(FacadeComponent facadeComponent, String groupName) {
        _facadeComponentsAnnotation.put(groupName, facadeComponent);
    }

    public FacadeComponent getFacadeComponentForHandlerGroup(String groupName) {
        return _facadeComponentsAnnotation.get(groupName);
    }
}
