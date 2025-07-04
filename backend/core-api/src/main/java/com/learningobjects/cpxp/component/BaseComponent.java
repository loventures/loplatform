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

/** A base class for non-proxied component implementations. */
public abstract class BaseComponent implements ComponentInterface {
    private final ComponentInstance componentInstance;

    protected BaseComponent(ComponentInstance componentInstance) {
        this.componentInstance = componentInstance;
    }

    @Override
    public ComponentInstance getComponentInstance() {
        return componentInstance;
    }

    protected ComponentDescriptor getComponentDescriptor() {
        return componentInstance.getComponent();
    }

    @Override
    public boolean isComponent(Class<? extends ComponentInterface> iface) {
        return componentInstance.getComponent().isSupported(iface);
    }

    @Override
    public <T extends ComponentInterface> T asComponent(Class<T> iface, Object... args) {
        return componentInstance.getInstance(iface, args);
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
        return "Component[" + componentInstance.getItem() + "/" + componentInstance.getIdentifier() + "]";
    }
}
