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

import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.de.web.MediaType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Creates a {@link WebRequest} from an {@link HttpServletRequest}.
 */
@Service
public interface WebRequestFactory {
    /**
     * Create a {@link WebRequest} from an {@link HttpServletRequest}.
     *
     * @param request the {@link HttpServletRequest}
     * @return a {@link WebRequest} for the given {@code request}.
     */
    public WebRequest create(HttpServletRequest request);

    /**
     * Create a {@link WebRequest} from a request path.
     *
     * @param method the request method
     * @param path the request path
     * @return a {@link WebRequest} for the given {@code path}.
     */
    public WebRequest create(Method method, String path);

    /**
     * Create a {@link WebRequest} from an embed path. The created {@link WebRequest} is a
     * pale shadow of one created from a {@link HttpServletRequest}, but it is good enough
     * for embed processing.
     *
     * @param embedPath the embed path
     * @param version the api version to use, should be one from main {@link WebRequest}.
     * @return a {@link WebRequest} for use by embedding processors for the given {@code
     * path}.
     */
    public WebRequest createForEmbed(final String embedPath, final int version, final WebRequest original);

    /**
     * Safely get accepted media types from a request. Only use this method in
     * exception handling situations when {@link #create(HttpServletRequest)} has
     * already failed.
     */
    public List<MediaType> getAcceptedMediaTypes(HttpServletRequest httpServletRequest);

}
