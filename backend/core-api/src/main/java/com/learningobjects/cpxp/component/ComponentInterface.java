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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.learningobjects.cpxp.component.annotation.SchemaInclude;


@SchemaInclude(SchemaInclude.Feature.UNDERSCORE_TYPE)
@JsonAutoDetect(getterVisibility = Visibility.NONE, creatorVisibility   = Visibility.NONE,
                fieldVisibility  = Visibility.NONE, isGetterVisibility  = Visibility.NONE,
                setterVisibility = Visibility.NONE)
public interface ComponentInterface {
    ComponentInstance getComponentInstance();
    boolean isComponent(Class<? extends ComponentInterface> iface);
    <T extends ComponentInterface> T asComponent(Class<T> iface, Object... args);
}
