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

package com.learningobjects.cpxp.service.data;

import com.learningobjects.cpxp.dto.Ontology;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;

import java.time.Instant;
import java.util.Date;

public class DataUtil {
    /**
     * Set the value by examining the data format.
     *  @param data
     * @param value the value
     * @param ontology
     * @param itemService
     */
    public static void setValue(Data data, Object value, Ontology ontology, ItemService itemService) {
        switch (ontology.getDataFormat(data.getType())) {
            case string:
            case path:
            case tsvector:
                data.setString((String) value, ontology);
                break;
            case text:
                data.setText((String) value);
                break;
            case number:
                data.setNumber((Long) value);
                break;
            case DOUBLE:
                Double d = (value instanceof String) ? Double.parseDouble((String) value) : (Double) value;
                data.setString((d == null) ? null : d.toString(), ontology);
                break;
            case time:
                if (value instanceof Instant) {
                    data.setNumber(((Instant) value).toEpochMilli());
                } else {
                    data.setTime((Date) value);
                }
                break;
            case bool:
                data.setBoolean((Boolean) value);
                break;
            case item:
                if ((value != null) && !(value instanceof Item)) {
                    Long pk = (value instanceof Long) ? (Long) value : ((com.learningobjects.cpxp.Id) value).getId();
                    String type = ontology.getDataType(data.getType()).itemType();
                    value = type.isEmpty() ? itemService.get(pk) : itemService.get(pk, type);
                }
                data.setItem((Item) value);
                break;
            case json:
                data._json = value;
                break;
            case uuid:
                data.setString(value == null ? null : value.toString(), ontology);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Create a new data instance.
     *
     * @param type the data type
     *
     * @return the new data
     */
    public static Data getInstance(String type) {
        Data data = new Data();
        data.setType(type);
        return data;
    }

    /**
     * Create a new data instance.
     *
     * @param type the data type
     * @param value the data value
     *
     * @param ontology
     * @param itemService
     * @return the new data
     */
    public static Data getInstance(String type, Object value, Ontology ontology, ItemService itemService) {
        Data data = new Data();
        data.setType(type);
        setValue(data, value, ontology, itemService);
        return data;
    }
}
