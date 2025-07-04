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

package com.learningobjects.cpxp.component.init;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.eval.Evaluator;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

public class FieldInitializer implements Initializer {
    private final Field _field;
    private final Evaluator _evaluator;

    public FieldInitializer(Field field, Evaluator evaluator) {
        _field = field;
        _evaluator = evaluator;
        field.setAccessible(true);
    }

    @Override
    public void initialize(ComponentInstance instance, Object object) throws Exception {
        Object value = _evaluator.decodeValue(instance, object, BaseWebContext.getContext().getRequest());
        if (value != null) {
            _field.set(object, value);
        }
    }

    @Override
    public Member getTarget() {
        return _field;
    }

    @Override
    public boolean isStateless() {
        return _evaluator.isStateless();
    }
}
