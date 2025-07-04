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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Data structure to piece together a sequence of {@link Conjunction} instances
 * that should be OR'ed together as a larger disjunctive clause. Includes the
 * boolean algebra to pull shared terms out of the individual
 * {@link Conjunction} instances and add them as top level conditions to the
 * {@link QueryDescription}.
 */
class DisjunctionSeries implements Iterable<List<Conjunction>> {
    private static final Logger logger = Logger.getLogger(DisjunctionSeries.class.getName());

    private final QueryDescription _description;

    private final List<List<Conjunction>> _disjunctions = new LinkedList<List<Conjunction>>();

    /**
     * @param description needed by {@link #preprocessDisjunctions()} to promote
     *                    individual conditions that appear in all of the
     *                    {@link Conjunction} instances
     */
    DisjunctionSeries(QueryDescription description) {
        _description = description;
    }

    DisjunctionSeries(QueryDescription description,
                      List<List<Condition>>... disjunctions) {
        this(description);
        Arrays.stream(disjunctions).forEach(this::addDisjunction);
    }

    /**
     * Handles the special case where conditions has one entry, otherwise
     * converts the {@link List} of {@link List} to {@link List} of
     * {@link Conjunction}.
     *
     * @param conditions consumes a {@link List} or {@link List} instances to be
     *                   compatible with {@link QueryDescription}'s public API
     */
    void addDisjunction(List<List<Condition>> conditions) {
        if (conditions.size() == 1) {
            for (Condition condition : conditions.get(0)) {
                _description.addCondition(condition);
            }

            return;
        }
        List<Conjunction> temp = new LinkedList<Conjunction>();
        for (List<Condition> conjunctiveConditions : conditions) {
            Conjunction conjunction = new Conjunction(_description);

            for (Condition condition : conjunctiveConditions) {
                if (condition.getDataType() != null) {
                    _description.addDataType(condition.getDataType());
                }
                conjunction.addCondition(condition);
            }
            temp.add(conjunction);
        }
        _disjunctions.add(temp);
    }

    /**
     * Trivial simplifications because empty conjunctions are bad sql.
     */
    void preprocessDisjunctions() {
        for (List<Conjunction> disjunction : _disjunctions) {
            logger.fine("Preprocess disjunction" + disjunction);
            // If any conjunction is empty then it means always true
            // so we can clear the disjunction
            if (isShortCircuit(disjunction)) {
                disjunction.clear();
            }
        }
    }


    // if any conjunction list is empty, then the processing can short circuit
    private boolean isShortCircuit(List<Conjunction> disjunction) {
        for (Conjunction conjunction: disjunction) {
            if (conjunction.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    int size() {
        return _disjunctions.size();
    }

    List<Conjunction> get(int index) {
        return _disjunctions.get(index);
    }

    @Override
    public Iterator<List<Conjunction>> iterator() {
        return _disjunctions.iterator();
    }

}
