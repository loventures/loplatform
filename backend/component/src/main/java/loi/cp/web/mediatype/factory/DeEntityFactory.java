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

package loi.cp.web.mediatype.factory;

import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.web.WebRequest;
import loi.cp.web.mediatype.DeEntity;

@Service
public interface DeEntityFactory {

    /**
     * Create a {@link DeEntity} for {@code handlerResult} that has metadata but no expansions (i.e. no embeds and no
     * fetches) yet.
     *
     * @param handlerResult      the result of invoking the handler method targeted by {@code usedPath}, or to put it
     *                           another way, the final return object of the last handler in the handler chain invoked
     *                           by the entire request URI.
     * @param request            the {@link WebRequest}.
     * @return the {@link DeEntity} for {@code handlerResult}, with metadata but without any expansions (i.e. embeds
     * aren't embedded yet, fetches aren't fetched yet).
     */
    DeEntity create(Object handlerResult, WebRequest request);

    /**
     * Create a {@link DeEntity}  for {@code embedResult} that is designed to be embedded in another {@link DeEntity}.
     * @param embedResult the object to embed
     * @param request the {@link WebRequest}.
     * @return a {@link DeEntity} for the {@code embedResult}.
     */
    DeEntity createEmbedded(Object embedResult, WebRequest request);
}
