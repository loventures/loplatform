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

import com.google.common.annotations.VisibleForTesting;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import scala.collection.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class BaseCondition implements Condition {
    private String _dataType;
    private Comparison _comparison;
    private String _string;
    private Long _number;
    private Date _time;
    private Boolean _boolean;
    private Long _item;
    private String _function;
    private Double _double;
    private QueryBuilder _query;
    private List _list;
    private List<PathElem> _field;
    private String _language;

    @Override
    public String getDataType() {
        return _dataType;
    }

    @Override
    public void setDataType(String dataType) {
        _dataType = dataType;
    }

    @Override
    public Comparison getComparison() {
        return _comparison;
    }

    @Override
    public void setComparison(Comparison comparison) {
        _comparison = comparison;
    }

    @Override
    public String getString() {
        return _string;
    }

    @Override
    public void setString(String string) {
        _string = string;
    }

    @Override
    public Long getNumber() {
        return _number;
    }

    @Override
    public void setNumber(Long number) {
        _number = number;
    }

    @Override
    public Double getDouble() {
        return _double;
    }

    @Override
    public void setDouble(Double d) {
        _double = d;
    }

    @Override
    public Date getTime() {
        return _time;
    }

    @Override
    public void setTime(Date time) {
        _time = time;
    }

    @Override
    public Boolean getBoolean() {
        return _boolean;
    }

    @Override
    public void setBoolean(Boolean bool) {
        _boolean = bool;
    }

    @Override
    public Long getItem() {
        return _item;
    }

    @Override
    public void setItem(Long item) {
        _item = item;
    }

    @Override
    public String getFunction() {
        return _function;
    }

    @Override
    public void setFunction(String function) {
        _function = function;
    }

    @Override
    public void setQuery(QueryBuilder query) {
        _query = query;
    }

    @Override
    public QueryBuilder getQuery() {
        return _query;
    }

    @Override
    public void setList(List list) {
        _list = list;
    }

    @Override
    public List getList() {
        return _list;
    }

    @Override
    public void setField(PathElem... field) {
        _field = Arrays.asList(field);
    }

    @Override
    public List<PathElem> getField() {
        return _field;
    }

    @Override
    public String getLanguage() {
        return _language;
    }

    @Override
    public void setLanguage(String language) {
        _language = language;
    }

    @Override
    public boolean equals(Object o) {
        /* a case class, a case class, my kingdom for a case class */
        if (o instanceof Condition) {
            Condition other = ((Condition) o);
            return new EqualsBuilder()
              .append(_boolean, other.getBoolean())
              .append(_comparison, other.getComparison())
              .append(_dataType, other.getDataType())
              .append(_double, other.getDouble())
              .append(_field, other.getField())
              .append(_function, other.getFunction())
              .append(_item, other.getItem())
              .append(_list, other.getList())
              .append(_number, other.getNumber())
              .append(_query, other.getQuery())
              .append(_string, other.getString())
              .append(_time, other.getTime())
              .append(_language, other.getLanguage())
              .build();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Condition(");
        sb.append("dataType: ").append(_dataType)
          .append(", comparison: ").append(_comparison);
        if (_function != null) {
            sb.append(", function: ").append(_function);
        }
        if (_boolean != null) {
            sb.append(", boolean: ").append(_boolean);
        }
        if (_double != null) {
            sb.append(", double: ").append(_double);
        }
        if (_field != null) {
            sb.append(", fields: [").append(StringUtils.join(_field, ", ")).append("]");
        }
        if (_item != null) {
            sb.append(", item: ").append(_item);
        }
        if (_list != null) {
            sb.append(", values: [").append(StringUtils.join(_list, ", ")).append("]");
        }
        if (_number != null) {
            sb.append(", number: ").append(_number);
        }
        if (_string != null) {
            sb.append(", string: ").append(_string);
        }
        if (_time != null) {
            sb.append(", time: ").append(_time);
        }
        if (_language != null) {
            sb.append(", language: ").append(_language);
        }
        if (_query != null) {
            sb.append(", value: <QueryBuilder>");
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
          _boolean, _comparison, _dataType, _double,
          _field, _function, _item, _list,
          _number, _query, _string, _time, _language);
    }

    @Override
    public BaseCondition copy() {
        try {
            return (BaseCondition) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    /**
     * Factory method that creates a fresh instance initialized with the
     * specified arguments. This overload accepts the String equivalent of a
     * member of {@link Comparison}.
     *
     * @param dataType
     *            the type of field against which this condition will be applied
     * @param cmp
     *            comparison operator to be used between the field and the
     *            specified value
     * @param value
     *            value to evaluate as part of the condition, may be null
     * @return a valid and usable condition
     */
    public static Condition getInstance(String dataType, String cmp,
                                                 Object value) {
        Comparison cmpEnum = Comparison.valueOf(cmp);

        assert cmpEnum != null : "Comparison, %1$s, is not recognized as a valid operator.";

        return getInstance(dataType, cmpEnum, value);
    }

    /**
     * Factory method that creates a fresh instance initialized with the
     * specified arguments. This overload accepts the String equivalent of a
     * member of {@link Comparison} and a function.
     *
     * @param dataType
     *            the type of field against which this condition will be applied
     * @param cmp
     *            comparison operator to be used between the field and the
     *            specified value
     * @param value
     *            value to evaluate as part of the condition, may be null
     * @param function
     *            the function to apply to the data. Note that this is not
     *            applied to the supplied value.
     *
     * @return a valid and usable condition
     */
    public static Condition getInstance(String dataType, String cmp,
                                                 Object value, String function) {
        Condition dto = getInstance(dataType, cmp, value);
        dto.setFunction(function);
        return dto;
    }

    /**
     * Factory method that creates a fresh instance initialized with the
     * specified arguments.
     *
     * @param dataType
     *            the type of field against which this condition will be applied
     * @param cmp
     *            comparison operator to be used between the field and the
     *            specified value
     * @param value
     *            value to evaluate as part of the condition, may be null
     * @return a valid and usable condition
     */
    public static Condition getSimpleInstance(String dataType, Comparison cmp,
                                        Object value) {
        assert dataType != null : "DataType must be valid.";
        assert cmp != null : "Comparison operator must be valid.";

        BaseCondition dto = new BaseCondition();
        dto.setDataType(dataType);
        dto.setComparison(cmp);

        switch (BaseOntology.getOntology().getDataFormat(dataType)) {
            case string:
                if (value instanceof Enum) {
                    value = ((Enum) value).name();
                }
                assert assertTypeOf(value, String.class) : typeOfFailureMessage(
                  String.class, value, dataType, cmp);

                dto.setString((String)value);
                break;
            case text:
                assert (value == null && (cmp.equals(Comparison.eq) || cmp.equals(Comparison.ne)))
                  || cmp.equals(Comparison.search) : textSearchConstraintViolatedFailureMessage(value, dataType, cmp);
                dto.setString((String) value);
                break;
            case path:
                assert assertTypeOf(value, String.class) : typeOfFailureMessage(
                  String.class, value, dataType, cmp);

                dto.setString((String) value);
                break;
            case json: // initially we'll only support string comparisons against json attributes
                assert assertTypeOf(value, String.class) : typeOfFailureMessage(
                  String.class, value, dataType, cmp);

                dto.setString((String) value);
                break;
            case tsvector:
                assert assertTypeOf(value, String.class) : typeOfFailureMessage(
                  String.class, value, dataType, cmp);

                dto.setString((String) value);
                break;
            case number:
                if (value instanceof Enum) {
                    value = (long) ((Enum) value).ordinal();
                } else if (value instanceof Number) {
                    value = ((Number) value).longValue();
                }
                assert assertTypeOf(value, Long.class) : typeOfFailureMessage(
                  Long.class, value, dataType, cmp);

                dto.setNumber((Long) value);
                break;
            case DOUBLE:
                assert assertTypeOf(value, Double.class) : typeOfFailureMessage(
                  Double.class, value, dataType, cmp);

                dto.setDouble((Double) value);
                break;
            case time:
                if (value instanceof Instant) {
                    value = Date.from((Instant) value);
                } else if (value instanceof LocalDateTime){
                    value = Date.from(((LocalDateTime)value).atZone(ZoneId.systemDefault()).toInstant());
                }
                assert assertTypeOf(value, Date.class) : typeOfFailureMessage(
                  Date.class, value, dataType, cmp);

                dto.setTime((Date) value);
                break;
            case bool:
                assert assertTypeOf(value, Boolean.class) : typeOfFailureMessage(
                  Boolean.class, value, dataType, cmp);

                dto.setBoolean((Boolean) value);
                break;
            case item:
                if (value instanceof Id) {
                    dto.setItem(((Id) value).getId());
                } else {
                    assert assertTypeOf(value, Long.class) : typeOfFailureMessage(
                      Long.class, value, dataType, cmp);

                    dto.setItem((Long) value);
                }
                break;
            case uuid:
                dto.setString(value.toString()); // String or UUID
                break;
            default:
                throw new IllegalArgumentException(
                  String.format("DataType %1$s is not supported for use as a Condition in Query Builder",dataType));
        }
        return dto;
    }

    public static Condition inQuery(QueryBuilder inQuery) {
        BaseCondition condition = new BaseCondition();
        condition.setComparison(Comparison.in);
        condition.setQuery(inQuery);
        return condition;
    }

    public static Condition inQuery(String dataType, QueryBuilder inQuery) {
        BaseCondition condition = new BaseCondition();
        condition.setDataType(dataType);
        condition.setComparison(Comparison.in);
        condition.setQuery(inQuery);
        // The null value wins us the item data projection
        // of is null/is not null
        return condition;
    }

    public static Condition inIterable(String dataType, Iterable<?> it) {
        return ofIterable(dataType, Comparison.in, it);
    }

    public static Condition notInIterable(String dataType, Iterable<?> it) {
        return ofIterable(dataType, Comparison.notIn, it);
    }

    public static Condition inIterable(String dataType, Seq<?> it) {
        return ofIterable(dataType, Comparison.in, CollectionConverters.asJava(it));
    }

    public static Condition notInIterable(String dataType, Seq<?> it) {
        return ofIterable(dataType, Comparison.notIn, CollectionConverters.asJava(it));
    }

    public static Condition ofIterable(String dataType, Comparison cmp, Iterable<?> it) {
        return ofIterable(dataType, cmp, it, DEFAULT_VALUE_FIXER);
    }

    public static Condition ofIterable(String dataType, Comparison cmp, Seq<?> it) {
        return ofIterable(dataType, cmp, CollectionConverters.asJava(it), DEFAULT_VALUE_FIXER);
    }

    @VisibleForTesting
    protected static Condition ofIterable(String dataType, Comparison cmp, Iterable<?> it, ValueFixer fixer) {
        BaseCondition condition = new BaseCondition();
        condition.setDataType(dataType);
        condition.setComparison(cmp);
        condition.setList(fixValues(dataType, it, fixer));
        return condition;
    }

    public static Condition notInQuery(QueryBuilder notInQuery) {
        BaseCondition condition = new BaseCondition();
        condition.setComparison(Comparison.notIn);
        condition.setQuery(notInQuery);
        return condition;
    }

    public static Condition notInQuery(String dataType, QueryBuilder notInQuery) {
        BaseCondition condition = new BaseCondition();
        condition.setDataType(dataType);
        condition.setComparison(Comparison.notIn);
        condition.setQuery(notInQuery);
        return condition;
    }

    public static Condition jsonInstance(String dataType, String field, Comparison cmp, String value) {
        BaseCondition condition = new BaseCondition();
        condition.setDataType(dataType);
        condition.setField(PathElem.apply(field));
        condition.setComparison(cmp);
        condition.setString(value);
        return condition;
    }

    public static Condition jsonInstance(String dataType, List<PathElem> fields, Comparison cmp, String value) {
        BaseCondition condition = new BaseCondition();
        condition.setDataType(dataType);
        condition._field = fields;
        condition.setComparison(cmp);
        condition.setString(value);
        return condition;
    }

    public static Condition getInstance(String dataType, Comparison cmp,
                                                 Object value) {
        return getInstance(dataType, cmp, value, DEFAULT_VALUE_FIXER);
    }

    @VisibleForTesting
    static Condition getInstance(String dataType, Comparison cmp, Object value, ValueFixer valueFixer) {
        if (Comparison.in == cmp || Comparison.notIn == cmp) {
            BaseCondition condition = new BaseCondition();
            condition.setComparison(cmp);
            if (!DataTypes.META_DATA_TYPE_ITEM.equals(dataType)) {
                condition.setDataType(dataType);
            }
            if (value instanceof QueryBuilder) {
                condition.setQuery((QueryBuilder) value);
            } else {
                if (value instanceof scala.collection.Iterable) {
                    List<Object> list = new ArrayList<>();
                    for (scala.collection.Iterator<?> i = ((scala.collection.Iterable) value).iterator(); i.hasNext();) {
                        list.add(i.next());
                    }
                    value = list;
                } else if (!(value instanceof Iterable<?>)) {
                    value = Collections.singletonList(value);
                }

                condition.setList(fixValues(dataType, (Iterable<?>) value, valueFixer));
            }
            return condition;
        } else {
            return getSimpleInstance(dataType, cmp, value);
        }
    }

    public static Condition getInstance(String dataType, Comparison cmp, Object value, Function fn) {
        Condition dto = getInstance(dataType, cmp, value);
        if(!Function.NONE.equals(fn)){
            dto.setFunction(fn.toString());
        }
        return dto;
    }

    @SuppressWarnings("unchecked") // this is just known typeless
    private static List fixValues(String dataType, Iterable<?> values, ValueFixer fixer) {
        final DataFormat fmt = getDataFormat(dataType);
        List newlist = new java.util.ArrayList();
        for (Object obj : values) {
            newlist.add(fixer.fixValue(fmt, obj));
        }
        if ((fmt == DataFormat.item) || (fmt == DataFormat.number)) {
            Collections.sort((List<Long>) newlist); // cacheability
        }
        if (newlist.isEmpty()) { // TODO: FIXME: This is because hibernate generates invalid sql for in empty list. QueryBuilder should just generate and false but...
            // TODO: Almost certainly wrong. this is a temporary band aid.
            if (fmt == DataFormat.item) {
                newlist.add(-1L);
            } else {
                newlist.add(null);
            }
        }
        return newlist;
    }

    private static DataFormat getDataFormat(String dataType) {
        return dataType.startsWith("#") ? DataFormat.item : BaseOntology.getOntology().getDataFormat(dataType);
    }

    @VisibleForTesting
    static class ValueFixer {
        public Object fixValue(DataFormat fmt, Object value) {
            switch (fmt) {
                case string:
                    if (value instanceof Enum) {
                        value = ((Enum) value).name();
                    }
                    return "".equals(value) ? null : value;
                case time:
                    if (value instanceof Instant) {
                        value = Date.from((Instant) value);
                    }
                    return value;
                case path:
                case DOUBLE:
                case bool:
                case text:
                case json:
                case tsvector:
                    return value;
                case number:
                    if (value instanceof Enum) {
                        value = Long.valueOf(((Enum) value).ordinal());
                    }
                    return value;
                case item:
                    if (value instanceof Id) {
                        return ((Id) value).getId();
                    } else {
                        return ((Number) value).longValue();
                    }
            }
            return value;
        }
    }

    private static ValueFixer DEFAULT_VALUE_FIXER = new ValueFixer();

    private static boolean assertTypeOf(Object value, Class<?> typeOf) {
        return null == value || typeOf.isInstance(value);
    }

    private static String typeOfFailureMessage(Class<?> typeOf, Object value,
                                               String dataType, Comparison operator) {
        return String
          .format(
            "Value, %1$s, for \"%2$s\" comparison to DataType, %3$s, must be of type, %4$s, but is of type, %5$s.",
            value, operator, dataType, typeOf.getName(),
            null == value ? "Void" : value.getClass().getName());
    }

    private static String textSearchConstraintViolatedFailureMessage(Object value, String dataType, Comparison operator) {
        return String
          .format("DataType %1$s is a text field and does not permit comparison \"%2$s\" with value '%3$s'; only comparison to null and Comparison.search is permitted.",
            dataType, operator, value);
    }
}
