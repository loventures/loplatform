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

package com.learningobjects.cpxp.component.query;

import scala.Option;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiQueryResults<T> extends ArrayList<T> {
    private final Long _filterCount;
    private final Long _totalCount;

    private final boolean truncated;

    public static <T> ApiQueryResults<T> emptyResults() {
        return new ApiQueryResults<>(List.of(), 0L, 0L);
    }

    /**
     * Results packaged for return from an api query
     *
     * @param t           items to return; may represent a single page, thus be fewer than filterCount
     * @param filterCount number of items available with current filters applied
     * @param totalCount  number of items available in principle, if no filters were applied
     */
    public ApiQueryResults(Collection<T> t, @Nullable Long filterCount, @Nullable Long totalCount) {
        this(t, filterCount, totalCount, false);
    }

    /**
     * @param t           items to return; may represent a single page, thus be fewer than filterCount
     * @param filterCount number of items available with current filters applied
     * @param totalCount  number of items available in principle, if no filters were applied
     * @param truncated   results were truncated, for example by timeout, so counts may be incomplete
     */
    public ApiQueryResults(Collection<T> t, @Nullable Long filterCount, @Nullable Long totalCount, boolean truncated) {
        super(t);
        _filterCount = filterCount;
        _totalCount = totalCount;
        this.truncated = truncated;
    }

    @Nullable
    public Long getFilterCount() {
        return _filterCount;
    }

    @Nullable
    public Long getTotalCount() {
        return _totalCount;
    }

    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Returns the first element of the results, if present, or an error if there are many.
     */
    public Optional<T> toOptional() {
        switch (size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(get(0));
            default:
                throw QueryResultCountException.forResultCount(size());
        }
    }

    public Option<T> asOption() {
        return Option.apply(toOptional().orElse(null));
    }

    public <U> ApiQueryResults<U> map(Function<T, U> fn) { // I am sad and unhappy
        return new ApiQueryResults<>(
          stream().map(fn).collect(Collectors.toList()),
          _filterCount,
          _totalCount
        );
    }

    public <U> ApiQueryResults<U> flatMap(Function<T, Stream<U>> fn) { // I am sad and unhappy
        return new ApiQueryResults<>(
          stream().flatMap(fn).collect(Collectors.toList()),
          _filterCount,
          _totalCount
        );
    }
}
