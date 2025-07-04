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

package loi.cp.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.de.authorization.Secured;
import loi.cp.admin.right.AdminRight;
import loi.cp.admin.right.IntegrationAdminRight;
import loi.cp.admin.right.LtiAdminRight;
import loi.cp.right.RightMatch;

import java.util.List;
import java.util.Optional;

@Service
@Controller(value = "connectors", root = true, category = Controller.Category.API_SUPPORT)
@RequestMapping(path = "connectors")
public interface ConnectorRootComponent extends ApiRootComponent {
    @RequestMapping(method = Method.GET)
    @Secured(value = IntegrationAdminRight.class)
    ApiQueryResults<SystemComponent> getSystems(ApiQuery query);

    @RequestMapping(path = "{id}", method = Method.GET)
    @Secured(value = IntegrationAdminRight.class)
    Optional<SystemComponent> getSystem(@PathVariable("id") Long id);

    @RequestMapping(method = Method.GET, path = "names")
    @Secured(value = AdminRight.class, overrides = true, match = RightMatch.ANY)
    ApiQueryResults<SystemName> getSystemNames(ApiQuery query);

    @RequestMapping(path = "names/{id}", method = Method.GET)
    @Secured(value = AdminRight.class, overrides = true, match = RightMatch.ANY)
    Optional<SystemName> getSystemName(@PathVariable("id") Long id);

    @RequestMapping(method = Method.POST)
    @Secured(IntegrationAdminRight.class)
    <T extends SystemComponent> T addSystem(@RequestBody T system);

    // meh, but if the methods intrinsically check rights then their
    // behaviour will be surprising so i must duplicate them.

    @RequestMapping(path = "lti", method = Method.GET)
    @Secured(LtiAdminRight.class)
    ApiQueryResults<SystemComponent> getLtiSystems(ApiQuery query);

    @RequestMapping(path = "lti/{id}", method = Method.GET)
    @Secured(LtiAdminRight.class)
    Optional<SystemComponent> getLtiSystem(@PathVariable("id") Long id);

    @RequestMapping(path = "key", method = Method.GET)
    @Secured(LtiAdminRight.class)
    String generateKey();

    @Secured(LtiAdminRight.class)
    @RequestMapping(path = "config/{identifier}", method = Method.GET)
    Optional<ConnectorConfig> getConnectorConfig(@PathVariable("identifier") String identifier);

    public static class ConnectorConfig {
        @JsonProperty
        public String name;

        @JsonProperty
        public String schema;

        @JsonProperty
        public List<ConfigEntry> configs;
    }

    public static class ConfigEntry {
        @JsonProperty
        public String id;

        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        public int size;
    }
}
