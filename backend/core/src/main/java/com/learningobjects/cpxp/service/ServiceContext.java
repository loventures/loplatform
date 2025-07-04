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

package com.learningobjects.cpxp.service;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.ServiceMeta;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.query.QueryService;
import com.learningobjects.cpxp.util.ManagedUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ServiceContext  {
    private static final ServiceContext SINGLETON = new ServiceContext();

    protected ServiceContext() {
    }

    /**
     * Performs a lookup for an EJB reference mapped into the service context.
     *
     * @param <T>
     *            type of session bean for which the reference will be retrieved
     * @param serviceInterface
     *            binding parameter and used for computing a name, using
     *            {@link Class#getSimpleName()}
     * @return a usable reference if one is mapped into the context or throws an
     *         unchecked exception with an explanation
     */
    @Deprecated // outside of framework, ask for the service explicitly
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface) {
        return ManagedUtils.getService(serviceInterface);
    }

    public EntityManager getEntityManager() {
        return ManagedUtils.getEntityContext().getEntityManager();
    }

    public Query createQuery(String sql) {
        return getEntityManager().createQuery(sql);
    }

    public Query createNativeQuery(String sql) {
        return getEntityManager().createNativeQuery(sql);
    }

    public DataService getDataService() {
        return getService(DataService.class);
    }

    public ItemService getItemService() {
        return getService(ItemService.class);
    }

    public FacadeService getFacadeService() {
        return getService(FacadeService.class);
    }

    public QueryBuilder queryBuilder() {
        return getService(QueryService.class).queryAllDomains();
    }

    public ServiceMeta getServiceMeta() {
        return BaseServiceMeta.getServiceMeta();
    }

    public Item findDomainItemById(Long domainId, String id) {
        Item domain = getItemService().get(domainId);
        QueryBuilder qb = queryBuilder();
        qb.setRoot(domain);
        qb.setCacheQuery(true); // force caching
        qb.setCacheNothing(false); // don't cache nothing
        qb.addCondition(DataTypes.DATA_TYPE_ID, "eq", id);
        return (Item) qb.getResult();
    }

    public boolean loadFinder(Item item) {
        return getItemService().loadFinder(item);
    }

    public static ServiceContext getContext() {
        return SINGLETON;
    }

}
