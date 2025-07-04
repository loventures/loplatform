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

package com.learningobjects.cpxp.component.eval;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

public interface Evaluator {
    void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations);
    Object getValue(ComponentInstance instance, Object object, Map<String, Object> parameters);
    Object decodeValue(ComponentInstance instance, Object object, HttpServletRequest request);
    /** The name of the parameter consumed by this evaluator, "*" or null. */
    String getParameterName();
    Type getType();
    boolean isStateless();
}
