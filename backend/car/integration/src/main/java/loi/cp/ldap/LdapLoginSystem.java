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

package loi.cp.ldap;

import com.learningobjects.cpxp.component.BaseComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.util.ConfigUtils;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService;
import com.learningobjects.cpxp.service.exception.ValidationException;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.login.LoginException;
import com.learningobjects.cpxp.service.login.LoginWebService;
import com.learningobjects.cpxp.service.relationship.RelationshipWebService;
import com.learningobjects.cpxp.service.relationship.RoleFacade;
import com.learningobjects.cpxp.service.user.UserType;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.StringUtils;
import com.typesafe.config.Config;
import loi.cp.integration.AbstractSystem;
import loi.cp.user.UserComponent;
import loi.cp.user.UserParentFacade;
import scaloi.GetOrCreate;

import javax.inject.Inject;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(name = "LDAP Login", alias = { "loi.cp:LDAP", "loi.cp.ldap.LdapSystem" })
public class LdapLoginSystem extends AbstractSystem<LdapLoginSystemComponent> implements LdapLoginSystemComponent {
    private static final Logger logger = Logger.getLogger(LdapLoginSystem.class.getName());

    @Inject
    private Config config;

    @Inject
    private EnrollmentWebService enrollmentWebService;

    @Inject
    private FacadeService facadeService;

    @Inject
    private LoginWebService loginWebService;

    @Inject
    private RelationshipWebService relationshipWebService;

    @Override
    public LdapLoginSystemComponent update(LdapLoginSystemComponent system) {
        try {
            URI uri = new URI(system.getUrl());
            if ((!"ldap".equals(uri.getScheme()) && !"ldaps".equals(uri.getScheme())) || (uri.getPort() < 0)) {
                throw new Exception();
            }
            _self.setUrl(system.getUrl());
        } catch (Exception ex) {
            throw new ValidationException("url", system.getUrl(), "Invalid LDAP URL");
        }
        _self.setLogin(system.getUsername());
        _self.setPassword(system.getPassword());
        _self.setConfiguration(ConfigUtils.encodeConfiguration(JsonMap.of("config", system.getConfig())));
        return super.update(system);
    }

    @Override
    public Long login(String userName, String password) {
        LoginWebService.Login login = loginWebService.authenticateExternal(userName, password);

        LdapAuthenticator authenticator = getAuthenticator();
        boolean cacheCredentials = authenticator.getProperties().doCacheCredentials();
        // bogus: Not last login time, last validate time.
        Date lastLogin = ((login.userId == null) || !cacheCredentials) ? null
                : loginWebService.getExternalAuthTime(login.userId);
        long loginDelta = (lastLogin == null) ? Long.MAX_VALUE : (Current.getTime().getTime() - lastLogin.getTime());

        boolean valid = LoginWebService.LoginStatus.OK.equals(login.status);
        if (cacheCredentials && valid && (loginDelta < authenticator.getProperties().getCredentialCache() * 1000L)) {
            logger.log(Level.FINE, "LDAP authenticated user against credential cache: {0}", userName);
        } else {
            LdapResult result = authenticator.authenticate(userName, password, config);
            switch (result) {
                case InvalidCredentials:
                    if (valid) {
                        loginWebService.setExternalPassword(login.userId, "LDAP", getId(), null);
                    }
                    throw new LoginException("Invalid LDAP credentials", LoginWebService.LoginStatus.InvalidCredentials, login.userId);

                case ServerError:
                    if ((login.userId != null) && !login.pass) {
                        throw new LoginException("LDAP server error, no local password", LoginWebService.LoginStatus.ServerError, login.userId);
                    } else if (!valid) {
                        throw new LoginException("LDAP server error, password rejected", LoginWebService.LoginStatus.ServerError, login.userId);
                    } else if (!cacheCredentials) {
                        throw new LoginException("LDAP server error, no offline login", LoginWebService.LoginStatus.ServerError, login.userId);
                    } else if (!login.external && !authenticator.getProperties().getAllowLocalAccounts()) {
                        throw new LoginException("LDAP server error, locally authenticated", LoginWebService.LoginStatus.ServerError, login.userId);
                    }
                    logger.log(Level.INFO, "LDAP server error, locally authenticated: {0}", userName);
                    break;

                case NotFound:
                    if ((login.userId != null) && !login.pass) {
                        throw new LoginException("LDAP unknown user, no local password", LoginWebService.LoginStatus.InvalidCredentials, login.userId);
                    } else if (login.external) {
                        // If a user is no longer known in LDAP, I won't let
                        // them log in even if I've cached their password.
                        if (valid) {
                            loginWebService.setExternalPassword(login.userId, "LDAP", getId(), null);
                        }
                        throw new LoginException("LDAP unknown user, external password", LoginWebService.LoginStatus.InvalidCredentials, login.userId);
                    } else if (!valid) {
                        throw new LoginException("LDAP unknown user, password rejected", LoginWebService.LoginStatus.InvalidCredentials, login.userId);
                    } else if (!authenticator.getProperties().getAllowLocalAccounts()) {
                        throw new LoginException("LDAP unknown user, locally authenticated", LoginWebService.LoginStatus.InvalidCredentials, login.userId);
                    }
                    logger.log(Level.INFO, "LDAP unknown user, locally authenticated: {0}", userName);
                    break;

                case Authenticated:
                    if (login.userId == null) {
                        if (!authenticator.getUserCreate()) {
                            throw new LoginException("LDAP authenticated, no local user", LoginWebService.LoginStatus.InvalidCredentials, login.userId);
                        }
                        try {
                            login.userId = createUser(authenticator);
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Error creating user from LDAP", ex);
                            throw new LoginException("LDAP user creation failed", LoginWebService.LoginStatus.ServerError, login.userId);
                        }
                    } else {
                        updateUser(login.userId, authenticator);
                    }
                    if (cacheCredentials) {
                        loginWebService.setExternalAuthTime(login.userId, Current.getTime());
                        if (!valid) {
                            loginWebService.setExternalPassword(login.userId, "LDAP", getId(), password);
                        }
                    }
                    logger.log(Level.INFO, "LDAP authenticated user: {0}", userName);
                    break;
            }
        }

        return login.userId;
    }

    @Override
    public boolean externalPassword() {
        return true;
    }

    @Override
    public String getUrl() {
        return _self.getUrl();
    }

    @Override
    public String getUsername() {
        return _self.getLogin();
    }

    @Override
    public String getPassword() {
        return _self.getPassword();
    }

    @Override
    public String getConfig() {
        return ConfigUtils.decodeConfigurationValue(_self.getConfiguration(), "config", String.class);
    }

    private LdapAuthenticator getAuthenticator() {
        Properties props = new Properties();
        try {
            URI uri = new URI(getUrl());
            props.setProperty("server_secure", String.valueOf("ldaps".equals(uri.getScheme())));
            props.setProperty("server_host", uri.getHost());
            props.setProperty("server_port", String.valueOf(uri.getPort()));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid URL", ex);
        }
        props.setProperty("auth_dn", getUsername());
        props.setProperty("auth_password", getPassword());
        for (String line : getConfig().split("\n")) {
            int index = line.indexOf('=');
            if (StringUtils.isBlank(line) || line.startsWith("#") || (index < 0)) {
                continue;
            }
            props.setProperty(line.substring(0, index).trim(), line.substring(1 + index).trim());
        }
        return new LdapAuthenticator(props);
    }

    private void updateUser(Long userId, LdapAuthenticator auth) {
        UserComponent user = ComponentSupport.get(userId, UserComponent.class);
        if (user != null) {
            user.setGivenName(auth.getGivenName());
            user.setMiddleName(auth.getMiddleName());
            user.setEmailAddress(auth.getEmailAddress());
            user.setFamilyName(auth.getFamilyName());
            String[] roleNames = StringUtils.isEmpty(auth.getUserRole()) ? new String[0] : new String[] { auth.getUserRole() };
            HashSet<Long> roles = new HashSet<>();
            Long folder = relationshipWebService.getRoleFolder();
            for (String name : roleNames) {
                RoleFacade role = relationshipWebService.getRoleByRoleId(folder, name);
                if (role == null) {
                    throw new RuntimeException("Invalid role name: " + name);
                }
                roles.add(role.getId());
            }
            enrollmentWebService.setEnrollments(Current.getDomain(), roles, userId, null, null, null,  false);
        }
    }

    private Long createUser(LdapAuthenticator authenticator) {
        UserComponent.Init init = new UserComponent.Init();
        init.userName = authenticator.getUserName();
        init.givenName = authenticator.getGivenName();
        init.middleName = authenticator.getMiddleName();
        init.emailAddress = authenticator.getEmailAddress();
        init.familyName = authenticator.getFamilyName();
        init.roles = StringUtils.isEmpty(authenticator.getUserRole()) ? new String[0] : new String[] { authenticator.getUserRole() };
        if (DomainConstants.DOMAIN_TYPE_OVERLORD.equals(Current.getDomainDTO().getType())) {
            init.userType = UserType.Overlord;
        }
        GetOrCreate<UserComponent> user = facadeService
          .getFacade(UserParentFacade.ID_FOLDER_USERS, UserParentFacade.class)
          .getOrCreateUserByUsername(authenticator.getUserName(), init);
        if (user.isGotten()) {
            throw new LoginException("LDAP user creation conflict", LoginWebService.LoginStatus.ServerError, user.result().getId());
        }
        return user.result().getId();
    }

    @Override
    public String logout() {
        return null;
    }

    @Override
    public LdapLoginComponent getLoginComponent() {
        return new LdapLogin();
    }

    private class LdapLogin extends BaseComponent implements LdapLoginComponent {
        public LdapLogin() {
            super(LdapLoginSystem.this.getComponentInstance());
        }

        @Override
        public Long getId() {
            return LdapLoginSystem.this.getId();
        }

        @Override
        public String getName() {
            return LdapLoginSystem.this.getName();
        }
    }
}
