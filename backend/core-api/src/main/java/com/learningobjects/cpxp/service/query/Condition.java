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

import java.util.Date;
import java.util.List;

public interface Condition extends Cloneable {
    String getDataType();

    void setDataType(String dataType);

    Comparison getComparison();

    void setComparison(Comparison comparison);

    String getString();

    void setString(String string);

    Long getNumber();

    void setNumber(Long number);

    Double getDouble();

    void setDouble(Double d);

    Date getTime();

    void setTime(Date time);

    Boolean getBoolean();

    void setBoolean(Boolean bool);

    Long getItem();

    void setItem(Long item);

    String getFunction();

    void setFunction(String function);

    void setQuery(QueryBuilder query);

    QueryBuilder getQuery();

    void setList(List list);

    List getList();

    void setField(PathElem... field);

    List<PathElem> getField();

    void setLanguage(String language);

    String getLanguage();

    String LANGUAGE_SIMPLE = "simple"; // the simple language, no stemming or stop words

    Condition copy();
}
