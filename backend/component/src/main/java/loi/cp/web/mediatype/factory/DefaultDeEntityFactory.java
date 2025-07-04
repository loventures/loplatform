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

import com.fasterxml.jackson.databind.JsonNode;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.query.ApiPage;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.WebRequest;
import loi.cp.web.mediatype.*;
import scala.jdk.javaapi.CollectionConverters;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultDeEntityFactory implements DeEntityFactory {

    @Override
    public DeEntity create(Object handlerResult, WebRequest request) {
        return create(handlerResult, request, false);

    }

    @Override
    public DeEntity createEmbedded(Object handlerResult, WebRequest request) {
        return create(handlerResult, request, true);
    }

    /**
     * Designated creator
     */
    private DeEntity create(Object handlerResult, WebRequest request, boolean embedded) {

        final DeEntity entity;

        // de-scala
        if (handlerResult instanceof Seq) {
            handlerResult = CollectionConverters.asJava((Seq) handlerResult);
        }

        if (handlerResult instanceof ComponentInterface) {
            entity = createComponentEntity((ComponentInterface) handlerResult);
        } else if (handlerResult instanceof Iterable<?> && !(handlerResult instanceof JsonNode)) {

            final Iterable<?> handlerResults = (Iterable<?>) handlerResult;

            if (embedded) {
                /* don't get metadata */
                entity = createSimpleCollectionEntity(handlerResults);
            } else {
                /* get metadata too */
                entity = createCollectionEntity(handlerResults, request);
            }

        } else {
            entity = createObjectEntity(handlerResult);
        }

        return entity;
    }


    private DeEntity createComponentEntity(final ComponentInterface component) {
        return new ComponentEntity(component);
    }

    private DeEntity createObjectEntity(final Object object) {
        return new ObjectEntity(object);
    }

    // accounts for different forms of the WebRequest when queries/pages are present
    private Optional<ApiPage> getPage(WebRequest request) {
        if (request.getPage().isPresent()) {
            return request.getPage();
        } else if (request.getQuery().isPresent()) {
            if (request.getQuery().get().getPage().isSet()) {
                return Optional.of(request.getQuery().get().getPage());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

    }

    private DeEntity createCollectionEntity(final Iterable<?> entries, final WebRequest request) {

        final List<DeEntity> entities = toEntities(entries);

        final int count = entities.size();

        final Long totalCount;
        final Long filterCount;
        final Integer offset;
        final Integer limit;
        final Boolean truncated;

        final Optional<ApiPage> page = getPage(request);

        if (entries instanceof ApiQueryResults) {
            ApiQueryResults<?> filtered = (ApiQueryResults<?>) entries;
            totalCount = filtered.getTotalCount();
            filterCount = filtered.getFilterCount();
            if (page.isPresent()) { // TODO: Should I just always include page
                offset = page.get().getOffset();
                limit = page.get().getLimit();
            } else {
                offset = null;
                limit = null;
            }
            truncated = filtered.isTruncated() ? Boolean.TRUE : null;
        } else {
            totalCount = null;
            filterCount = null;
            offset = null;
            limit = null;
            truncated = null;
        }

        return new CollectionEntity(offset, limit, count, filterCount, totalCount, entities, truncated);
    }

    private DeEntity createSimpleCollectionEntity(final Iterable<?> handlerResults) {
        final List<DeEntity> entities = toEntities(handlerResults);
        return new SimpleCollectionEntity(entities);
    }

    /**
     * Converts an {@link Iterable} of objects to a heterogeneous list of {@link DeEntity}. Each {@link DeEntity} in
     * the
     * returned list is either a entity for a component (and its callbacks, like @Id,  are called) or a simple object
     * entity.
     *
     * @param handlerResults results from the last handler in the component chain
     * @return the results converted to specific implementations of {@link DeEntity} based on nature of each result.
     */
    private List<DeEntity> toEntities(final Iterable<?> handlerResults) {

        final List<DeEntity> entities = new ArrayList<>();

        for (final Object handlerResult : handlerResults) {

            if (handlerResult instanceof ComponentInterface) {
                // component
                entities.add(createComponentEntity((ComponentInterface) handlerResult));

            } else {
                // not a component
                entities.add(createObjectEntity(handlerResult));
            }
        }

        return Collections.unmodifiableList(entities);
    }
}
