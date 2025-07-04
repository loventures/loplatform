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

package loi.cp.web.handler;

import com.learningobjects.cpxp.component.web.WebRequest;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Method handler is used to handle all or part of an HTTP message. Method handlers
 * also decide what the next method scope is. This gives method handlers control
 * over what the next method handler will be or whether or not the HTTP message processing
 * is finished.
 */
public interface SequencedMethodHandler {

    SequencedMethodResult handle(final WebRequest request, final HttpServletResponse response);


}
