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

package loi.cp.login;

import com.learningobjects.cpxp.component.BaseComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.service.login.LoginWebService;
import loi.cp.accountRequest.AccountRequestRootComponent;
import loi.cp.integration.AbstractSystem;
import loi.cp.password.PasswordRootApi;

import javax.inject.Inject;

@Component(name = "Direct Login")
public class DirectLoginSystem extends AbstractSystem<DirectLoginSystemComponent> implements DirectLoginSystemComponent {
    @Inject
    LoginWebService _loginWebService;

    @Inject
    PasswordRootApi _passwordRoot;

    @Inject
    AccountRequestRootComponent _accountRequestRoot;

    @Override
    public DirectLoginComponent getLoginComponent() {
        return new DirectLogin();
    }

    @Override
    public String logout() {
        return null;
    }

    @Override
    public Long login(String userName, String password) {
        return _loginWebService.authenticate(userName, password);
    }

    @Override
    public boolean externalPassword() {
        return false;
    }

    private class DirectLogin extends BaseComponent implements DirectLoginComponent {
        public DirectLogin() {
            super(DirectLoginSystem.this.getComponentInstance());
        }

        @Override
        public Long getId() {
            return DirectLoginSystem.this.getId();
        }

        @Override
        public String getName() {
            return DirectLoginSystem.this.getName();
        }

        @Override
        public boolean isPasswordRecoveryEnabled() {
            return _passwordRoot.settingsAdmin().recovery().enabled();
        }

        @Override
        public AccountRequestRootComponent.Status getAccountRequests() {
            return _accountRequestRoot.getAccountRequestStatus();
        }
    }
}
