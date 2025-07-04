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

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.Infer;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.component.query.ItemMappingAnnotation;
import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataUtil;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.*;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.service.site.SiteConstants;
import com.learningobjects.cpxp.util.ObjectUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.lang.OptionLike;
import scala.jdk.javaapi.CollectionConverters;
import scala.reflect.ClassTag;
import scaloi.GetOrCreate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handler for a facade method that deals with components.
 */
public class FacadeComponentHandler implements FacadeMethodHandler {
    private final Method _method;
    private final Class<?> _wrappingResultType, _resultType;
    private final boolean _isAdd, _isGet, _isSet, _isQuery, _byId, _isCount, _isGetOrCreate;
    private final boolean _isList, _isScalaList, _isTypeVariable, _isOptional, _isGoC;
    private final Class<? extends ComponentInterface> _category;
    private final ItemMapping _mapping;
    private String _orderType;
    private final Direction _direction;
    private final FacadeCondition[] _conditions;
    private final int _offset;
    private final int _limit;
    private final int _createIndex; // index of optional creation parameter
    private final int _firstIndex; // first unannotated index, implicit component class
    private final int _secondIndex; // second unannotated index, initializer item
    private final boolean _classTag; // do we have a class tag (goes last)
    private final boolean _addCopy;
    private final boolean _cache, _domain, _debug;

    @SuppressWarnings("unchecked")
    public FacadeComponentHandler(Method method, Class<? extends ComponentInterface> componentInterface, FacadeComponent facadeComponent) throws Exception {
        _method = method;
        String methodName = method.getName();
        _isAdd = methodName.startsWith("add");
        _isGet = methodName.startsWith("get") || methodName.startsWith("find");
        _isGetOrCreate = methodName.startsWith("getOrCreate");
        _isSet = methodName.startsWith("set");
        _isQuery = methodName.startsWith("query");
        _isCount = methodName.startsWith("count");
        _wrappingResultType = method.getReturnType();
        _isScalaList = scala.collection.immutable.List.class.isAssignableFrom(_wrappingResultType);
        _isList = List.class.isAssignableFrom(_wrappingResultType) || _isScalaList;
        _isOptional = OptionLike.isOptionLike(_wrappingResultType);
        _isGoC = GetOrCreate.class.isAssignableFrom(_wrappingResultType);
        boolean unwrap = (_isList || _isOptional || _isGoC);
        Type genericType = method.getGenericReturnType();
        _resultType = unwrap ? FacadeDescriptorFactory.getActualType(genericType, 0) : _wrappingResultType;
        Type unwrapped = unwrap ? ((ParameterizedType) genericType).getActualTypeArguments()[0] : genericType;
        _isTypeVariable = unwrapped instanceof TypeVariable;
        Class<?>[] paramTypes = method.getParameterTypes();
        _category = componentInterface;
        _mapping = _category.getAnnotation(ItemMapping.class);
        int argl = paramTypes.length;
        boolean hasDataModel = (argl > 0) && DataModel.class.equals(paramTypes[paramTypes.length - 1]);
        if (hasDataModel) {
            -- argl;
        }
        if ((_mapping == null) && !hasDataModel) {
            throw new Exception("Missing data model evidence for " + _category);
        }
        if (_isList || _isQuery || _isGet || _isSet) { // do i ever not want it???
            FacadeQuery q = method.getAnnotation(FacadeQuery.class);
            if (q != null) {
                _orderType = q.orderType();
                _direction = q.orderDirection();
                _domain = q.domain();
                _cache = q.cache() && !_domain;
                _debug = q.debug();
            } else {
                _orderType = facadeComponent.orderType();
                _direction = facadeComponent.direction();
                _cache = true;
                _domain = false;
                _debug = false;
            }
        } else {
            _orderType = null;
            _direction = null;
            _cache = true;
            _domain = false;
            _debug = false;
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        _conditions = new FacadeCondition[annotations.length];
        int offset = -1, limit = -1, createIndex = -1, firstIndex = -1, secondIndex = -1;
        for (int i = 0; i < argl; ++i) {
            boolean known = false;
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof FacadeCondition) {
                    _conditions[i] = (FacadeCondition) annotation;
                    known = true;
                } else if (annotation instanceof FacadeOffset) {
                    offset = i;
                    known = true;
                } else if (annotation instanceof FacadeLimit) {
                    limit = i;
                    known = true;
                } else if (annotation instanceof FacadeCreate) {
                    createIndex = i;
                    known = true;
                }
            }
            if (!known) { // yuck.. TODO use @FacadeImplementation etc
                if (firstIndex < 0) {
                    firstIndex = i;
                } else if (secondIndex < 0) {
                    secondIndex = i;
                }
            }
        }
        _byId = _isGet && (argl == 1) && (Long.class.equals(paramTypes[0]) || Long.TYPE.equals(paramTypes[0]))  && (annotations[0].length == 0);
        _offset = offset;
        _limit = limit;
        _createIndex = createIndex;
        _firstIndex = firstIndex;
        _secondIndex = secondIndex;
        _classTag = _isAdd && (argl > 0) && ClassTag.class.isAssignableFrom(paramTypes[paramTypes.length - 1]);
        _addCopy = (_firstIndex >= 0) && ComponentInterface.class.isAssignableFrom(paramTypes[_firstIndex]);
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    private static ItemMapping fakeMapping(DataModel<?> dm) {
        return new ItemMappingAnnotation(dm.itemType(), null, dm.singleton(), dm.schemaMapped());
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) throws Throwable {
        final ItemMapping mapping = (_mapping != null) ? _mapping :
          fakeMapping((DataModel<?>) args[args.length - 1]);
        if (_isAdd || _isSet) {
            return invokeAddOrSet(handler, args, mapping);
        } else if (_byId) {
            Long id = (Long) args[0];
            ComponentFacade facade = handler.getContext().getFacadeService().getFacade(id, mapping.value(), ComponentFacade.class);
            if ((facade != null) && !facade.getParentId().equals(handler.getId())) {
                facade = null;
            }
            ComponentInstance instance = (facade == null) ? null : getComponent(facade, handler, mapping);
            Object result = getResult(instance);
            if (_isOptional) {
                return OptionLike.ofNullable(_wrappingResultType, result);
            } else {
                return result;
            }
        } else if (_isGet) {
            QueryBuilder queryBuilder = queryComponents(handler, args, mapping);
            if (!_isList) {
                queryBuilder.setLimit(1);
            }
            List<Object> results = new ArrayList<>();
            for (ComponentFacade facade : queryBuilder.getFacadeList(ComponentFacade.class)) {
                try {
                    results.add(getResult(getComponent(facade, handler, mapping)));
                } catch (Exception ex) {
                    // Ignore and move on.. That's not cool at all, but it's to handle
                    // the case where a component is no longer available. Should instead
                    // return an unsupported component implementation.
                    // logThrowable(Level.WARNING, "Get error: " + facade.getId(), ex);
                }
            }
            boolean created = false;
            if (results.isEmpty() && (_isGetOrCreate || ((_createIndex >= 0) && Boolean.TRUE.equals(args[_createIndex])))) {
                new LockHandler().invoke(handler, new Object[]{Boolean.TRUE}); // pessimistic lock..
                queryBuilder.setCacheQuery(false); // re-run the query without caching
                for (ComponentFacade facade : queryBuilder.getFacadeList(ComponentFacade.class)) {
                    results.add(getResult(getComponent(facade, handler, mapping)));
                }
                if (results.isEmpty()) { // create with pessimistic lock
                    created = true;
                    results.add(invokeAddOrSet(handler, args, mapping));
                }
            }
            if (_isScalaList) {
                return CollectionConverters.asScala(results).toList();
            } else if (_isList) {
                return results;
            } else {
                Object result = ObjectUtils.getFirstNonNullIn(results);
                if (_isOptional) {
                    return OptionLike.ofNullable(_wrappingResultType, result);
                } else if (_isGoC) {
                    return GetOrCreate.apply(result, created);
                } else {
                    return result;
                }
            }
        } else if (_isQuery) {
            return queryComponents(handler, args, mapping);
        } else if (_isCount) {
            return queryComponents(handler, args, mapping).getAggregateResult(Function.COUNT);
        } else { // remove
            List valuesToRemove;
            if (List.class.isAssignableFrom(args[0].getClass())) {
                valuesToRemove = (List) args[0];
            } else {
                valuesToRemove = Collections.singletonList(args[0]);
            }
            for (Object valueToRemove : valuesToRemove) {
                Long id = Facade.class.isAssignableFrom(valueToRemove.getClass()) ? ((Facade) valueToRemove).getId() : (Long) valueToRemove;
                Item item = handler.getContext().getItemService().get(id);
                if (item != null) {
                    if (!item.getParent().equals(handler.getItem())) {
                        throw new RuntimeException("Attempt to remove non-child: " + id);
                    }
                    handler.getContext().getItemService().destroy(item);
                    // handler.getContext().getService(ItemWebService.class).evictFromCache(handler.getItem().getId());
                }
            }
            return null;
        }
    }

    private Object invokeAddOrSet(FacadeInvocationHandler handler, Object[] args, ItemMapping mapping) throws Throwable {
        int typeIx = _classTag ? args.length - 1 : _firstIndex;
        ComponentDescriptor component = mapping.singleton() ? getSingletonComponent() : getComponentDescriptor(args[typeIx]);
        if (_isSet && StringUtils.isEmpty(_orderType)) {
            if (!queryComponents(handler, null, mapping).getItems().isEmpty()) {
                throw new Exception("A component of category: " + _category + " is already set");
            }
        }
        String itemType = mapping.value();
        Item item = handler.getContext().getItemService().create(handler.getItem(), itemType);
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                FacadeCondition condition = _conditions[i];
                if ((condition == null) || !Comparison.eq.equals(condition.comparison())) {
                    continue;
                }
                Data data = DataUtil.getInstance(condition.value(), args[i], BaseOntology.getOntology(), ServiceContext.getContext().getItemService());
                handler.getContext().getService(DataService.class).copy(item, data);
            }
        }
        ComponentFacade facade = handler.getContext().getService(FacadeService.class).getFacade(item, ComponentFacade.class);
        if (!mapping.singleton()) {
            if (facade == null) {
                throw new RuntimeException("Invalid item: "+item.getType()+"-"+item.getId());
            }
            if (component == null) {
                throw new RuntimeException("Invalid component for "+args[_firstIndex]);
            }
            String identifier = !mapping.schemaMapped() ? component.getIdentifier()
              : ComponentSupport.getSchemaName(component);
            facade.setIdentifier(identifier);
        }
        if (SiteConstants.ITEM_TYPE_SITE.equals(itemType)) { // hack that ITEM_TYPE_SITE uses explicit category field.. TODO: de-hack
            facade.setCategory(_category.getName());
        }
        /* classtag isn't at _firstindex even if it's nonsingleton */
        int index = (mapping.singleton() || _addCopy || _classTag) ? _firstIndex : _secondIndex;
        Object init = (index >= 0) ? args[index] : null;
        Object[] initArgs = (index < 0) ? new Object[0]
            : Arrays.copyOfRange(args, index, args.length);
        ComponentInstance instance = component.getInstance(facade, handler.getId());
        if (init != null) {
            Current.put(Infer.class, init);
        }
        ComponentSupport.lifecycle(instance, PostCreate.class);
        if (init != null) {
            Current.remove(Infer.class);
        }
        return getResult(instance);
    }

    private ComponentInstance getComponent(ComponentFacade facade, FacadeInvocationHandler handler, ItemMapping mapping) {
        if (mapping.singleton()) {
            return getSingletonComponent().getInstance(facade, null);
        } else {
            return ComponentSupport.getComponent(facade, handler.getId());
        }
    }

    private ComponentDescriptor getSingletonComponent() {
        ComponentEnvironment env = BaseWebContext.getContext().getComponentEnvironment();
        return ObjectUtils.getFirstNonNullIn(env.getComponents(_category));
    }

    private QueryBuilder queryComponents(FacadeInvocationHandler handler, Object[] args, ItemMapping mapping) {
        QueryService queryService = handler.getContext().getService(QueryService.class);
        QueryBuilder queryBuilder;
        if (_domain) {
            queryBuilder = queryService.queryRoot(mapping.value());
        } else {
            queryBuilder = queryService.queryParent(handler.getItem().getId(), mapping.value());
        }
        if (SiteConstants.ITEM_TYPE_SITE.equals(mapping.value())) { // hack that ITEM_TYPE_SITE uses explicit category field.. TODO: de-hack
            queryBuilder.addCondition(SiteConstants.DATA_TYPE_COMPONENT_CATEGORY, "eq", _category.getName());
        }
        if (StringUtils.isNotEmpty(_orderType)) {
            queryBuilder.setOrder(_orderType, _direction);
        }
        queryBuilder.setLogQuery(_debug);
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                Object arg = args[i];
                if (i == _offset) {
                    queryBuilder.setFirstResult(((Number) arg).intValue());
                } else if (i == _limit) {
                    queryBuilder.setLimit(((Number) arg).intValue());
                } else if (_conditions[i] != null) {
                    if (arg instanceof Id) {
                        arg = ((Id) arg).getId();
                    }
                    FacadeCondition condition = _conditions[i];
                    Comparison cmp = condition.comparison();
                    if ((arg != null) || (cmp == Comparison.eq) || (cmp == Comparison.ne)) {
                        queryBuilder.addCondition(BaseCondition.getInstance(condition.value(), cmp, arg, condition.function()));
                    }
                }
            }
        }
        if (!_cache) {
            queryBuilder.setCacheQuery(false);
        }

        return queryBuilder;
    }

    private ComponentDescriptor getComponentDescriptor(Object value) {
        if (value instanceof BaseComponentDescriptor) {
            return (ComponentDescriptor) value;
        } else if (value instanceof ComponentInterface) {
            return getInstanceDescriptor((ComponentInterface) value);
        } else if (value instanceof ClassTag) {
            String identifier = ((ClassTag<?>) value).runtimeClass().getName();
            return ComponentSupport.getComponentDescriptor(identifier);
        } else if (value instanceof String) {
            return ComponentSupport.getComponentDescriptor((String) value);
        } else {
            return ComponentSupport.getComponentDescriptor((Class<? extends ComponentInterface>) value);
        }
    }

    private Object getResult(ComponentInstance instance) throws Exception {
        if ((instance == null) || void.class.equals(_resultType)) {
            return null;
        } else if (Long.class.equals(_resultType) || Long.TYPE.equals(_resultType)) {
            return instance.getId();
        } else if (ComponentInstanceImpl.class.equals(_resultType)) {
            return instance;
        } else if (ComponentInterface.class.isAssignableFrom(_resultType)) {
            if (_isTypeVariable) { // public <T extends XyzComponent> T addFoo(Class<T> foo)
                return instance.getInstance();
            } else {
                return instance.getInstance((Class<? extends ComponentInterface>) _resultType);
            }
        } else {
            throw new Exception("Unsupported result type: " + _resultType);
        }
    }

    /**
     * Retrieves the component descriptor for a given component instance.
     * This supports both component instances and synthetic components fabricated
     * by such as mr bean.
     */
    public static <T extends ComponentInterface> ComponentDescriptor getInstanceDescriptor(T t) {
        ComponentInstance instance = t.getComponentInstance();
        if (instance != null) { // a real component
            return instance.getComponent();
        } else { // a synthetic component
            Class<?>[] interfaces = t.getClass().getInterfaces();
            if (interfaces.length == 0) { // a subclass of a component implementation
                interfaces = t.getClass().getSuperclass().getInterfaces();
            }
            Class<? extends ComponentInterface> iface = (Class<? extends ComponentInterface>) interfaces[0];
            return ComponentSupport.getComponentDescriptor(iface);
        }
    }
}
