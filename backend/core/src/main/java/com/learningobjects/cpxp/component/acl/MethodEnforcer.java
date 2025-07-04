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

package com.learningobjects.cpxp.component.acl;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.FnInstance;
import com.learningobjects.cpxp.component.annotation.Enforce;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.util.StringUtils;

public class MethodEnforcer implements AccessEnforcer {
    @Infer
    private ComponentInstance _component;

    @Infer
    private Enforce _method;

    @Override
    public boolean checkAccess() {
        // lookup and evaluate the referenced method
        for (String method: _method.value()) {
            String methodRef = StringUtils.stripStart(method, "#");

            // Try looking up a @Fn annotated method (in the component or its delegates)
            try {
                FnInstance function = ComponentSupport.getFunction(FnInstance.class, _component, methodRef);
                if (function != null) {
                        return ComponentUtils.test(function.invoke());
                }
            } catch (Exception ex) {
                // ignore
            }

            // else fall back to unannotated functions within the component itself (i.e. not in delegates). This is legacy behavior.
            if (!ComponentUtils.test(_component.getComponent().getDelegate().invokeRef(methodRef, _component, _component.getInstance()))) {
                return false;
            }
        }

        return true;
    }
}
