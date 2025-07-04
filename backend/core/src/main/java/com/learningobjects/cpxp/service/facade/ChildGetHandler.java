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

package com.learningobjects.cpxp.service.facade;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * Handler for a facade method that gets a child.
 */
class ChildGetHandler extends FacadeGetHandler implements UserDefinedMethodHandler {
    private final Method _method;
    protected final String _itemType;
    private final String[] _orderType;
    private final Function[] _orderFunction;
    private final Direction[] _direction;
    private final boolean _isStandalone;
    private final boolean _isDomain;
    private final boolean _byId;
    private final boolean _isQuery;
    private final Function _function;
    private final FacadeQuery _query;
    protected final FacadeCondition[] _conditions;
    private final int _offset;
    private final int _limit;

    public ChildGetHandler(Method method, FacadeChild facadeChild, Class<? extends Facade> facade, boolean byId, FacadeQuery query) {
        super(method, facadeChild, facade, query);
        _method = method;
        _itemType = FacadeUtils.getChildType(facadeChild, facade);
        _orderType = facadeChild.orderType();
        _orderFunction = facadeChild.orderFunction();
        _direction = facadeChild.direction();
        EntityDescriptor descriptor = BaseOntology.getOntology().getEntityDescriptor(_itemType);
        _isStandalone = (descriptor != null) && !descriptor.getItemRelation().isPeered();
        _isDomain = (descriptor != null) && (descriptor.getItemRelation() == ItemRelation.DOMAIN);
        _isQuery = method.getName().startsWith("query");
        _function = method.getName().startsWith("count") ? Function.COUNT : (query == null) ? Function.NONE : query.function();
        Annotation[][] annotations = method.getParameterAnnotations();
        _query = query;
        _conditions = new FacadeCondition[annotations.length];
        boolean hasCondition = false;
        int offset = -1, limit = -1;
        for (int i = 0 ; i < annotations.length; ++ i) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof FacadeCondition) {
                    _conditions[i] = (FacadeCondition) annotation;
                    hasCondition = true;
                } else if (annotation instanceof FacadeOffset) {
                    offset = i;
                } else if (annotation instanceof FacadeLimit) {
                    limit = i;
                }
            }
        }
        _byId = byId && !hasCondition;
        _offset = offset;
        _limit = limit;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    protected Object getValue(FacadeInvocationHandler handler, Object[] args) {
        Object value = null;
        if (_byId) {
            Item item;
            Long id = (Long) args[0];
            if (_isStandalone) {
                item = handler.getContext().getItemService().get(id, _itemType);
            } else {
                item = handler.getContext().getItemService().get(id);
            }
            if ((item != null) && !item.getParent().getId().equals(handler.getItem().getId())) {
                throw new IllegalArgumentException("Not a child: " + item + " / " + handler.getItem());
            }
            value = item;
        } else {
            QueryBuilder qb = queryBuilder(handler, args);
            if (_isQuery) {
                return qb;
            } else if (_function != Function.NONE) {
                qb.setFunction(_function);
                return qb.getResult();
            } else {
                qb.setLimit(1);
                return (Item) qb.getResult();
            }
        }
        return value;
    }

    @Override
    protected Collection<?> findValues(FacadeInvocationHandler handler, Object[] args) {
        if (_byId) {
            return Collections.singleton(getValue(handler, args));
        } else {
            return queryBuilder(handler, args).getItems();
        }
    }

    protected QueryBuilder queryBuilder(FacadeInvocationHandler handler, Object[] args) {
        QueryBuilder qb = handler.getContext().queryBuilder();
        if (!_isDomain && ((_query == null) || !_query.domain())) {
            qb.setParent(handler.getItem());
        } else {
            qb.setRoot(handler.getItem().getRoot());
        }
        qb.setItemType(_itemType);
        if (args != null) {
            for (int i = 0; i < args.length; ++ i) {
                Object arg = args[i];
                if (i == _offset) {
                    qb.setFirstResult(((Number) arg).intValue());
                } else if (i == _limit) {
                    qb.setLimit(((Number) arg).intValue());
                } else {
                    FacadeCondition condition = _conditions[i];
                    if (condition == null) {
                        continue;
                    }
                    if (arg instanceof Id) {
                        arg = ((Id) arg).getId();
                    }
                    Comparison cmp = condition.comparison();
                    if ((arg != null) || (cmp == Comparison.eq) || (cmp == Comparison.ne)) {
                        Condition cond = BaseCondition.getInstance(condition.value(), cmp, arg);
                        if (!Function.NONE.equals(condition.function())) {
                            cond.setFunction(condition.function().name());
                        }
                        qb.addCondition(cond);
                    }
                }
            }
        }
        if (_function == Function.NONE) {
            // TODO: Could have hacks to support non-data orders using META_DATA_TYPE
            for (int i = 0; i < _orderType.length; ++ i) {
                Direction orderDir = (_direction.length == 0) ? Direction.ASC : _direction[i];
                if (DataTypes.META_DATA_TYPE_ID.equals(_orderType[i])) {
                    qb.setOrder(Function.ID, orderDir);
                } else {
                    Function orderFn = ((_orderFunction.length == 0) || (_orderFunction[i] == Function.NONE)) ? null : _orderFunction[i];
                    qb.setOrder(_orderType[i], orderFn, orderDir);
                }
            }
        }
        if (_query != null) {
            if (!_query.cache()) {
                qb.setCacheQuery(false);
            }
            if (_query.debug()) {
                qb.setLogQuery(true);
            }
            if (StringUtils.isNotEmpty(_query.projection())) {
                qb.setDataProjection(_query.projection());
            }
            if (StringUtils.isNotEmpty(_query.orderType())) {
                Function orderFn = (_query.orderFunction() == Function.NONE) ? null : _query.orderFunction();
                qb.setOrder(_query.orderType(), orderFn, _query.orderDirection());
            }
        }
        return qb;
    }
}
