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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.registry.ResourceRegistryContainer;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public interface ComponentDescriptor {
    Class<? extends ComponentInterface> getCategory();

    Class<?> getComponentClass();

    ComponentArchive getArchive();

    Component getComponentAnnotation();

    boolean isStateless();

    Set<Class<? extends ComponentInterface>> getInterfaces();

    @Nonnull
    DelegateDescriptor getDelegate();

    Collection<FunctionDescriptor> getFunctionDescriptors();

    ResourceRegistryContainer getResourceRegistryContainer();

    Map<String, ConfigurationDescriptor> getConfigurations();

    void loadMessages();

    void addMessage(String message);

    String getMessage(Locale locale, String key);

    Map<Locale, ? extends Map<String, String>> getAvailableMessages();

    Iterable<Annotation> getVirtualAnnotations();

    <T extends Annotation> T getAnnotation(Class<T> type);

    String getIdentifier();

    Iterable<String> getIdentifiers();

    String getVersion();

    URL getResource(String path);

    boolean isSupported(Class<? extends ComponentInterface> iface);

    @Deprecated // this implicitly summons an environment... prefer to pass it if you can
    ComponentInstance getInstance(IdType item, Long context, Object... args);

    /* I would very much like the `env` parameter to be implicit... */
    ComponentInstance getInstance(ComponentEnvironment env, IdType item, Long context, Object... args);

    // should be private, but has to be called by {@link DelegateDescriptor}
    void addFunction(FunctionDescriptor function);

    /**
     * Called by introspection code of delegates when a delegate expresses a resource to add to this
     * namespace/componentDescriptor.
     */
    void addResource(Annotation annotation, Class<?> clazz);

    void addConfiguration(Method member);

    boolean addInterface(Class<? extends ComponentInterface> iface, DelegateDescriptor delegate);
}
