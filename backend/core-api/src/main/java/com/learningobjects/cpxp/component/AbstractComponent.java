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

import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;

public abstract class AbstractComponent implements ComponentInterface {
    @Infer
    private ComponentInstance componentInstance;

    @Infer
    private DelegateDescriptor delegateDescriptor;

    @Override
    public ComponentInstance getComponentInstance() {
        return componentInstance;
    }

    protected ComponentDescriptor getComponentDescriptor() {
        return (componentInstance == null) ? null : componentInstance.getComponent();
    }

    @Override
    public boolean isComponent(Class<? extends ComponentInterface> iface) {
        return (componentInstance != null) && getComponentDescriptor().isSupported(iface);
    }

    @Override
    public <T extends ComponentInterface> T asComponent(Class<T> iface, Object... args) {
        return (componentInstance == null) ? null : componentInstance.getInstance(iface, args);
    }

    @Override
    public int hashCode() {
        return (componentInstance == null) ? 0 : componentInstance.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (componentInstance == null) ? (o == this) :
                ((o instanceof ComponentInterface) && componentInstance.equals(((ComponentInterface) o).getComponentInstance()));
    }

    @Override
    public String toString() {
        return "Component[" +
                    componentInstance.getItem() + "/" +
                    componentInstance.getIdentifier() + "/" +
                    delegateDescriptor.getDelegateClass().getName() + "]";
    }
}

