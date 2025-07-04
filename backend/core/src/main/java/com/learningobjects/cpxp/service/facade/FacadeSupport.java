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
import com.learningobjects.cpxp.service.ServiceContext;

public abstract class FacadeSupport {

    @Deprecated // don't be implicit about the fact that you're doing db access.
    public static <T extends Facade> T get(Long id, Class<T> facade) {
        return ServiceContext.getContext().getFacadeService().getFacade(id, null, facade);
    }

    public static String getItemType(Class<? extends Facade> facade) {
        String itemType = FacadeDescriptorFactory.getFacadeType(facade);
        return "*".equals(itemType) ? null : itemType;
    }
}
