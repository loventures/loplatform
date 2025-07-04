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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AbstractComponentServlet extends AbstractComponent implements ServletComponent {
    @Infer
    private DelegateDescriptor _delegate;

    protected DelegateDescriptor getDelegateDescriptor() {
        return _delegate;
    }

    @Override
    public WebResponse service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        _delegate.checkAccess(null);
        Method method;
        try {
            method = Method.valueOf(request.getMethod());
        } catch (IllegalArgumentException ex) {
            unsupported(request, response);
            return NoResponse.instance();
        }
        switch (method) {
          case GET:
              get(request, response);
              break;
          case POST:
              post(request, response);
              break;
          case HEAD:
              head(request, response);
              break;
          case OPTIONS:
              options(request, response);
              break;
          case PUT:
              put(request, response);
              break;
          case DELETE:
              delete(request, response);
              break;
        }
        return NoResponse.instance();
    }

    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        unsupported(request, response);
    }

    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        unsupported(request, response);
    }

    public void head(HttpServletRequest request, HttpServletResponse response) throws Exception {
        get(request, response); // hrm
    }

    public void options(HttpServletRequest request, HttpServletResponse response) {
        unsupported(request, response);
    }

    public void put(HttpServletRequest request, HttpServletResponse response) {
        unsupported(request, response);
    }

    public void delete(HttpServletRequest request, HttpServletResponse response) {
        unsupported(request, response);
    }

    protected void unsupported(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException();
    }
}
