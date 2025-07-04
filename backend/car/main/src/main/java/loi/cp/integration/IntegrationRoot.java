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

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.service.integration.IntegrationFacade;
import com.learningobjects.cpxp.service.integration.IntegrationParentFacade;

import java.util.Optional;

@Component
public class IntegrationRoot extends AbstractComponent implements IntegrationRootComponent {
    @Instance
    private IntegrationParentFacade _self;

    @Override
    public Optional<IntegrationComponent> getIntegration(Long id) {
        return getIntegrations(ApiQuery.byId(id, IntegrationComponent.class)).toOptional();
    }

    @Override
    public Optional<IntegrationComponent> getIntegrationBySystemId(Long systemId) {
        return _self.getIntegrations()
          .stream()
          .filter(integration -> systemId.equals(integration.getExternalSystem()))
          .sorted((i1,i2) -> (int) (i1.getId() - i2.getId()))
          .findFirst()
          .map(ComponentSupport.idToComponent(IntegrationComponent.class));
    }

    @Override
    public ApiQueryResults<IntegrationComponent> getIntegrations(ApiQuery query) {
        return ApiQuerySupport.query(_self.getId(), query, IntegrationComponent.class);
    }

    @Override
    public IntegrationComponent addIntegration(IntegrationComponent integration) {
        IntegrationComponent.Init init = new IntegrationComponent.Init();
        init.systemId = integration.getSystemId();
        init.uniqueId = integration.getUniqueId();
        return addIntegration(init);
    }

    @Override
    public IntegrationComponent addIntegration(IntegrationComponent.Init integration) {
        //todo: should we allow duplicates here? Would a single user ever have multiple
        // uniqueIds per 1 system? probably not...
        IntegrationFacade newIntegration = _self.addIntegration();
        newIntegration.setUniqueId(integration.uniqueId);
        newIntegration.setExternalSystem(integration.systemId);
        return ComponentSupport.get(newIntegration, IntegrationComponent.class);
    }
}
