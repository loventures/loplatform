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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.component.annotation.Tag;
import com.learningobjects.cpxp.component.element.CustomTag;
import com.learningobjects.cpxp.component.eval.Evaluator;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.function.DefaultFunctionRegistry;
import com.learningobjects.cpxp.component.function.FunctionBinding;
import com.learningobjects.cpxp.component.util.DynamicHtml;
import com.learningobjects.cpxp.component.util.EmptyHtml;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.*;
import java.util.logging.Logger;

@FunctionBinding(
  registry = DefaultFunctionRegistry.class,
  annotations = Tag.class
)
public class TagInstance extends AbstractFunctionInstance {
    private static final Logger logger = Logger.getLogger(TagInstance.class.getName());

    private final Map<String, Object> _parameters = new HashMap<>();

    public void setParameter(String name, Object value) {
        // TODO: This is a hack. Is there a better parameter name to use than parameters?
        if ("parameters".equals(name) && (value instanceof Map)) {
            Map<String, Object> parameters = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (!_parameters.containsKey(entry.getKey())) {
                    _parameters.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            _parameters.put(name, value);
        }
    }

    public Object invoke() throws Exception {
        List<Evaluator> evaluators = _function.getEvaluators();
        int n = evaluators.size();
        Set<String> names = new HashSet<String>(_parameters.keySet());
        Object[] args = new Object[n];
        Object object = getObject();
        for (int i = 0; i < n; ++i) {
            Evaluator evaluator = evaluators.get(i);
            args[i] = evaluator.getValue(_instance, object, _parameters);
            if ("*".equals(evaluator.getParameterName())) {
                names.clear();
            } else {
                names.remove(evaluator.getParameterName());
            }
        }
        if (!names.isEmpty()) {
            throw new Exception("Invalid " + _function + " tag attribute(s): " + StringUtils.join(names, ", "));
        }
        Class<?> type = _function.getMethod().getReturnType();
        if (Html.class.isAssignableFrom(type)) {
            return (Html) super.invoke(object, args);
        } else if (Void.TYPE == type) {
            return DynamicHtml.apply(out -> super.invoke(object, args));
        } else if (CustomTag.class.isAssignableFrom(type)) {
            return (CustomTag) super.invoke(object, args);
        } else {
            logger.warning("Unknown tag result " + type);
            return EmptyHtml.instance();
        }
    }
}
