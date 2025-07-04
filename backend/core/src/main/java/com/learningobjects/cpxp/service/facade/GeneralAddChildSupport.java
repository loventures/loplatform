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

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

public class GeneralAddChildSupport {

    private final Method _method;
    private final String _itemType;
    private final String[] _orderType;

    public GeneralAddChildSupport(FacadeChild facadeChild, Method method, String itemType) {
        super();
        _method = method;
        _itemType = itemType;
        _orderType = facadeChild.orderType();
    }

    public <T extends Facade> Item addChild(final FacadeInvocationHandler handler, final BiConsumer<Item, T> init, final Class<T> facadeClass) {
        Item item = handler.getContext().getItemService().create(handler.getItem(), _itemType, (init == null) ? null : (i) ->
                init.accept(i, facadeClass != null ? FacadeFactory.getFacade(facadeClass, i, handler.getContext()) : null));

        if ((_orderType.length == 1) && (DataTypes.DATA_TYPE_INDEX.equals(_orderType[0]))) { // TODO: KILLME
            QueryBuilder qb = handler.getContext().queryBuilder();
            qb.setParent(handler.getItem());
            qb.setItemType(_itemType);
            long count = qb.getAggregateResult(Function.COUNT);
            handler.getContext().getDataService().createNumber(item, _orderType[0], count + 1); // meh: killme
        }

//         handler.getContext().getService(ItemWebService.class).evictFromCache(handler.getItem().getId());

        handler.removeAllValuesInHandlerGroup(_method);

        return item;
    }
}
