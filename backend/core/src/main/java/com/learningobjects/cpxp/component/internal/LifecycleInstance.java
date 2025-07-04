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

package com.learningobjects.cpxp.component.internal;

import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.component.annotation.PostLoad;
import com.learningobjects.cpxp.component.annotation.PreShutdown;
import com.learningobjects.cpxp.component.annotation.PreUnload;
import com.learningobjects.cpxp.component.eval.Evaluator;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.function.DefaultFunctionRegistry;
import com.learningobjects.cpxp.component.function.FunctionBinding;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@FunctionBinding(
  registry = DefaultFunctionRegistry.class,
  annotations = { PostCreate.class, PostLoad.class, PreShutdown.class, PreUnload.class }
)
public class LifecycleInstance extends AbstractFunctionInstance {
    private static final Map<String, Object> NONE = Collections.emptyMap();

    public void invoke() {
        List<Evaluator> evaluators = _function.getEvaluators();
        int n = evaluators.size();
        Object[] args = new Object[n];
        Object object;
        DelegateDescriptor delegate = _function.getDelegate();
        if (delegate.isService()) {
            // Lifecycle on services need to lookup the singleton instance.
            // Fairly naïve lookup of the service class or interface but it should
            // fail exceptionally if wrong.
            object = _instance.getEnvironment().getSingletonCache().getRealService(delegate);
        } else {
            object = getObject();
        }
        for (int i = 0; i < n; ++i) {
            Evaluator evaluator = evaluators.get(i);
            args[i] = evaluator.getValue(_instance, object, NONE);
        }
        super.invoke(object, args);
    }
}
