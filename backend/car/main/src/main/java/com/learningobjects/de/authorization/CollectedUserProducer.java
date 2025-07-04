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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.user.UserConstants;
import loi.cp.user.UserComponent;

/**
 * Collects the userId from an object
 */
@Component
public class CollectedUserProducer extends AbstractComponent implements CollectedAuthorityProducer {


    @Override
    public void produce(final SecurityContext securityContext, final Object object) {

        Long userId = null;

        if (object instanceof ComponentInterface) {
            final ComponentInterface component = (ComponentInterface) object;
            if (component.isComponent(UserComponent.class)) {
                final UserComponent userComponent = component.asComponent(UserComponent.class);
                userId = userComponent.getId();
            }
        } else if (object instanceof Facade) {
            final Facade facade = (Facade) object;
            if (UserConstants.ITEM_TYPE_USER.equals(facade.getItemType())) {
                userId = facade.getId();
            }
        }

        if (userId != null) {
            // overwrites a prior collected user
            securityContext.put(CollectedUser.class, new CollectedUser(userId));
        }

    }
}
