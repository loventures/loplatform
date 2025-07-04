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

package com.learningobjects.cpxp.component.registry;

import com.learningobjects.cpxp.component.RequestMappingInstance;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.function.DuplicateControllerNameException;
import com.learningobjects.cpxp.component.function.RequestMappingIndex;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A registry of all {@link RequestMapping} methods in the environment that can the first
 * {@link RequestMapping} method invoked in the chain of {@link RequestMapping} methods
 * that service a request.
 */
public class RootControllerRegistry implements Registry<Controller, DelegateDescriptor> {

    /**
     * Root request mapping paths to the components that host them
     */
    private final RequestMappingIndex index = new RequestMappingIndex();

    /**
     * Keys are all controller names in the environment. Values are the class names that
     * host the {@link Controller}.
     */
    private final Map<String, String> controllers = new HashMap<>();

    private final Map<String, Collection<DelegateDescriptor>> _toMap = new HashMap<>();

    private ComponentRegistry registry;

    @Override
    public void init(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Map<String, Collection<DelegateDescriptor>> toMap() {
        return Collections.unmodifiableMap(_toMap);
    }

    @Override
    public void register(final Controller controller, final DelegateDescriptor delegate) {
        final String controllerName = getControllerName(controller, delegate.getDelegateClass());
        final String alreadyBoundClassName = controllers.get(controllerName);
        final String hostClassName = delegate.getDelegateClass().getName();
        if (alreadyBoundClassName != null) {
            throw new DuplicateControllerNameException(controllerName,
                    alreadyBoundClassName, hostClassName);
        } else {
            controllers.put(controllerName, hostClassName);
            _toMap.put(controllerName,Collections.singleton(delegate));
        }

        if (controller.root()) {
            registry.getFunctions(delegate.getComponent(), RequestMappingInstance.class).forEach(function -> {
                final RequestMapping requestMapping = function.getAnnotation();
                index.put(requestMapping, function);
            });
        }

    }

    public static final String getControllerName(final Controller controller, final Class<?> delegate) {
        if (StringUtils.isNotEmpty(controller.value())) {
            return controller.value();
        } else {
            return StringUtils.toLowerCaseFirst(
              delegate.getSimpleName()
                .replaceAll("(Root(Api(Impl)?)?)|(WebController(Impl)?)", "")
            );
        }
    }

    /**
     * Return the first {@link RequestMapping} method to invoke for the request. Never
     * returns null.
     *
     * The lookup keys must be <ul> <li>{@code keys[0]} - {@link String} path, must be
     * nonnull</li> <li>{@code keys[1]} - {@link Method} HTTP method, must be nonnull</li>
     * <li>{@code keys[2]} - {@link String} schema name of the request, may be null</li>
     * </ul>
     *
     * @param keys the lookup keys
     * @return the first {@link RequestMapping} method to invoke for the request.
     * @see RequestMappingIndex#get(String, Method, String)
     */
    @Override
    public DelegateDescriptor lookup(final Object[] keys) {
        final String path = (String) keys[0];
        final Method method = (Method) keys[1];
        final String typeName = (String) keys[2];
        return index.get(path, method, typeName).getDelegate();
    }

    @Override
    public Iterable<DelegateDescriptor> lookupAll() {
        return index.values().stream().map(FunctionDescriptor::getDelegate).collect(Collectors.toList());
    }
}
