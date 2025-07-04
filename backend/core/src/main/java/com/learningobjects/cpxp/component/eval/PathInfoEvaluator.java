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

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.annotation.PathInfo;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

@Evaluates(PathInfo.class)
public class PathInfoEvaluator extends AbstractEvaluator {
    public static final String REQUEST_ATTRIBUTE_PATH_INFO = "ug:pathInfo";

    private int _index;

    @Override
    protected void init() {
        _index = getAnnotation(PathInfo.class).value();
    }

    @Override
    public Object decodeValue(ComponentInstance instance, Object object, HttpServletRequest request) {
        String pathInfo = (String) request.getAttribute(REQUEST_ATTRIBUTE_PATH_INFO);
        if (pathInfo == null) {
            throw new RuntimeException("Path info not available");
        }
        if (_index < 0) {
            return pathInfo;
        } else {
            String[] parts = StringUtils.split(pathInfo, '/'); // TODO: FIXME: Foo//Bar is evaluated as Foo Bar null, not Foo "" Bar
            return (_index < parts.length) ? parts[_index] : null;
        }
    }

    @Override
    public boolean isStateless() {
        return false;
    }
}
