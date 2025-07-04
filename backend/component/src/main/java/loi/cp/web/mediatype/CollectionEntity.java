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

package loi.cp.web.mediatype;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An entity in the Difference Engine media type that has a collection of components and metadata describing the
 * collection.
 */
public class CollectionEntity implements DeEntity, WithComponents {


    private final Integer offset;

    private final Integer limit;

    private final Integer count;

    private final Long filterCount;

    private final Long totalCount;

    private final List<DeEntity> entities;

    private final Boolean truncated;

    public CollectionEntity(final Integer offset, final Integer limit, final Integer count, final Long filterCount, final Long totalCount,
                            final List<DeEntity> entities, final Boolean truncated) {
        this.offset = offset;
        this.limit = limit;
        this.count = count;
        this.filterCount = filterCount;
        this.totalCount = totalCount;
        this.entities = entities;
        this.truncated = truncated;
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getOffset() {
        return offset;
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getLimit() {
        return limit;
    }

    @JsonProperty
    public Integer getCount() {
        return count;
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getFilterCount() {
        return filterCount;
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long getTotalCount() {
        return totalCount;
    }

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isTruncated() {
        return truncated;
    }

    @JsonProperty("objects")
    public List<DeEntity> getEntities() {
        return entities;
    }

    @Override
    public List<ComponentEntity> getComponents() {
        return entities.stream().filter(ComponentEntity.class::isInstance).map(ComponentEntity.class::cast).collect(Collectors.toList());
    }
}
