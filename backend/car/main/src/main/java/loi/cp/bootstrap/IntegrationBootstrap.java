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

package loi.cp.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.service.integration.IntegrationFacade;
import com.learningobjects.cpxp.service.integration.IntegrationWebService;
import loi.cp.integration.ConnectorRootComponent;
import loi.cp.integration.SystemComponent;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class IntegrationBootstrap extends AbstractComponent {
    private static final Logger logger = Logger.getLogger(IntegrationBootstrap.class.getName());
    @Inject
    private IntegrationWebService _integrationWebService;

    @Inject
    private ConnectorRootComponent _connectorRoot;

    @Bootstrap("core.connector.create")
    public void createConnector(ObjectNode node) throws Exception {
        String systemId = node.get("systemId").asText();
        logger.log(Level.INFO, "Create connector {0}", systemId);
        String identifier = node.get("identifier").asText();
        ComponentDescriptor componentDescriptor = ComponentSupport.getComponentDescriptor(identifier);
        Class<? extends ComponentInterface> systemType = ComponentSupport.getNarrowestComponentInterface(componentDescriptor.getComponentClass());
        JsonNode without = node.without("identifier");
        SystemComponent prototype = (SystemComponent) ComponentSupport.getObjectMapper().readValue(ComponentSupport.getObjectMapper().treeAsTokens(without), systemType);
        _connectorRoot.addSystem(prototype);
    }

    @Bootstrap("core.integration.apply")
    public void applyIntegrations(Long id, List<JsonIntegration> integrations) throws Exception {
        for (JsonIntegration jsoni : integrations) {
            Long systemId = _integrationWebService.getSystemBySystemId(jsoni.systemId);
            if (systemId == null) {
                throw new Exception("Unknown system: " + jsoni.systemId);
            }
            IntegrationFacade integration = _integrationWebService.addIntegration(id);
            integration.setExternalSystem(systemId);
            integration.setUniqueId(jsoni.uniqueId);
            integration.setDataSource(jsoni.dataSource);
        }
    }

    public static class JsonIntegration {
        public String systemId;
        public String uniqueId;
        public String dataSource;
    }
}
