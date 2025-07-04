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

import com.learningobjects.cpxp.component.annotation.Direct;
import com.learningobjects.cpxp.component.annotation.Get;
import com.learningobjects.cpxp.component.annotation.Post;
import com.learningobjects.cpxp.component.annotation.Rpc;
import com.learningobjects.cpxp.component.eval.Evaluator;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.function.FunctionBinding;
import com.learningobjects.cpxp.component.function.RpcFunctionRegistry;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.util.Html;
import com.learningobjects.cpxp.component.web.WebResponse;
import jakarta.servlet.http.HttpServletRequest;
import loi.apm.Apm;

import java.util.List;

@FunctionBinding(
  registry = RpcFunctionRegistry.class,
  annotations = { Get.class, Post.class, Rpc.class }
)
public class RpcInstance extends AbstractFunctionInstance {
    private Rpc _rpc;
    private boolean _json;
    private boolean _web;
    private boolean _void;

    @Override
    public void init(ComponentInstance instance, FunctionDescriptor function) {
        super.init(instance, function);
        final Class<?> returnType = function.getMethod().getReturnType();
        if (function.getAnnotation() instanceof Rpc) {
            _rpc = function.getAnnotation();
        }
        if (Html.class.isAssignableFrom(returnType) || WebResponse.class.isAssignableFrom(returnType)) {
            _web = true;
        } else if (!function.getMethod().isAnnotationPresent(Direct.class)) {
            _json = true;
        }
        _void = _json && Void.TYPE.equals(function.getMethod().getReturnType());
   }

    public boolean isWebResponse() {
        return _web;
    }

    public boolean isJson() {
        return _json;
    }

    public boolean isVoid() {
        return _void;
    }

    public String getBinding() {
        return (_rpc == null) ? null : _rpc.binding();
    }

    public String getName() {
        return (_rpc == null) ? null : _rpc.name();
    }

    public String getFullName() {
        return _instance.getIdentifier() + "/" + _function.getMethod().getName();
    }

    public Object invoke(HttpServletRequest request) throws Exception {
        Apm.setTransactionName("rpc", getFullName());

        _function.getDelegate().checkAccess(_instance);
        _function.checkAccess(_instance);
        List<Evaluator> evaluators = _function.getEvaluators();
        int n = evaluators.size();
        Object[] args = new Object[n];
        Object object = getObject();
        for (int i = 0; i < n; ++i) {
            args[i] = evaluators.get(i).decodeValue(_instance, object, request);
        }
        return super.invoke(object, args);
    }
}
