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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.internal.FunctionDescriptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultFunctionRegistry implements FunctionRegistry {
    private Map<String, FunctionDescriptor> _functions = new LinkedHashMap<>(); // for in-order iteration

    @Override
    public void register(FunctionDescriptor function) {
        String name = function.getMethod().getName();
        if (_functions.containsKey(name)) {
            // throw new RuntimeException("Duplicate function: " + function.getMethod());
            return; // first wins
        }
        _functions.put(name, function);
    }

    @Override
    public FunctionDescriptor lookup(Object... keys) {
        return (keys.length == 1) ? _functions.get(keys[0]) : null;
    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {
        return _functions.values();
    }
}
