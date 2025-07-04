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
import com.learningobjects.cpxp.component.annotation.Session;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;

@Evaluates(Session.class)
public class SessionEvaluator extends AbstractEvaluator {
    private String _name;
    // Function for depickling strings if needed.
    private Function<String, Object> _transformer = (x -> x);

    @Override
    public void init(DelegateDescriptor delegate, String name, Type type, Annotation[] annotations) {
        super.init(delegate, name, type, annotations);
        _name = StringUtils.defaultIfEmpty(getAnnotation(Session.class).name(), name);
        if (Long.class.equals(type)) {
            _transformer = NumberUtils::parseLong;
        } else if (Boolean.class.equals(type)) {
            _transformer = BooleanUtils::toBooleanObject;
        }
    }

    @Override
    protected Object getValue(ComponentInstance instance, Object object) {
        HttpSession session = BaseWebContext.getContext().getRequest().getSession(false);
        if (session != null) {
            Object value = session.getAttribute(_name);
            return (value instanceof String) ? _transformer.apply((String)value) : value;
        } else {
            return null;
        }
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
