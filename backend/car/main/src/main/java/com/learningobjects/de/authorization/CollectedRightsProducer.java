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
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.util.Ids;
import com.learningobjects.de.group.GroupComponent;
import loi.cp.right.Right;
import loi.cp.right.RightService;

import javax.inject.Inject;
import java.util.Set;

/**
 * Collects the subject's rights from an object.
 */
@Component
public class CollectedRightsProducer extends AbstractComponent implements CollectedAuthorityProducer {

    @Inject
    private RightService rightService;

    private static final Set<String> SUPPORTED_ROLE_ITEM_TYPES = Set.of(GroupConstants.ITEM_TYPE_GROUP, DomainConstants.ITEM_TYPE_DOMAIN);

    @Override
    public void produce(final SecurityContext securityContext, final Object object) {

        Id groupId = null;

        if (object instanceof ComponentInterface) {
            final ComponentInterface component = (ComponentInterface) object;
            if (component.isComponent(GroupComponent.class)) {
                final GroupComponent groupComponent = component.asComponent(GroupComponent.class);

                groupId = Ids.of(groupComponent.getId());

            }
        } else if (object instanceof Facade) {
            final Facade facade = (Facade) object;
            if (SUPPORTED_ROLE_ITEM_TYPES.contains(facade.getItemType())) {
                groupId = facade;
            }
        }

        if (groupId != null) {
            final Set<Class<? extends Right>> userRights = rightService.getUserRights(groupId);
            final CollectedRights collectedRights = securityContext.get(CollectedRights.class);
            collectedRights.addAll(userRights);
        }

    }

}
