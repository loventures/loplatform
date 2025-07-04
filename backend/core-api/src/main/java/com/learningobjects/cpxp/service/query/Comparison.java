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

import java.util.Collection;
import java.util.Objects;

/**
 * A comparison operation.
 */
public enum Comparison {
    eq("="), ne("<>"), lt("<"), le("<="), gt(">"), ge(">="), like("LIKE"), in("IN"), notIn("NOT IN"), equalsAny("= ANY"), unequalAll("<> ALL"), search("@@"), intersects("&&");

    private String _qlOperator;

    Comparison(String qlOperator) {
        _qlOperator = qlOperator;
    }

    public String getQueryOperator() {
        return _qlOperator;
    }

    public boolean forcesNative() {
        return (this == search) || (this == equalsAny) || (this == unequalAll) || (this == intersects);
    }

    public boolean expectsArray() {
        return (this == equalsAny) || (this == unequalAll) || (this == intersects);
    }

    public boolean evaluate(Object left, Object right) {
        switch (this) {
            case eq:
                return Objects.equals(left, right);
            case ne:
                return !Objects.equals(left, right);
            case lt:
                return ((Number) left).doubleValue() < ((Number) right).doubleValue();
            case le:
                return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
            case gt:
                return ((Number) left).doubleValue() > ((Number) right).doubleValue();
            case ge:
                return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
            case in:
                return ((Collection<?>) right).contains(left);
            case notIn:
                return !((Collection<?>) right).contains(left);
            default:
                throw new RuntimeException("Evaluate comparison " + getQueryOperator());
        }
    }

}
