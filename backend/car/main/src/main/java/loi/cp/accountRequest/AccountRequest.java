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

package loi.cp.accountRequest;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import loi.cp.user.UserComponent;

import java.util.Date;
import java.util.Map;

/**
 * This decorates a User item with additional account request properties.
 */
@Component
public class AccountRequest extends AbstractComponent implements AccountRequestComponent {
    @Instance
    private AccountRequestFacade _instance;

    @Override
    public void init(AccountRequestComponent request) {
        _instance.setAccountRequestAttributes(request.getAttributes());
    }

    @Override
    public Long getId() {
        return _instance.getId();
    }

    @Override
    public UserComponent getUser() {
        return ComponentSupport.get(_instance.getId(), UserComponent.class);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return _instance.getAccountRequestAttributes();
    }

    @Override
    public Date getCreateTime() {
        return _instance.getCreateTime();
    }
}
