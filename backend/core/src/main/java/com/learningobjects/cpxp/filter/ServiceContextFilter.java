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

package com.learningobjects.cpxp.filter;

import com.learningobjects.cpxp.BaseWebContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Set up a service context so any class executed by this thread can
 * gain access to some basic, lower level services.
 */
public class ServiceContextFilter extends AbstractFilter {

    public ServiceContextFilter() {
        super();
    }

    @Override
    protected void filterImpl(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {
        try {
            BaseWebContext.getContext().init(httpRequest, httpResponse);
            chain.doFilter(httpRequest, httpResponse);
        } finally {
            BaseWebContext.getContext().clear();
        }
    }
}
