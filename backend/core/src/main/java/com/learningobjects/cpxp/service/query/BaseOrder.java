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

public class BaseOrder implements Order {
    private final QueryBuilder _queryBuilder;
    private final String _dataType;
    private final String _jsonField;
    private final Function _function;
    private final Direction _direction;

    private final Order _coalesce;

    private BaseOrder(QueryBuilder queryBuilder, String dataType, String jsonField, Function function, Direction direction, Order coalesce) {
        _queryBuilder = queryBuilder;
        _dataType = dataType;
        _jsonField = jsonField;
        _function = function;
        _direction = direction;
        _coalesce = coalesce;
    }

    @Override
    public QueryBuilder getQuery() { // meh, but easy..
        return _queryBuilder;
    }

    @Override
    public String getType() {
        return _dataType;
    }

    @Override
    public Order getCoalesce() {
        return _coalesce;
    }

    @Override
    public String getJsonField() {
        return _jsonField;
    }

    @Override
    public Function getFunction() {
        return _function;
    }

    @Override
    public Direction getDirection() {
        return _direction;
    }

    public Order coalesceWith(QueryBuilder query, String dataType) {
        return new BaseOrder(_queryBuilder, _dataType, _jsonField, _function, _direction, new BaseOrder(query, dataType, null, null, null, null));
    }

    public static BaseOrder byAggregate(Function function, Direction direction) {
        return new BaseOrder(null, null, null, function, direction, null);
    }

    public static BaseOrder byData(String dataType, Direction direction) {
        return new BaseOrder(null, dataType, null, null, direction, null);
    }

    public static BaseOrder byData(String dataType, Function function, Direction direction) {
        return new BaseOrder(null, dataType, null, function, direction, null);
    }

    public static BaseOrder byJsonField(String dataType, String jsonField, Direction direction) {
        return new BaseOrder(null, dataType, jsonField, null, direction, null);
    }

    public static BaseOrder byJsonField(String dataType, String jsonField, Function function, Direction direction) {
        return new BaseOrder(null, dataType, jsonField, function, direction, null);
    }

    public static BaseOrder byChildQuery(QueryBuilder query, Direction direction) {
        return new BaseOrder(query, null, null, null, direction, null);
    }
}
