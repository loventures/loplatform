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
import com.learningobjects.cpxp.component.function.FunctionInstance;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ComponentInstance extends IdType {
    Object[] getArgs();

    ComponentDescriptor getComponent();

    ComponentEnvironment getEnvironment();

    Long getId();

    String getItemType();

    boolean getAcl();

    IdType getItem();

    Long getContext();

    String getIdentifier();

    String getVersion();

    String getName();

    String getDescription();

    String getIcon();

    // TODO: The context should be a Facade, not an Id.
    boolean checkContext();

    boolean isSupported(Class<? extends ComponentInterface> iface);

    <T extends ComponentInterface> T getInstance(Class<T> iface, Object... args);

    <T extends ComponentInterface> T getInstance(Class<T> iface, DelegateDescriptor delegate);
    /** This a version of newServiceInstance that accepts a method for caching a service instance before initialization.
     */
    Object newServiceInstance(DelegateDescriptor descriptor, boolean immediate, Consumer<Object> cache);

    <T extends FunctionInstance> Optional<T> getOptionalFunctionInstance(Class<T> type, Object... keys);

    <T extends FunctionInstance> T getFunctionInstance(Class<T> type, Object... keys);

    <T extends FunctionInstance> T getFunctionInstance(Class<T> type, FunctionDescriptor function);

    Object getInstance(DelegateDescriptor delegate);

    Object getInstance();

    Object eval(String value);

    String evalI18n(String value);
}
