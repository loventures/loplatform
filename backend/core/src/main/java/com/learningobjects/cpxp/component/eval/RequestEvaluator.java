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

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.annotation.Request;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Evaluates(Request.class)
public class RequestEvaluator extends AbstractEvaluator {
    private String _name;

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _name = StringUtils.defaultIfEmpty(getAnnotation(Request.class).name(), name);
    }

    @Override
    protected Object getValue(ComponentInstance instance, Object object) {
        return BaseWebContext.getContext().getRequest().getAttribute(_name);
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
