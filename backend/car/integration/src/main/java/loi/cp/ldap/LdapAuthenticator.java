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

import com.learningobjects.cpxp.util.StringUtils;
import com.typesafe.config.Config;
import netscape.ldap.*;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LdapAuthenticator {
    private static final Logger logger = Logger.getLogger(LdapAuthenticator.class.getName());

    private LdapProperties _properties;
    private LDAPEntry _entry;
    private String _type;
    private boolean _debug;
    private StringBuilder debugLog;

    LdapAuthenticator(Properties properties) {
        _properties = new LdapProperties(properties);
    }

    void setDebugMode(StringBuilder log) {
        _debug = true;
        debugLog = log;
    }

    LdapResult authenticate(String userName, String password, Config config) {
        logImpl("LDAP authenticating", userName);
        try {
            LDAPConnection connection = new LDAPConnection();

            if (_properties.getServerSecure()) {
                // TODO: How to handle cert validation. Ideally I'd disable
                // validation and just do explicit public key matching.
                connection.setSocketFactory(new LdapSocketFactory(config));
            }

            connection.setConnectTimeout(_properties.getConnectTimeout());
            LDAPConstraints constraints = connection.getConstraints();
            constraints.setTimeLimit(_properties.getOperationTimeout() * 1000);

            logImpl("LDAP connecting", _properties.getServerHost(), _properties.getServerPort());
            connection.connect(_properties.getServerHost(), _properties.getServerPort());

            try {
                logImpl("LDAP connected");

                if (_properties.hasAuth()) {
                    logImpl("LDAP login: {0}", _properties.getAuthDn());
                    connection.authenticate(_properties.getProtocolVersion(), _properties.getAuthDn(), _properties.getAuthPassword());
                }

                LDAPSearchConstraints searchConstraints = connection.getSearchConstraints();
                searchConstraints.setBatchSize(0);
                searchConstraints.setMaxResults(2); // So I'll see if there were multiple matches
                searchConstraints.setReferrals(true);
                searchConstraints.setTimeLimit(_properties.getOperationTimeout() * 1000);
                searchConstraints.setServerTimeLimit(_properties.getOperationTimeout());

                for (String type: _properties.getUserTypes()) {
                    logImpl("LDAP user type: {0}", type);

                    String base = _properties.getUserBase(type);
                    int scope = _properties.getUserScope(type);
                    String filter = getFilter(type, userName);
                    logImpl("LDAP searching: {0}, {1}, {2}", base, scope, filter);
                    LDAPSearchResults results = connection.search(base, scope, filter, null, false);

                    if ((null == results) || !results.hasMoreElements() )  {
                        logImpl("LDAP user not found");
                    } else {
                        try {
                            _entry = results.next();
                            _type = type;
                            logImpl("LDAP entry: {0}, {1}", type, _entry);

                            if (results.hasMoreElements()) {
                                logImpl("Additional Match: {0}", results.next());
                                throw new LDAPException("More than one user is found", LDAPException.INVALID_CREDENTIALS);
                            }

                            logImpl("LDAP authenticating");
                            connection.authenticate(_entry.getDN(), password);

                            logImpl("LDAP authenticated", connection.getAuthenticationDN());
                            return LdapResult.Authenticated;
                        } catch (LDAPException ex) {
                            if (ex.getLDAPResultCode() == LDAPException.INVALID_CREDENTIALS) {
                                logImpl("Invalid LDAP credentials", ex);
                                return LdapResult.InvalidCredentials;
                            }
                            throw ex;
                        }
                    }
                }

                logImpl("LDAP no user found");
                return LdapResult.NotFound;
            } finally {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            logImpl("LDAP error", ex);
            return LdapResult.ServerError;
        }
    }

    void logImpl(String msg, Object...params) {
        if (_debug) {
            debugLog.append(msg);
            boolean first = true;
            for (Object param : params) {
                debugLog.append(first ? ": " : ", ").append(param);
                first = false;
            }
            debugLog.append("\n");
        } else if ((params.length == 1) && (params[0] instanceof Throwable)) {
            logger.log(Level.WARNING, msg, (Throwable) params[0]);
        } else {
            logger.log(Level.INFO, msg, params);
        }
    }

    String getFilter(String type, String userName) {
        String filter = _properties.getUserFilter(type);
        return filter.replace("{userName}", LdapUtils.escapeLDAPSearchFilter(userName));
    }

    LdapProperties getProperties() {
        return _properties;
    }

    boolean getUserCreate() {
        return _properties.getUserCreate(_type);
    }

    String getUserRole() {
        return getUserProperty("role");
    }

    String getUserName() {
        return getUserProperty("userName");
    }

    String getGivenName() {
        return getUserProperty("givenName");
    }

    String getMiddleName() {
        return getUserProperty("middleName");
    }

    String getFamilyName() {
        return getUserProperty("familyName");
    }

    String getEmailAddress() {
        return getUserProperty("emailAddress");
    }

    private String getUserProperty(String property) {
        Pattern SELECTOR_RE = Pattern.compile("([^=]*)=(.*)");
        Pattern TOKEN_RE = Pattern.compile("\\{([^}]+)\\}");

        String value = _properties.getUserProperty(_type, property);
        Matcher matcher = TOKEN_RE.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = null;
            LDAPAttribute attr = _entry.getAttribute(matcher.group(1));
            if (attr != null) {
                String[] values = attr.getStringValueArray();
                if ((null != values) && (values.length > 0)) {
                    replacement = values[0];
                }
            }
            // If any requested attribute is null, return empty
            // so that {cn}@example.org will return "" if cn is unknown
            if (StringUtils.isEmpty(replacement)) {
                return "";
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
