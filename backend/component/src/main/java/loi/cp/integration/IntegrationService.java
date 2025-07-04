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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.cpxp.service.integration.IntegrationFacade;
import com.learningobjects.cpxp.service.integration.IntegrationWebService;
import com.learningobjects.cpxp.service.integration.SystemFacade;
import com.learningobjects.cpxp.service.item.ItemService;
import com.learningobjects.cpxp.service.query.*;
import loi.cp.apikey.ApiKeySystem;
import loi.cp.ip.IpMatch;
import scala.Option;

import javax.inject.Inject;
import java.util.logging.Logger;

@Service
public class IntegrationService {
    private static final Logger LOGGER = Logger.getLogger(IntegrationService.class.getName());

    @Inject
    private IntegrationWebService _integrationWebService;

    @Inject
    private ItemService _itemService;

    @Inject
    private QueryService _queryService;

    public ApiKeySystem getApiKeyByIdAndSecret(String systemId, String key, String ip) {
        SystemFacade system = _integrationWebService.getById(systemId);
        if (system == null || system.getDisabled() || !ComponentSupport.isSupported(ApiKeySystem.class, system)) {
            return null;
        }
        ApiKeySystem apiKey = ComponentSupport.getInstance(ApiKeySystem.class, system, null);
        if (!key.equals(apiKey.getKey())) {
            LOGGER.info("API Key secret mismatch");
            return null;
        } else if (!IpMatch.parse(apiKey.getWhiteListIps()).emptyOrMatches(ip)) {
            LOGGER.info("API Key whitelist failed: " + apiKey.getName() + ": " + apiKey.getWhiteList() + " vs " + ip);
            return null;
        } else {
            return apiKey;
        }
    }

    // TODO: Kill me. Looking up a system by its secret is bad.
    public ApiKeySystem getApiKeyBySecret(String key, String ip) {
        SystemFacade system = _integrationWebService.getByKey(key);
        if (system == null || system.getDisabled() || !ComponentSupport.isSupported(ApiKeySystem.class, system)) {
            return null;
        }
        ApiKeySystem apiKey = ComponentSupport.getInstance(ApiKeySystem.class, system, null);
        if (!IpMatch.parse(apiKey.getWhiteListIps()).emptyOrMatches(ip)) {
            LOGGER.info("API Key whitelist failed: " + apiKey.getName() + ": " + apiKey.getWhiteList() + " vs " + ip);
            return null;
        } else {
            return apiKey;
        }
    }

    /* This should be a method on system component */
    public QueryBuilder queryIntegrated(SystemComponent system, String uniqueId) {
        QueryBuilder qb = _queryService.queryAllDomains();
        qb.setItemType(IntegrationConstants.ITEM_TYPE_INTEGRATION);
        qb.setImplicitRoot(_itemService.get(Current.getDomain()));
        qb.addCondition(BaseCondition.getInstance(IntegrationConstants.DATA_TYPE_UNIQUE_ID, "eq", uniqueId.toLowerCase(), "lower"));
        qb.addCondition(IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM, Comparison.eq, system.getId());
        qb.setProjection(Projection.PARENT_ID);
        return qb;
    }

    public Option<IntegrationComponent> integrate(final Id parent, final SystemComponent system, final String uniqueId) {
        final Long parentId = parent.getId();
        final Long systemId = system.getId();
        if (_integrationWebService.getIntegrationFacades(parentId).stream().noneMatch(i -> uniqueId.equalsIgnoreCase(i.getUniqueId()) && systemId.equals(i.getExternalSystem()))) {
            final IntegrationFacade facade = _integrationWebService.addIntegration(parentId);
            facade.setUniqueId(uniqueId);
            facade.setExternalSystem(systemId);
            return Option.apply(ComponentSupport.get(facade, IntegrationComponent.class));
        } else {
            return Option.empty();
        }
    }

    public boolean isIntegrated(final Id parent, final SystemComponent system) {
        final Long parentId = parent.getId();
        final Long systemId = system.getId();
        return _integrationWebService.getIntegrationFacades(parentId).stream().anyMatch(i -> systemId.equals(i.getExternalSystem()));
    }

}
