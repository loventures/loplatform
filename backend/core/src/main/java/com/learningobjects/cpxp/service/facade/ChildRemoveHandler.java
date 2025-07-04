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

import com.learningobjects.cpxp.dto.BaseOntology;
import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.service.item.Item;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Handler for a facade method that removes a child.
 */
class ChildRemoveHandler implements FacadeMethodHandler, UserDefinedMethodHandler {
    private final Method _method;
    private final Class<?> _declaringClass;
    private final String _itemType;
    private final boolean _isStandalone;

    public ChildRemoveHandler(Method method, FacadeChild facadeChild, Class<? extends Facade> facade) {
        _method = method;
        _declaringClass = method.getDeclaringClass();
        _itemType = FacadeUtils.getChildType(facadeChild, facade);
        Class<?> paramType = method.getParameterTypes()[0];
        EntityDescriptor descriptor = BaseOntology.getOntology().getEntityDescriptor(_itemType);
        _isStandalone = (descriptor != null) && !descriptor.getItemRelation().isPeered();
    }

    @Override
    public String getMethodName() {
        return _method.getName();
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    public Object invoke(FacadeInvocationHandler handler, Object[] args) {
        List valuesToRemove;
        if (List.class.isAssignableFrom(args[0].getClass())) {
            valuesToRemove = (List)args[0];
        } else {
            valuesToRemove = Collections.singletonList(args[0]);
        }
        for (Object valueToRemove : valuesToRemove) {
            // Technically I could just access the Item underlying the facade
            // in that case, but to expose that would be unspeakable.
            Long id = Facade.class.isAssignableFrom(valueToRemove.getClass()) ? ((Facade) valueToRemove).getId() : (Long) valueToRemove;
            Item item;
            if (_isStandalone) {
                item = handler.getContext().getItemService().get(id, _itemType);
            } else {
                item = handler.getContext().getItemService().get(id);
            }
            if (item != null) {
                if (!item.getParent().equals(handler.getItem())) {
                    throw new RuntimeException("Attempt to remove non-child: " + id);
                }
                handler.getContext().getItemService().destroy(item);
                // handler.getContext().getService(ItemWebService.class).evictFromCache(handler.getItem().getId());
            }
        }

        handler.removeAllValuesInHandlerGroup(getMethod());

        return null;
    }

    @Override
    public String toString() {
        return String.format("[itemType: %1$s, declaringClass: %2$s, method: %3$s]", _itemType, _declaringClass, _method.getName());
    }
}
