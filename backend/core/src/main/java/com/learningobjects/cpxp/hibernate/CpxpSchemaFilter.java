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

package com.learningobjects.cpxp.hibernate;

import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.DefaultSchemaFilterProvider;
import org.hibernate.tool.schema.spi.SchemaFilter;

public class CpxpSchemaFilter extends DefaultSchemaFilterProvider {
    @Override
    public SchemaFilter getMigrateFilter() {
        return INSTANCE;
    }

    private static final DefaultSchemaFilter INSTANCE = new DefaultSchemaFilter() {
        @Override
        public boolean includeTable(Table table) {
            return !table.getName().equals("AnalyticFinder");
        }
    };
}
