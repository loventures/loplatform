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

public class ForwardingComponent<T extends ComponentInterface> implements ComponentInterface {

    private final T value;

    public ForwardingComponent(final T value) {
        this.value = value;
    }

    protected T value() {
        return value;
    }

    @Override
    public ComponentInstance getComponentInstance() {
        return value.getComponentInstance();
    }

    @Override
    public boolean isComponent(
            final Class<? extends ComponentInterface> iface) {
        return value.isComponent(iface);
    }

    @Override
    public <S extends ComponentInterface> S asComponent(
            final Class<S> iface, final Object... args) {
        return value.asComponent(iface, args);
    }

}
