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

package com.learningobjects.cpxp.service.login;

import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.service.BasicServiceBean;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataService;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.user.*;
import com.learningobjects.cpxp.util.DigestUtils;
import com.learningobjects.cpxp.util.NumberUtils;
import com.learningobjects.cpxp.util.PasswordUtils;
import com.learningobjects.cpxp.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Login web service implementation.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class LoginWebServiceBean extends BasicServiceBean implements LoginWebService {
    private static final Logger logger = Logger.getLogger(LoginWebServiceBean.class.getName());

    private static final String EXTERNAL_PREFIX = "Ext:";

    /** The data service. */
    @Inject
    private DataService _dataService;

    /** The facade service. */
    @Inject
    private FacadeService _facadeService;

    /** The user web service. */
    @Inject
    private UserWebService _userWebService;

    /** The random. */
    private SecureRandom _random;

    @PostConstruct
    @SuppressWarnings("unused")
    private void initialize() throws NoSuchAlgorithmException {
        logger.log(Level.FINE, "Initializing secure random");
        _random = SecureRandom.getInstance(DigestUtils.SECURE_RANDOM_SHA_1);
        _random.nextInt(); // force self-seeding
    }

    public Long authenticate(String userName, String password) {
        UserFacade user = _userWebService.getUserByUserName(userName);

        String encPassword = (user == null) ? null : user.getPassword();
        UserState state = (user == null) ? null : user.getUserState();
        Long userId = (user == null) ? null : user.getId();
        boolean validPassword = validatePassword(password, encPassword);
        boolean externalPassword = StringUtils.startsWith(encPassword, EXTERNAL_PREFIX);

        // Authenticate first in order to not leak user state to anonymous users.
        if (!validPassword) {
            throw new LoginException("Invalid credentials", LoginStatus.InvalidCredentials, userId);
        } else if ((user != null) && Boolean.TRUE.equals(user.getDisabled())) {
            throw new LoginException("Disabled user: " + userName, LoginStatus.Suspended, userId);
        } else if (UserState.Suspended.equals(state) || UserState.Gdpr.equals(state)) {
            throw new LoginException("Account suspended", LoginStatus.Suspended, userId);
        } else if ((user != null) && (externalPassword || StringUtils.isEmpty(encPassword))) {
            throw new LoginException("Known user, no local password", LoginStatus.InvalidCredentials, userId);
        } else if (UserState.Pending.equals(state)) {
            throw new LoginException("Account pending", LoginStatus.Pending, userId);
        } else if (UserState.Unconfirmed.equals(state)) {
            throw new LoginException("Account unconfirmed", LoginStatus.Unconfirmed, userId);
        } else if (!UserState.Active.equals(state)) {
            throw new LoginException("Invalid credentials", LoginStatus.InvalidCredentials, userId);
        }

        String[] split = splitPassword(encPassword);
        String salt = Current.getDomainDTO().getDomainId() + "/" + userName;
        if (!salt.equals(split[0])) {
            logger.log(Level.FINE, "Rewriting password with invalid salt, {0}", userName);
            setPassword(user.getId(), password);
        }

        logger.log(Level.FINE, "Locally authenticated user, {0}", userName);

        return user.getId();
    }

    private static LoginStatus getLoginStatus(UserState state, boolean validPassword) {
        if (UserState.Suspended.equals(state)) {
            return LoginStatus.Suspended;
        } else if (validPassword) {
            switch (state) {
              case Active:
                  return LoginStatus.OK;
              case Pending:
                  return LoginStatus.Pending;
              case Unconfirmed:
                  return LoginStatus.Unconfirmed;
            }
        }
        return LoginStatus.InvalidCredentials;
    }

    public Login authenticateExternal(String userName, String password) {
        UserFacade user = _userWebService.getUserByUserName(userName);
        UserState state = (user == null) ? null : user.getUserState();
        String encPassword = (user == null) ? null : user.getPassword();
        boolean externalPassword = StringUtils.startsWith(encPassword, LoginWebService.EXTERNAL_PREFIX);
        boolean validPassword = validatePassword(password, encPassword);

        Login login = new Login();
        login.userId = (user != null) ? user.getId() : null;
        login.pass = StringUtils.isNotEmpty(encPassword);
        login.status = getLoginStatus(state, validPassword);
        login.external = externalPassword;

        return login;
    }

    public void setPassword(Long userId, String password) {

        Item user = _itemService.get(userId);
        String userName = DataTransfer.getStringData(user,
                UserConstants.DATA_TYPE_USER_NAME);
        String encoded = PasswordUtils.encodePassword(Current.getDomainDTO(), userName, password);
        _dataService.setString(user,
                UserConstants.DATA_TYPE_PASSWORD, encoded);

    }

    public void recordLogin(Long userId) {
        UserFacade user = _facadeService.getFacade(userId, UserFacade.class);
        UserHistoryFacade history = user.getOrCreateHistory();
        history.setAccessTime(getCurrentTime());
        history.setLoginTime(getCurrentTime());
        history.setLoginCount(1 + NumberUtils.longValue(history.getLoginCount()));
    }

    /**
     * Validate a password.
     *
     * @param password
     *            the password
     * @param encoded
     *            the encoded password against which to validate
     *
     * @return whethen the password validates.
     */
    public static boolean validatePassword(String password, String encoded) {
        String[] split = splitPassword(encoded);
        String salt = split[0], remainder = split[1];
        String digested = PasswordUtils.digest(salt, password);
        return StringUtils.equalsIgnoreCase(digested, remainder);
    }

    private static final String FAKE_ENCODED_PASSWORD = "XYZ:0123456789abcdef0123456789abcdef012345678";

    private static String[] splitPassword(String encoded) {
        encoded = StringUtils.defaultIfEmpty(encoded, FAKE_ENCODED_PASSWORD);
        if (encoded.startsWith(EXTERNAL_PREFIX)) { // Ext:LDAP:foo:salt:digest
            encoded = StringUtils.substringAfter(encoded, ":"); // LDAP:foo:salt:digest
            encoded = StringUtils.substringAfter(encoded, ":"); // foo:salt:digest
            encoded = StringUtils.substringAfter(encoded, ":"); // salt:digest
        }
        String salt = StringUtils.substringBefore(encoded, ":");
        String remainder = StringUtils.substringAfter(encoded, ":");
        return new String[] { salt, remainder };
    }

    // This returns true only if the user has an internal
    // password; false if they have an external (e.g. LDAP)
    // password, to prevent change password link and
    // reset password from trying to work
    public boolean hasPassword(Long userId) {

        Item user = _itemService.get(userId);
        String password = DataTransfer.getStringData(
                user, UserConstants.DATA_TYPE_PASSWORD);
        boolean hasPassword = !StringUtils.isEmpty(password) &&
            !password.startsWith(EXTERNAL_PREFIX);

        return hasPassword;
    }

    public Date getExternalAuthTime(Long userId) {
        return _facadeService.getFacade(userId, UserFacade.class)
          .getOrCreateHistory().getExternalAuthTime();
    }

    public void setExternalAuthTime(Long userId, Date when) {
        _facadeService.getFacade(userId, UserFacade.class)
          .getOrCreateHistory().setExternalAuthTime(when);
    }

    public void setExternalPassword(Long userId, String mechanism, Long mechanismId, String password) {

        Item user = _itemService.get(userId);
        String prefixed = null;
        if (password != null) {
            String userName = DataTransfer.getStringData(user,
                                                        UserConstants.DATA_TYPE_USER_NAME);

            String encoded = PasswordUtils.encodePassword(Current.getDomainDTO(), userName, password);
            prefixed = EXTERNAL_PREFIX + mechanism + ":" + mechanismId + ":" + encoded;
        }
        _dataService.setString(user,
               UserConstants.DATA_TYPE_PASSWORD, prefixed);

    }
}
