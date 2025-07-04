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
import com.learningobjects.cpxp.component.annotation.Init;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Evaluates(Init.class)
public class InitEvaluator extends AbstractEvaluator {
    private int _index;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _index = getAnnotation(Init.class).value();
    }

    @Override
    protected Object getValue(ComponentInstance instance, Object object) {
        Object[] args = instance.getArgs();
        return ((args == null) || (_index >= args.length)) ? null : args[_index];
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
