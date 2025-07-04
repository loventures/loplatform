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

package com.learningobjects.cpxp.component;

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import scaloi.GetOrCreate;
import scala.Option;
import scala.reflect.ClassTag$;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Default implementation of {@link ComponentService}
 */
@Service
@SuppressWarnings("unused") // service impl
public class ComponentServiceBean implements ComponentService {
    private final ItemService itemService;
    private final FacadeService facadeService;

    public ComponentServiceBean(final ItemService itemService, final FacadeService facadeService) {
        this.itemService = itemService;
        this.facadeService = facadeService;
    }

    @Override
    public <T extends ComponentInterface> T get(IdType id, Class<T> iface) {
        return ComponentSupport.get(id, iface);
    }

    @Override
    public <T extends ComponentInterface> T get(Long id, String itemType, Class<T> iface) {
        return ComponentSupport.get(id, itemType, iface);
    }

    @Override
    public <T extends ComponentInterface> List<T> getById(Iterable<Long> ids, Class<T> iface) {
        return ComponentSupport.getById(ids, iface);
    }

    @Override
    public <T> Iterable<Class<? extends T>> lookupAllClasses(Class<T> clas) {
        return ComponentSupport.lookupAllClasses(clas);
    }

    @Override
    public Class<?> loadClass(String fullyQualifiedClassName) {
        return ComponentSupport.loadClass(fullyQualifiedClassName);
    }

    @Override
    public <T extends ComponentInterface, S extends T> GetOrCreate<T> getOrCreate(final QueryBuilder qb, final Class<T> type, final DataModel<T> dataModel, final Class<S> impl, final Object init) {
        return getOrCreateImpl(qb, type, dataModel, id -> facadeService.addComponent(id, type, dataModel, impl, init));
    }

    @Override
    public <T extends ComponentInterface, S extends T> GetOrCreate<T> getOrCreate(final QueryBuilder qb, final Class<T> type, final DataModel<T> dataModel, final S init) {
        return getOrCreateImpl(qb, type, dataModel, id -> facadeService.addComponent(id, type, dataModel, init));
    }

    private <T extends ComponentInterface, S extends T> GetOrCreate<T> getOrCreateImpl(final QueryBuilder qb, final Class<T> type, final DataModel<T> dataModel, final Function<Long, S> creator) {
        Optional<Item> parent = qb.getParent();
        if (!parent.isPresent()) {
            throw new IllegalArgumentException("Not a parent-based query: " + qb);
        }
        Option<T> existing = qb.getComponent(ClassTag$.MODULE$.apply(type), dataModel);
        if (existing.isDefined()) {
            return GetOrCreate.gotten(existing.get());
        }
        itemService.pessimisticLock(parent.get());
        qb.setCacheQuery(false);
        existing = qb.getComponent(ClassTag$.MODULE$.apply(type), dataModel);
        if (existing.isDefined()) {
            return GetOrCreate.gotten(existing.get());
        }
        return GetOrCreate.created(creator.apply(parent.get().getId()));
    }
}
