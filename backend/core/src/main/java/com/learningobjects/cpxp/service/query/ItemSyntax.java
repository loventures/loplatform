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

class ItemSyntax {
    private final QueryDescription _description;

    ItemSyntax(QueryDescription description) {
        _description = description;
    }

    StringBuilder rootExpr(StringBuilder buffer) {
        return itemPropExpr(buffer, _description._itemLabel, "root");
    }

    StringBuilder parentExpr(StringBuilder buffer) {
        return itemPropExpr(buffer, _description._itemLabel, "parent", _description._parentLabel);
    }

    StringBuilder itemExpr(StringBuilder buffer) {
        return itemPropExpr(buffer, _description._itemLabel,  "id", _description._degenerateItemLabel);
    }

    StringBuilder notDeletedExpr(StringBuilder buffer) {
        buffer.append('(').append(_description._itemLabel).append('.');
        buffer.append(_description._entityDescription == null ? "deleted" : "del");
        buffer.append(" is null)");
        return buffer;
    }

    StringBuilder typeExpr(StringBuilder buffer) {
        buffer.append(_description._itemLabel);
        buffer.append(".type");
        if (_description.nativeQuery()) {
            buffer.append("_name");
        }
        buffer.append(" = :");
        buffer.append(_description._typeLabel);
        return buffer;
    }

    String itemProjection() {
        if (_description.nativeQuery()) {
            return _description._itemLabel.concat(".id");
        }
        return _description._itemLabel;
    }

    String idProjection() {
        if (_description.nativeQuery()) {
            return _description._itemLabel.concat(".id");
        }
        return _description._itemLabel + ".id";
    }

    String entityOrTable() {
        return _description.nativeQuery() ? _description._entityDescription.tableName()
                : _description._entityDescription.entityName();
    }

    private StringBuilder itemPropExpr(StringBuilder buffer, String itemLabel,
            String propertyLabel) {
        return itemPropExpr(buffer, itemLabel, propertyLabel, propertyLabel);
    }

    private StringBuilder itemPropExpr(StringBuilder buffer, String itemLabel,
            String propertyLabel, String argumentAlias) {
        buffer.append(itemLabel);
        this.itemLhs(buffer, propertyLabel);
        buffer.append(" = :").append(argumentAlias);
        return buffer;
    }

    private StringBuilder itemLhs(StringBuilder buffer, String propertyLabel) {
        buffer.append('.').append(propertyLabel);
        if (_description.nativeQuery() && !propertyLabel.equals(_description._degenerateItemLabel)) {
            buffer.append("_id");
        }
        return buffer;
    }
}
