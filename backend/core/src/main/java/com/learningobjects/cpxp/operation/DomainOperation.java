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

package com.learningobjects.cpxp.operation;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.WebContext;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.util.Operation;

public class DomainOperation<T> extends DecoratedOperation<T> {
    private Long _domainId;

    public DomainOperation(Long domainId, Operation<T> operation) {
        super(operation);
        _domainId = domainId;
    }

    public T perform() {
        /* this ever-growing list of thread-localities increasingly motivates a later refactoring */
        UserDTO user = Current.getUserDTO();
        DomainDTO domain = Current.getDomainDTO();
        WebContext wc = BaseWebContext.copy(BaseWebContext.getContext());
        try {
            Current.clearCache();
            ServiceContext.getContext().getService(DomainWebService.class).setupContext(_domainId);
            return super.perform();
        } finally {
            Current.clearCache();
            Current.setUserDTO(user);
            Current.setDomainDTO(domain);
            BaseWebContext.setContext(wc);
        }
    }
}
