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
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.CacheFactory;

import java.lang.reflect.Proxy;
import java.util.logging.Logger;

public class FacadeFactory {
    private static final Logger logger = Logger.getLogger(FacadeFactory.class.getName());

    private FacadeFactory() {
    }

    public static<T extends Facade> T getDummy(Class<T> facadeClass) {
        return getDummy(facadeClass, ServiceContext.getContext());
    }

    public static <T extends Facade> T getDummy(Class<T> facadeClass, ServiceContext context) {
        FacadeDescriptor descriptor = FacadeDescriptorFactory.getDescriptor
            ("*", facadeClass);
        FacadeInvocationHandler handler = new FacadeInvocationHandler
            (descriptor, null, context);
        Facade facade = (Facade) Proxy.newProxyInstance
            (facadeClass.getClassLoader(), new Class<?>[]
                    { facadeClass }, handler);
        return facadeClass.cast(facade);
    }

    public static <T extends Facade> T getFacade(Class<T> facadeClass, Item item, ServiceContext context) {
        // Long currentDomain = Current.getDomain();
        if ((item == null) || (item.getDeleted() != null)) { // TODO: Want to do this but it breaks too much... || ((currentDomain != null) && !currentDomain.equals(item.getRoot().getId()))) {
            return null;
        }

        // If GenericFacade supports this interface then create a
        // generic facade instead. This improves caching performace
        // at the cost of losing typesafety.
        // TODO: FIXME: How to still support typesafety.
        Class<? extends Facade> facadeImpl = facadeClass;
        if (facadeClass.isAssignableFrom(GenericFacade.class)) {
            facadeImpl = GenericFacade.class;
        }

        return facadeClass.cast(Current.get(new FacadeCacheFactory(facadeImpl, item, context)));
    }

    private static class FacadeCacheFactory extends CacheFactory<Facade> {
        private final Class<? extends Facade> _facadeClass;
        private final Item _item;
        private final ServiceContext _context;

        public FacadeCacheFactory(Class<? extends Facade> facadeClass, Item item, ServiceContext context) {
            _facadeClass = facadeClass;
            _item = item;
            _context = context;
        }

        public Object getKey() {
            /* include hash code in order to not choke on component environment reloads... why do I need this? */
            return (_item.getId() == null) ? null : _item.getId() + "/" + _facadeClass.getName() + "@" + _facadeClass.hashCode();
        }

        public Facade create() {
            String itemType = _item.getType();
            FacadeDescriptor descriptor = FacadeDescriptorFactory.getDescriptor
                (itemType, _facadeClass);
            FacadeInvocationHandler handler = new FacadeInvocationHandler
                (descriptor, _item, _context);
            Facade facade = (Facade) Proxy.newProxyInstance
                (_facadeClass.getClassLoader(), new Class<?>[]
                        { _facadeClass }, handler);

            logger.fine(itemType + "/" + _facadeClass.getName() + ": " + facade.getClass().getName());

            return facade;
        }
    }
}
