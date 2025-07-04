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
import com.learningobjects.cpxp.service.query.QueryBuilder;
import scaloi.GetOrCreate;

import javax.ejb.Local;
import java.util.List;

/**
 * Service for doing some things with components. Partially a pass-through to `ComponentSupport`.
 */
@Service
@Local
public interface ComponentService {

    <T extends ComponentInterface> T get(IdType id, Class<T> iface);

    <T extends ComponentInterface> T get(Long id, String itemType, Class<T> iface);

    <T extends ComponentInterface> List<T> getById(Iterable<Long> ids, Class<T> iface);

    <T> Iterable<Class<? extends T>> lookupAllClasses(Class<T> clas);

    Class<?> loadClass(String fullyQualifiedClassName);

    /**
     * Search for and return a component, if it already exists in the database, or else create a new instance.
     * It is the caller's responsibility to ensure that the new component meets the criteria of the query builder
     * to prevent a repeat of this calling creating the component a second time. This would typically by the case
     * if you are searching by a particular property and the new initializer contains that property.
     * @param qb the query builder by which to search for the component
     * @param type the component type
     * @param dataModel the component data model
     * @param init the new component initializer
     * @return the new or existing component
     */
    <T extends ComponentInterface, S extends T> GetOrCreate<T> getOrCreate(QueryBuilder qb, Class<T> type, DataModel<T> dataModel, S init);

    /**
     * Search for and return a component, if it already exists in the database, or else create a new instance.
     * It is the caller's responsibility to ensure that the new component meets the criteria of the query builder
     * to prevent a repeat of this calling creating the component a second time. This would typically by the case
     * if you are searching by a particular property and the new initializer contains that property.
     * @param qb the query builder by which to search for the component
     * @param type the component type
     * @param dataModel the component data model
     * @param impl the new component implementation
     * @param init the new component initializer
     * @return the new or existing component
     */
    <T extends ComponentInterface, S extends T> GetOrCreate<T> getOrCreate(QueryBuilder qb, Class<T> type, DataModel<T> dataModel, Class<S> impl, Object init);
}
