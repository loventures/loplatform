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

package com.learningobjects.cpxp.service.query;

/**
 * Allows for multiple strategies to formulate the QL to express a specific
 * conjunction.
 *
 * @see JoinConjunctionBuilder
 * @see SubselectConjunctionBuilder
 * @see EntityConjunctionBuilder
 */
interface ConjunctionBuilder {
    /**
     * @param stub
     *            allows the caller to provide a partial suffix to which the
     *            position in the conjunction will be added to form the full
     *            suffix in the value placeholder so all values are uniquely
     *            named in a deterministic fashion
     * @param offset
     *            for implementations that operate on filtered portions of
     *            conditions within the conjunction, the value index needs to be
     *            adjusted by this amount
     * @param conjunction
     *            a sequence of conditions to be built as a single conjunctive
     *            QL clause
     * @param handler
     *            used to make determinations about the expected value type when
     *            binding, later
     * @return
     */
    String build(String stub, int offset, Conjunction conjunction,
            QueryParameterHandler handler);
}
