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

package com.learningobjects.cpxp.component.web;

import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Instrument;
import com.learningobjects.cpxp.component.annotation.Required;
import com.learningobjects.cpxp.component.annotation.Stateless;
import com.learningobjects.cpxp.component.registry.Bound;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Bound(FilterBinding.class)
@Instrument
@Stateless
public interface FilterComponent extends ComponentInterface {
    /**
     * Return false if the request has processed, true if it should be continued. The
     * framework will then call invocation.proceed().
     */
    @Required
    boolean filter(HttpServletRequest request, HttpServletResponse response, FilterInvocation invocation) throws Exception;
}
