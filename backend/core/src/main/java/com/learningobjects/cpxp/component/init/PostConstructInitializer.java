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

import com.learningobjects.cpxp.component.ComponentInstance;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class PostConstructInitializer implements Initializer {
    private final Method _method;

    public PostConstructInitializer(Method method) {
        _method = method;
        _method.setAccessible(true);
    }

    @Override
    public void initialize(ComponentInstance instance, Object object) throws Exception {
        _method.invoke(object);
    }

    @Override
    public Member getTarget() {
        return _method;
    }

    @Override
    public boolean isStateless() {
        return true;
    }
}
