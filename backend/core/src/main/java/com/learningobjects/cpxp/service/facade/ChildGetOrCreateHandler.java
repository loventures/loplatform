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

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import org.apache.commons.lang3.reflect.TypeUtils;
import scala.Function1;
import scaloi.GetOrCreate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChildGetOrCreateHandler extends ChildGetHandler {

    private final GeneralAddChildSupport _addChildSupporter;
    private final int _createIndex, _f1Index, _consumerIndex;
    private final boolean _goc;
    private final boolean _finder;

    public ChildGetOrCreateHandler(Method method, FacadeChild facadeChild, Class<? extends Facade> facade) {
        super(method, facadeChild, facade, false, null);
        _addChildSupporter = new GeneralAddChildSupport(facadeChild, method, _itemType);
        Annotation[][] annotations = method.getParameterAnnotations();
        Class<?>[] types = method.getParameterTypes();
        int createIndex = -1, f1Index = -1, consumerIndex = -1;
        for (int i = 0; i < annotations.length; ++i) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof FacadeCreate) {
                    createIndex = i;
                }
            }
            if (Function1.class.isAssignableFrom(types[i])) {
                f1Index = i;
            } else if (Consumer.class.isAssignableFrom(types[i])) {
                consumerIndex = i;
            }
        }
        _createIndex = createIndex;
        _f1Index = f1Index;
        _consumerIndex = consumerIndex;
        _goc = GetOrCreate.class.isAssignableFrom(method.getReturnType());
        _finder = (f1Index >= 0) && Finder.class.isAssignableFrom(
                TypeUtils.getRawType(((ParameterizedType) method.getGenericParameterTypes()[f1Index]).getActualTypeArguments()[0], null));
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) throws Throwable {
        QueryBuilder qb = queryBuilder(handler, args);
        qb.setLimit(1);
        Item item = (Item) qb.getResult();
        if ((item != null) || ((_createIndex >= 0) && !Boolean.TRUE.equals(args[_createIndex]))) {
            return result(transformValue(item, handler, args), false);
        }
        new LockHandler().invoke(handler, new Object[]{Boolean.TRUE}); // pessimistic lock..
        // repeat without query cache
        qb.setCacheQuery(false);
        item = (Item) qb.getResult();
        if (item != null) {
            return result(transformValue(item, handler, args), false);
        }
        final Consumer initParam;
        if (_f1Index >= 0) {
            initParam = ((Function1) args[_f1Index])::apply;
        } else {
            initParam = (_consumerIndex >= 0) ? (Consumer) args[_consumerIndex] : null;
        }
        BiConsumer<Item, Facade> init = (newItem, newFacade) -> {
            addConditionData(handler, newItem, true, args);
            if (initParam != null) {
                initParam.accept(_finder ? newItem.getFinder() : newFacade);
            }
        };
        item = _addChildSupporter.addChild(handler, (BiConsumer) init, _facadeClass);
        addConditionData(handler, item, false, args); // this should go inside the init above
        Object value = transformValue(item, handler, args);
        return result(value, true);
    }

    // if getOrCreate had condition parameters then automatically apply them as data.
    private void addConditionData(FacadeInvocationHandler handler, Item item, boolean normalized, Object[] args) {
        for (int i = 0; i < _conditions.length; ++i) {
            FacadeCondition condition = _conditions[i];
            if ((condition != null)
                    && Comparison.eq.equals(condition.comparison())) {
                EntityDescriptor desc = BaseOntology.getOntology().getEntityDescriptor(item.getType());
                boolean isNormalized = (desc != null) && desc.containsDataType(condition.value());
                if (normalized == isNormalized) { // TODO: this breaks on mapped global data...
                    Data data = DataUtil.getInstance(condition.value(), args[i], BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
                    handler.getContext().getService(DataService.class).copy(item, data);
                }
            }
        }
    }

    private Object result(Object value, boolean created) {
        return _goc ? GetOrCreate.apply(value, created) : value;
    }
}
