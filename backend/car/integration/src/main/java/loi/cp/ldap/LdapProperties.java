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
import netscape.ldap.LDAPv2;

import java.util.Properties;

class LdapProperties {
    private Properties _properties;

    LdapProperties(Properties properties) {
        _properties = properties;
    }

    String getServerHost() {
        return getProperty("server_host");
    }

    int getServerPort() {
        return Integer.parseInt(getProperty("server_port", "389"));
    }

    boolean getServerSecure() {
        return "true".equalsIgnoreCase(getProperty("server_secure", "false"));
    }

    int getProtocolVersion() {
        return Integer.parseInt(getProperty("protocol_version", "3"));
    }

    int getConnectTimeout() {
        return Integer.parseInt(getProperty("connect_timeout", "5"));
    }

    int getOperationTimeout() {
        return Integer.parseInt(getProperty("operation_timeout", "5"));
    }

    boolean hasAuth() {
        return StringUtils.isNotEmpty(getProperty("auth_dn", ""));
    }

    String getAuthDn() {
        return getProperty("auth_dn");
    }

    String getAuthPassword() {
        return getProperty("auth_password", "");
    }

    boolean doCacheCredentials() {
        return getCredentialCache() > 0;
    }

    int getCredentialCache() {
        return Integer.parseInt(getProperty("credential_cache", "-1"));
    }

    boolean getAllowLocalAccounts() {
        return "true".equalsIgnoreCase(getProperty("local_accounts", "false"));
    }

    String[] getUserTypes() {
        return StringUtils.splitString(getProperty("user_types", "user"));
    }

    String getUserBase(String type) {
        return getUserProperty(type, "base");
    }

    int getUserScope(String type) {
        String scope = getUserProperty(type, "scope");
        if ("sub".equalsIgnoreCase(scope)) {
            return LDAPv2.SCOPE_SUB;
        } else if ("one".equalsIgnoreCase(scope)) {
            return LDAPv2.SCOPE_ONE;
        } else {
            throw new IllegalStateException("Invalid scope: " + scope);
        }
    }

    String getUserFilter(String type) {
        return getUserProperty(type, "filter");
    }

    boolean getUserCreate(String type) {
        return "true".equalsIgnoreCase(getUserProperty(type, "create"));
    }

    String getUserProperty(String type, String property) {
        return getProperty(type + "_" + property, "");
    }

    String getProperty(String name) {
        String value = _properties.getProperty(name);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalStateException("Missing property: " + name);
        }
        return value;
    }

    String getProperty(String name, String def) {
        return StringUtils.defaultIfEmpty(_properties.getProperty(name), def);
    }
}
