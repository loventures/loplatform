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
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.PostCreate;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.query.QueryCache;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.service.site.SiteConstants;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.NumberUtils;
import scala.collection.immutable.Map$;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The facade service.
 */
@SuppressWarnings("unused")
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FacadeServiceBean extends BasicServiceBean implements FacadeService {
    private static final Logger logger = Logger.getLogger(FacadeServiceBean.class.getName());

    public FacadeServiceBean(
      ItemService itemService,
      QueryCache queryCache
    ) {
        super(itemService, queryCache);
    }

    public <T extends Facade> T getDummy(Class<T> facadeClass) {
        return FacadeFactory.getDummy(facadeClass, ServiceContext.getContext());
    }

    @Override
    public <T extends Facade> List<T> getFacades(Iterable<Long> ids, Class<T> facadeClass) {
        return getFacades(ids, null, facadeClass);
    }

    @Deprecated
    public <T extends Facade> T getFacade(Long id, Class<T> facadeClass) {
        return getFacade(id, null, facadeClass);
    }

    @Deprecated
    public <T extends Facade> T getFacade(Id id, Class<T> facadeClass) {
        return getFacade((id == null) ? null : id.getId(), null, facadeClass);
    }

    public <T extends Facade> T getFacade(Long id, String itemType, Class<T> facadeClass) {
        if (itemType == null) {
            itemType = FacadeDescriptorFactory.getFacadeType(facadeClass);
        }
        Item item = _itemService.get(id, itemType);
        return FacadeFactory.getFacade(facadeClass, item, ServiceContext.getContext());
    }

    @Override
    public <T extends Facade> List<T> getFacades(Iterable<Long> ids, String itemType, Class<T> facadeClass){
        if (itemType == null) {
            itemType = FacadeDescriptorFactory.getFacadeType(facadeClass);
        }

        ServiceContext serviceContext = ServiceContext.getContext();
        return _itemService.get(ids, itemType)
          .stream()
          .map(item -> FacadeFactory.getFacade(facadeClass, item, serviceContext))
          .collect(Collectors.toList());
    }

    public <T extends Facade> T getFacade(String id, Class<T> facadeClass) {
        return getFacade(findDomainItemById(id), facadeClass);
    }

    public <T extends Facade> T getFacade(Item item, Class<T> facadeClass) {
        return FacadeFactory.getFacade(facadeClass, item, ServiceContext.getContext());
    }

    public <T extends Facade> T addFacade(Long parentId, Class<T> facadeClass) {
        String itemType = FacadeDescriptorFactory.getFacadeType(facadeClass);
        return addFacade(_itemService.get(parentId), facadeClass, itemType, null);
    }

    public <T extends Facade> T addFacade(Long parentId, Class<T> facadeClass, Consumer<T> init) {
        String itemType = FacadeDescriptorFactory.getFacadeType(facadeClass);
        return addFacade(_itemService.get(parentId), facadeClass, itemType, init);
    }

    public <T extends Facade> T addFacade(Item parent, Class<T> facadeClass) {
        String itemType = FacadeDescriptorFactory.getFacadeType(facadeClass);
        return addFacade(parent, facadeClass, itemType, null);
    }

    public <T extends Facade> T addFacade(Item parent, Class<T> facadeClass, String itemType) {
        return addFacade(parent, facadeClass, itemType, null);
    }

    private <T extends Facade> T addFacade(Item parent, Class<T> facadeClass, String itemType, Consumer<T> init) {
        Consumer<Item> itemInit = (init == null) ? null
          : (Item item) -> init.accept(getFacade(item, facadeClass));
        Item item = _itemService.create(parent, itemType, itemInit);
        return FacadeFactory.getFacade(facadeClass, item, ServiceContext.getContext());
    }

    public <T extends ComponentInterface> T getComponent(String id, Class<T> componentClass) {
        if (NumberUtils.isNumber(id)) {
            logger.log(Level.WARNING,"Passed id of digits (" + id + ") into getComponent(String, Class), maybe you wanted getComponent(Long, Class) ?");
        }

        Item i = findDomainItemById(id);
        return (i == null) ? null : getComponent(i.getId(), componentClass);
    }

    public <T extends ComponentInterface> T getComponent(Id id, Class<T> componentClass) {
        return getComponent(id.getId(), componentClass);
    }

    public <T extends ComponentInterface> T getComponent(Long id, Class<T> componentClass) {
        return ComponentSupport.get(id, componentClass);
    }

    @Override
    public <T extends ComponentInterface, S extends T> S addComponent(Long parent, Class<T> component, DataModel<T> dataModel, Class<S> implementation, Object init) {
        String fqIdentifier = ComponentSupport.getComponentDescriptor(implementation).getIdentifier();
        // TODO: this should only consider leaf schema annotations, not inherited ones (e.g. on NotificationComponent)
        String identifier = !dataModel.schemaMapped() ? fqIdentifier
          : ClassUtils.findAnnotation(implementation, Schema.class).map(Schema::value).orElse(fqIdentifier);
        return addComponent(parent, component, dataModel, identifier, init).getInstance(implementation);
    }

    @Override
    public <T extends ComponentInterface, S extends T> S addComponent(Long parent, Class<T> component, DataModel<T> dataModel, S init) {
        String fqIdentifier = FacadeComponentHandler.getInstanceDescriptor(init).getIdentifier();
        // TODO: this should only consider leaf schema annotations, not inherited ones (e.g. on NotificationComponent)
        String identifier = !dataModel.schemaMapped() ? fqIdentifier
          : ClassUtils.findAnnotation(init.getClass(), Schema.class).map(Schema::value).orElse(fqIdentifier);
        return (S) addComponent(parent, component, dataModel, identifier, init).getInstance();
    }

    private static <T extends ComponentInterface> DataModel<T> dataModelOf(Class<T> clas) {
        ItemMapping itemMapping = clas.getAnnotation(ItemMapping.class);
        return DataModel.apply(itemMapping.value(), itemMapping.singleton(), itemMapping.schemaMapped(), Map$.MODULE$.empty(), Map$.MODULE$.empty());
    }

    private <T extends ComponentInterface> ComponentInstance addComponent(Long parent, Class<T> component, DataModel<?> dataModel, String implementation, Object init) {
        String itemType = dataModel.itemType();
        Item item = _itemService.create(_itemService.get(parent), itemType);
        ComponentFacade facade = getFacade(item, ComponentFacade.class);
        if (!dataModel.singleton()) {
            facade.setIdentifier(implementation);
        }
        if (SiteConstants.ITEM_TYPE_SITE.equals(itemType)) { // hack that ITEM_TYPE_SITE uses explicit category field.. TODO: de-hack
            facade.setCategory(component.getName());
        }
        ComponentDescriptor descriptor = dataModel.singleton() ? ComponentSupport.getComponentDescriptor(component)
          : ComponentSupport.getComponentDescriptor(implementation);
        ComponentInstance instance = descriptor.getInstance(facade, null);
        if (init != null) {
            Current.put(init.getClass(), init);
            //TODO figure out a better way to do this.  For now, the InferEvaluator looks up the object via the parameter class
            // which might be a super-type or interface of this object, so we need to chuck the item keyed under all these types for now...
            for (Class inter : init.getClass().getInterfaces()) {
                Current.put(inter, init);
            }
        }
        try {
            ComponentSupport.lifecycle(instance, PostCreate.class);
        } catch (Exception e) {
            throw new RuntimeException("Init error", e);
        }
        if (init != null) {
            Current.remove(init.getClass());
            for (Class inter : init.getClass().getInterfaces()) {
                Current.remove(inter);
            }
        }
        return instance;
    }

}
