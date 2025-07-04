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

package loi.cp.bootstrap;

import com.learningobjects.cpxp.component.function.FunctionRegistry;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BootstrapFunctionRegistry implements FunctionRegistry {
    private final Map<String, FunctionDescriptor> _functions = new HashMap<>();

    @Override
    public void register(final FunctionDescriptor function) {
        String name = ((Bootstrap) function.getAnnotation()).value();
        _functions.put(name, function);
    }

    @Override
    public FunctionDescriptor lookup(final Object... keys) {
        return _functions.get(keys[0]);
    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {
        return _functions.values();
    }
}
