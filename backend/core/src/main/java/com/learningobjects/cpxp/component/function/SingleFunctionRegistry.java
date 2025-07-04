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
import java.util.List;

public class SingleFunctionRegistry implements FunctionRegistry {

    private FunctionDescriptor function;

    @Override
    public void register(final FunctionDescriptor function) {

        if (this.function == null || this.function.getMethod().equals(function.getMethod())) {
            /*
             * If the java.lang.reflect.Method are equal we can overwrite. Certain inheritance trees can cause a
             * class to be introspected twice (e.g. A extends B, A implements C, B implements C will cause C to be
             * introspected twice upon introspection of A)
             */
            this.function = function;
        } else {
            throw new RuntimeException("Function field value already set; it cannot be set again");
        }

    }

    @Override
    public FunctionDescriptor lookup(final Object... keys) {
        return function;
    }

    @Override
    public Collection<FunctionDescriptor> lookupAll() {

        if (function == null) {
            return List.of();
        } else {
            return List.of(function);
        }
    }
}
