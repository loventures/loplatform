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

package com.learningobjects.cpxp.entity;

import com.learningobjects.cpxp.dto.EntityDescriptor;

/**
 * Hints for adding indices to the tables that back generated entities.
 */
public enum IndexType {
    /**
     * Create a normal index.
     */
    NORMAL("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s (%4$s%3$s)"),
    /**
     * Create an index on the column using the lower case function, useful for
     * case insensitive text matching.
     */
    LCASE("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s (%4$sLOWER(%3$s))"),
    /**
     * Use the operator class to better support LIKE operations, incremental
     * character-by-character matching against the index.
     */
    LIKE("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s (%4$s%3$s VARCHAR_PATTERN_OPS)"),
    /**
     * Created an index that combines lower case for case insensitivity and the
     * operator class for incremental matching.
     */
    LCASELIKE("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s (%4$sLOWER(%3$s) VARCHAR_PATTERN_OPS)"),
    /**
     * Created an index using gin and the english dictionary for full text searches
     */
    FULL_TEXT("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s USING gin (to_tsvector('english', LOWER(%3$s)))"),
    /**
     * Created an index using gin and the simple (no stemming or stop words) for full text searches
     */
    FULL_TEXT_SIMPLE("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s USING gin (to_tsvector('simple', LOWER(%3$s)))"),
    /**
     * Created an index using gin and the english dictionary for full text searches
     */
    GIN("CREATE INDEX CONCURRENTLY IF NOT EXISTS %1$s ON %2$s USING gin (%3$s)"),
    /** No index. Suppresses item ref indexes. */
    NONE("");

    private static final String NAME_TEMPLATE = "%1$s_%2$s_%3$s_idx";

    private static final int MAX_POSTGRESQL_INDEX_NAME_LENGTH = 63;

    private final String _template;

    IndexType(String template) {
        _template = template;
    }

    /**
     * For a given table and property/column, generate the appropriate DDL.
     *
     * @param tableName from an {@link EntityDescriptor}
     * @param propertyName from an {@link EntityDescriptor}
     * @param parent whether to index by parent
     * @param deleted whether to index by non-deleted
     * @return statement ready to be executed
     */
    public String getStatement(String tableName, String propertyName, boolean parent, boolean deleted) {
        String indexName = String.format(NAME_TEMPLATE, tableName,
                propertyName, name().toLowerCase());
        if (indexName.length() > MAX_POSTGRESQL_INDEX_NAME_LENGTH) {
            indexName = indexName.substring(0, MAX_POSTGRESQL_INDEX_NAME_LENGTH);
        }

        // TODO: This method doesn't belong here. For DOMAIN entities this should look at root_id instead. Do that outside.
        return String.format(_template, indexName, tableName, propertyName, parent ? "parent_id, " : "").concat(deleted ? " WHERE del IS NULL" : "");
    }
}
