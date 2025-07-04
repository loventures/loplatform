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
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.DataModel;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.item.Item;

import javax.ejb.Local;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * The facade service.
 */
@Local
public interface FacadeService {
    <T extends Facade> T getDummy(Class<T> facadeClass);

    /**
     * Gets a facade for a particular id.
     */
    <T extends Facade> T getFacade(Long id, Class<T> facadeClass);

    /**
     * Gets a facade for a particular id.
     */
    <T extends Facade> T getFacade(Id id, Class<T> facadeClass);

    /**
     * Gets a facade for a particular string id.
     */
    <T extends Facade> T getFacade(String idStr, Class<T> facadeClass);

    /**
     * Gets a facade for a particular id and item type.
     */
    <T extends Facade> T getFacade(Long id, String itemType, Class<T> facadeClass);

    /**
     * Gets a facade.
     */
    <T extends Facade> T getFacade(Item item, Class<T> facadeClass);

    <T extends Facade> List<T> getFacades(Iterable<Long> ids, Class<T> facadeClass);
    <T extends Facade> List<T> getFacades(Iterable<Long> ids, String itemType, Class<T> facadeClass);

    <T extends Facade> T addFacade(Long parentId, Class<T> facadeClass);
    <T extends Facade> T addFacade(Item parent, Class<T> facadeClass);
    <T extends Facade> T addFacade(Item parent, Class<T> facadeClass, String itemType);
    <T extends Facade> T addFacade(Long parentId, Class<T> facadeClass, Consumer<T> init);

    <T extends ComponentInterface> T getComponent(String id, Class<T> componentClass);
    <T extends ComponentInterface> T getComponent(Long id, Class<T> componentClass);
    <T extends ComponentInterface> T getComponent(Id id, Class<T> componentClass);
    <T extends ComponentInterface, S extends T> S addComponent(Long parent, Class<T> component, DataModel<T> dataModel, Class<S> implementation, Object init);
    <T extends ComponentInterface, S extends T> S addComponent(Long parent, Class<T> component, DataModel<T> dataModel, S init);
}
