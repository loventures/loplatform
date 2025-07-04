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

import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.function.FunctionBinding;
import loi.cp.appevent.AppEvent;
import loi.cp.appevent.OnEvent;
import loi.cp.appevent.facade.AppEventFacade;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@FunctionBinding(registry = OnEventRegistry.class, annotations = OnEvent.class)
public class OnEventInstance extends AbstractFunctionInstance {
    public Object invoke(AppEventFacade facade, AppEvent event) throws Exception {
        Class<?>[] types = _function.getMethod().getParameterTypes();
        Type[] genericTypes = _function.getMethod().getGenericParameterTypes();
        Annotation[][] annotations = _function.getMethod().getParameterAnnotations();
        Object[] args = new Object[types.length];
        for (int i = 0; i < args.length; ++i) {
            if (types[i].isInstance(event)) {
                args[i] = event;
            } else {
                // I should precompute and reuse the evaluators
                OnEventEvaluator evaluator = new OnEventEvaluator();
                evaluator.init(_function.getDelegate(), _function.getMethod().getName(), genericTypes[i], annotations[i]);
                args[i] = evaluator.eventParameter(facade);
            }
        }
        return super.invoke(getObject(), args);
    }
}
