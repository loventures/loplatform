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

import com.learningobjects.cpxp.component.eval.PathInfoEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

// This class is really totally redundant. Support for Renderable responses is now
// first class. render() is just a poor person's @Get.
public abstract class AbstractHtmlServlet extends AbstractComponentServlet {
    private static final Logger logger = Logger.getLogger(AbstractHtmlServlet.class.getName());

    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pathInfo = (String) request.getAttribute(PathInfoEvaluator.REQUEST_ATTRIBUTE_PATH_INFO);
        WebResponseOps.send(render(pathInfo), request, response);
    }

    public abstract WebResponse render(String path) throws Exception;

    @Override
    public void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AbstractRpcServlet.service(getDelegateDescriptor(), getComponentInstance(), request, response, logger);
    }
}
