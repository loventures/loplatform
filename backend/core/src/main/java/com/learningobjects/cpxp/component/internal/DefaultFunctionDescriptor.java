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

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.component.annotation.Instrument;
import com.learningobjects.cpxp.component.annotation.Parameter;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.compiler.BytecodeEngineer;
import com.learningobjects.cpxp.component.eval.*;
import com.learningobjects.cpxp.component.function.Function;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import scala.compat.java8.OptionConverters;

import javax.ejb.Local;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultFunctionDescriptor extends AbstractDescriptor implements FunctionDescriptor{
    private final DelegateDescriptor _delegate;
    private final Method _method;
    private final Annotation _annotation;
    private final String _binding;
    private final Class<? extends Annotation> _annotationClass;
    private final boolean _static;
    private final List<Evaluator> _evaluators = new ArrayList<>();
    private final boolean _instrument;

    public DefaultFunctionDescriptor(DelegateDescriptor delegate, Method method,
            Annotation annotation) {
        _delegate = delegate;
        _method = method;
        _annotation = annotation;
        _annotationClass = annotation.annotationType();
        _static = Modifier.isStatic(method.getModifiers());
        try {
            Function fn = _annotationClass.getAnnotation(Function.class);
            String global = (fn == null) ? null : fn.global();
            _binding = StringUtils.isEmpty(global) ? "" : (String) _annotationClass.getMethod(global).invoke(annotation);
            introspect();
        } catch (final Exception ex) {
            throw Throwables.propagate(ex);
        }
        method.setAccessible(true);
        _instrument = method.isAnnotationPresent(Instrument.class);
    }

    @Override
    public boolean instrument() {
        return _instrument || _delegate.instrument();
    }

    @Override
    public DelegateDescriptor getDelegate() {
        return _delegate;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    public <T extends Annotation> T getAnnotation() {
        return (T) _annotation;
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass() {
        return _annotationClass;
    }

    @Override
    public String getBinding() {
        return _binding;
    }

    @Override
    public boolean isStatic() {
        return _static;
    }

    @Override
    public List<Evaluator> getEvaluators() {
        return _evaluators;
    }

    private void introspect() {
        super.introspectAccess(_delegate, _method);
        BytecodeEngineer.ParameterNames names = getParameterNames();
        Type[] types = _method.getGenericParameterTypes();
        Annotation[][] annotations = _method.getParameterAnnotations();
        for (int i = 0; i < types.length; ++i) {
            Type type = types[i];
            Class<?> clas = TypeUtils.getRawType(type, null);
            String parameterName = null;

            //converts each parameters annotations from jax-rs to LO annotations
            Annotation[] registryAnnotations = annotations[i];

            for (Annotation annotation : registryAnnotations) {

                if (annotation instanceof Parameter) {
                    parameterName = ((Parameter) annotation).name();
                    if (StringUtils.isEmpty(parameterName)) {
                        parameterName = names.getParameterName(_method, i);
                    }
                }
            }
            final Annotation evaluateAnn = Arrays.stream(registryAnnotations)
              .filter(ann -> ann.annotationType().getAnnotation(Evaluate.class) != null)
              .findFirst().orElse(null);
            Evaluator evaluator = null;
            if (evaluateAnn != null) {
                evaluator =
                  OptionConverters.<Evaluator>toJava(CpxpEvaluators.knownEvaluator(evaluateAnn.annotationType()))
                    .orElse(null);
            } else if ((clas != null) && (clas.isAnnotationPresent(Local.class) || clas.isAnnotationPresent(Service.class))) {
                evaluator = new InjectEvaluator();
            } else {
                evaluator = new InferEvaluator();
            }
            evaluator.init(_delegate, parameterName, type, registryAnnotations);
            _evaluators.add(evaluator);
        }
    }

    private BytecodeEngineer.ParameterNames getParameterNames() {
        try {
            return BytecodeEngineer.getParameterNames(_method.getDeclaringClass());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }


    public String toString() {
        return _delegate.getComponent().getIdentifier() + "#" + _method.getName();
    }
}
