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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.annotation.Configuration;
import com.learningobjects.cpxp.component.annotation.Schema;
import loi.cp.integration.PasswordLoginSystem;

@Schema("ldapLoginConnector")
public interface LdapLoginSystemComponent extends PasswordLoginSystem<LdapLoginSystemComponent, LdapLoginComponent> {
    @Configuration(
            type = "String",
            label = "$$field_ldapUrl=Directory URL",
            size = 128,
            order = 10
    )
    @JsonProperty
    public String getUrl();

    @Configuration(
            type = "String",
            label = "$$field_authDN=Auth DN",
            size = 128,
            order = 11
    )
    @JsonProperty
    public String getUsername();

    @Configuration(
            type = "Password",
            label = "$$field_authPassword=Auth Password",
            size = 96,
            order = 12
    )
    @JsonProperty
    public String getPassword();

    @Configuration(
            type = "Text",
            label = "$$field_configuration=Configuration",
            size = 160,
            order = 13
    )
    @JsonProperty
    public String getConfig();
}
