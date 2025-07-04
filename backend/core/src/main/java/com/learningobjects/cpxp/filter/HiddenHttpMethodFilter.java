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

import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.util.StringUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link Filter} that converts X-HTTP-Method-Override header into the HTTP method as retrieved via {@link
 * HttpServletRequest#getMethod()}.
 * <p/>
 */
public class HiddenHttpMethodFilter extends AbstractFilter {

    /**
     * method override header name
     */
    private static final String OVERRIDE_HEADER_NAME = "X-HTTP-Method-Override";

    public HiddenHttpMethodFilter() {
        super();
    }

    @Override
    protected void filterImpl(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
                              final FilterChain chain) throws Exception {


        final String methodOverride = httpRequest.getHeader(OVERRIDE_HEADER_NAME);
        final String method = httpRequest.getMethod();
        final HttpServletRequest wrapped;

        if (StringUtils.isNotEmpty(methodOverride) && Method.POST.matches(method)) {

            /* POSTs can be used to house an override to any other method safely, not true for other methods */
            wrapped = new HttpMethodRequestWrapper(httpRequest, methodOverride);

        } else {

            /* ignore override */
            wrapped = httpRequest;
        }

        chain.doFilter(wrapped, httpResponse);

    }

    private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

        private final String method;

        public HttpMethodRequestWrapper(final HttpServletRequest request, final String method) {
            super(request);
            this.method = method;
        }

        @Override
        public String getMethod() {
            return this.method;
        }
    }

}
