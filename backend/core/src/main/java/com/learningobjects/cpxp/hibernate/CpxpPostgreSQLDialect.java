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

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * This enhances the current version of the Postgres dialect.
 */
public class CpxpPostgreSQLDialect extends PostgreSQLDialect {
    public CpxpPostgreSQLDialect() {
        super();
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
        // the defaults from the database metadata don't work???
        builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.LOWER);
        return super.buildIdentifierHelper(builder, dbMetaData);
    }

    @Override
    public boolean dropConstraints() {
        // This prevents hibernate from failing at startup of the db tests when it tries to
        // drop non-existent constraints.
        return false;
    }
}
