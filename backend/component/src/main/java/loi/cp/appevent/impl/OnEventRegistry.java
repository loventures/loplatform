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

package loi.cp.appevent.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.learningobjects.cpxp.component.function.FunctionRegistry;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;

import loi.cp.appevent.AppEvent;

public class OnEventRegistry implements FunctionRegistry {
    private Map<Class<? extends AppEvent>, FunctionDescriptor> _functions = new LinkedHashMap<>();

    @Override
    public void register(FunctionDescriptor function) {
        Class<? extends AppEvent> eventType = null;
        for (Class<?> paramType : function.getMethod().getParameterTypes()) {
            if (AppEvent.class.isAssignableFrom(paramType)) {
                eventType = (Class<? extends AppEvent>) paramType;
                break;
            }
        }
        _functions.put(eventType, function);
    }

    @Override
    public FunctionDescriptor lookup(Object... keys) {
        return (keys.length == 1) ? _functions.get((Class<? extends AppEvent>) keys[0]) : null;
    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {
        return _functions.values();
    }
}
