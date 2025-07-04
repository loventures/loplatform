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

package com.learningobjects.de.authorization;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.util.lang.OptionLike;
import loi.cp.context.HasContextId;

import javax.inject.Inject;
import java.util.List;

/**
 * Invokes {@link CollectedAuthorityProducer}s lazily and caches them. (TODO lazily and TODO caching)
 */
@Service
public class CachingCollectedAuthorityManager implements CollectedAuthorityManager {

    @Inject
    private FacadeService facadeService;

    private final List<CollectedAuthorityProducer> producers;

    // for delegate descriptor access only
    public CachingCollectedAuthorityManager() {
        producers = ComponentSupport.getComponents(CollectedAuthorityProducer.class);
    }

    @Override
    public void collectAuthoritiesFromReference(final SecurityContext securityContext, final Object reference) {

        final Long itemId;
        if (reference instanceof Long) {
            itemId = (Long) reference;
        } else if (reference instanceof String) {
            itemId = Long.valueOf((String) reference);
        } else if (reference instanceof ComponentInterface) {
            final ComponentInterface component = (ComponentInterface) reference;
            itemId = component.getComponentInstance().getId();
        } else if (reference instanceof HasContextId) {
            itemId = ((HasContextId) reference).contextId().value();
        } else if (reference instanceof Id) {
            itemId = ((Id) reference).getId();
        } else {
            return;
        }

        collectAuthorities(securityContext, itemId, true);

    }

    @Override
    public void collectAuthorities(final SecurityContext securityContext, final Object value) {
        final Object object =
          ((value != null) && OptionLike.isOptionLike(value.getClass())) ? OptionLike.getOrNull(value) : value;

        final Long itemId;
        if (object instanceof Id) {
            // shortcut for lots of objects
            itemId = ((Id) object).getId();
        } else if (object instanceof ComponentInterface) {
            final ComponentInterface component = (ComponentInterface) object;
            itemId = component.getComponentInstance().getId();
        } else {
            return;
        }

        collectAuthorities(securityContext, itemId, false);

    }

    private void collectAuthorities(final SecurityContext securityContext, final Long itemId,
      final boolean failOnNotFound) {
        final Object authoritySource = getAuthoritySource(itemId, failOnNotFound);

        if (authoritySource != null) {
            for (final CollectedAuthorityProducer producer : producers) {
                producer.produce(securityContext, authoritySource);
            }
        }
    }

    private Object getAuthoritySource(final Long id, final boolean failOnNotFound) {

        Object authoritySource = null;
        final ComponentFacade facade = facadeService.getFacade(id, ComponentFacade.class);
        try {
            // only succeeds if item persisted componentId, or if item type has one implementation (singleton=true)
            authoritySource = ComponentSupport.getComponent(facade, null).getInstance();
        } catch (Exception ex) {
            authoritySource = facade;
        }

        if (authoritySource == null && failOnNotFound) {
            throw new ResourceNotFoundException("No such resource: " + id);
        }

        return authoritySource;
    }

}
